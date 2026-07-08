"""
知识库批量导入增强服务
支持：并发处理、分批次导入、错误重试、结构化日志、完整性验证
"""
import asyncio
import logging
import time
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass, field

logger = logging.getLogger("kb_import")

# 配置专用 logger
if not logger.handlers:
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter(
        '[%(asctime)s] [%(levelname)s] [KB-IMPORT] %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    ))
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)


@dataclass
class ImportProgress:
    """导入进度追踪"""
    total: int = 0
    processed: int = 0
    success: int = 0
    failed: int = 0
    current_batch: int = 0
    total_batches: int = 0
    errors: List[Dict[str, Any]] = field(default_factory=list)
    started_at: float = 0.0


@dataclass
class ImportResult:
    """单次导入结果"""
    status: str  # "success" | "partial" | "error"
    ingested: int
    skipped: int
    failed: int
    errors: List[Dict[str, Any]] = field(default_factory=list)
    duration_seconds: float = 0.0


class KnowledgeBaseImportService:
    """
    知识库批量导入服务
    封装 RAGService 的批量导入逻辑，提供并发控制、重试、日志记录
    """

    # 并发控制：同时处理的文章数
    MAX_CONCURRENT_ARTICLES = 10

    # 单篇文章最大重试次数
    MAX_RETRY_PER_ARTICLE = 3

    # 重试间隔(秒)
    RETRY_BASE_DELAY = 1.0

    def __init__(self, rag_service):
        """
        Args:
            rag_service: RAGService 实例
        """
        self.rag_service = rag_service
        self._active_tasks: Dict[str, ImportProgress] = {}
        self._cancelled: set = set()

    @property
    def is_ready(self) -> bool:
        return self.rag_service.is_ready()

    def cancel_task(self, task_id: str) -> bool:
        """取消正在进行的导入任务"""
        self._cancelled.add(task_id)
        logger.info("任务取消标记: task_id=%s", task_id)
        return True

    async def import_articles_batch(
        self,
        articles: List[Dict[str, Any]],
        task_id: Optional[str] = None,
        progress_callback: Optional[callable] = None
    ) -> ImportResult:
        """
        并发批量导入文章到知识库

        Args:
            articles: 文章列表，每篇包含 article_id, title, content, tags
            task_id: 可选的任务追踪ID
            progress_callback: 进度回调 (progress: ImportProgress) -> None

        Returns:
            ImportResult: 导入结果统计
        """
        if not self.is_ready:
            return ImportResult(
                status="error",
                ingested=0, skipped=0, failed=len(articles),
                errors=[{"error": "RAG service not ready"}]
            )

        start_time = time.time()
        total = len(articles)

        # 初始化进度追踪
        progress = ImportProgress(
            total=total,
            total_batches=max(1, (total + self.MAX_CONCURRENT_ARTICLES - 1) // self.MAX_CONCURRENT_ARTICLES),
            started_at=start_time
        )

        if task_id:
            self._active_tasks[task_id] = progress

        # 分批次创建并发任务
        semaphore = asyncio.Semaphore(self.MAX_CONCURRENT_ARTICLES)
        tasks = []

        for idx, article in enumerate(articles):
            if task_id and task_id in self._cancelled:
                logger.warning("任务已取消: task_id=%s, 剩余 %d 篇文章未处理",
                               task_id, total - progress.processed)
                break
            tasks.append(self._import_single_article(
                article, idx, semaphore, progress
            ))

        # 并发执行所有任务
        results = await asyncio.gather(*tasks, return_exceptions=True)

        # 汇总结果
        ingested = 0
        skipped = 0
        failed = 0

        for i, result in enumerate(results):
            if isinstance(result, Exception):
                failed += 1
                progress.failed += 1
                article_id = articles[i].get("article_id", f"unknown_{i}") if i < len(articles) else f"unknown_{i}"
                progress.errors.append({
                    "article_id": article_id,
                    "error": str(result)
                })
            elif isinstance(result, tuple):
                status, count, error_info = result
                if status == "ingested":
                    ingested += count
                    progress.success += count
                elif status == "skipped":
                    skipped += count
                elif status == "failed":
                    failed += 1
                    progress.failed += 1
                    if error_info:
                        progress.errors.append(error_info)

        duration = time.time() - start_time

        import_result = ImportResult(
            status="success" if failed == 0 else "partial",
            ingested=ingested,
            skipped=skipped,
            failed=failed,
            errors=progress.errors,
            duration_seconds=round(duration, 2)
        )

        logger.info(
            "批量导入完成: total=%d, ingested=%d, skipped=%d, failed=%d, duration=%.2fs",
            total, ingested, skipped, failed, duration
        )

        if task_id and task_id in self._active_tasks:
            del self._active_tasks[task_id]

        return import_result

    async def _import_single_article(
        self,
        article: Dict[str, Any],
        index: int,
        semaphore: asyncio.Semaphore,
        progress: ImportProgress
    ) -> Tuple[str, int, Optional[Dict[str, Any]]]:
        """
        导入单篇文章，带重试机制

        Returns:
            (status, chunk_count, error_info)
        """
        async with semaphore:
            article_id = str(article.get("article_id", f"unknown_{index}"))
            title = str(article.get("title", ""))
            content = str(article.get("content", ""))
            tags = article.get("tags", [])

            # 跳过空内容
            if not content or not content.strip():
                logger.debug("跳过空内容: article_id=%s, title=%s", article_id, title)
                progress.processed += 1
                return ("skipped", 0, None)

            metadata = {
                "article_id": article_id,
                "title": title,
                "tags": ",".join(tags) if isinstance(tags, list) else str(tags)
            }

            last_error = None
            for attempt in range(1, self.MAX_RETRY_PER_ARTICLE + 1):
                try:
                    chunk_count = self.rag_service.add_document(content, metadata)
                    progress.processed += 1

                    if progress.processed % 50 == 0 or progress.processed == progress.total:
                        logger.info("进度: %d/%d (%.1f%%)",
                                     progress.processed, progress.total,
                                     progress.processed / max(progress.total, 1) * 100)

                    return ("ingested", chunk_count, None)

                except Exception as exc:
                    last_error = exc
                    if attempt < self.MAX_RETRY_PER_ARTICLE:
                        delay = self.RETRY_BASE_DELAY * (2 ** (attempt - 1))
                        logger.warning(
                            "导入失败，%ds后重试 (%d/%d): article_id=%s, error=%s",
                            delay, attempt, self.MAX_RETRY_PER_ARTICLE, article_id, str(exc)
                        )
                        await asyncio.sleep(delay)
                    else:
                        logger.error(
                            "导入最终失败: article_id=%s, title=%s, error=%s",
                            article_id, title, str(exc)
                        )

            progress.processed += 1
            return ("failed", 0, {
                "article_id": article_id,
                "title": title,
                "error": str(last_error) if last_error else "unknown error"
            })

    def get_progress(self, task_id: str) -> Optional[ImportProgress]:
        """获取任务进度"""
        return self._active_tasks.get(task_id)

    def verify_import(self, expected_article_ids: List[str]) -> Dict[str, Any]:
        """
        验证知识库中的文章完整性

        Args:
            expected_article_ids: 期望存在的文章ID列表

        Returns:
            {
                "passed": True/False,
                "kb_document_count": int,
                "expected_count": int,
                "missing_ids": [...],
                "message": str
            }
        """
        if not self.is_ready:
            return {
                "passed": False,
                "kb_document_count": 0,
                "expected_count": len(expected_article_ids),
                "missing_ids": expected_article_ids,
                "message": "RAG service not ready"
            }

        try:
            # 获取知识库文档总数
            doc_count = self.rag_service.count_documents()

            # 抽样检查前20篇文章是否存在
            peek_data = self.rag_service.peek_documents(50)
            existing_ids = set()
            if peek_data.get("metadatas"):
                for meta in peek_data["metadatas"]:
                    if meta and "article_id" in meta:
                        existing_ids.add(meta["article_id"])

            missing_ids = [
                aid for aid in expected_article_ids[:100]  # 最多检查100篇
                if aid not in existing_ids
            ]

            passed = len(missing_ids) == 0 and doc_count > 0

            return {
                "passed": passed,
                "kb_document_count": doc_count,
                "expected_count": len(expected_article_ids),
                "missing_ids": missing_ids[:20],  # 最多返回20个
                "message": (
                    f"验证通过，知识库文档数: {doc_count}"
                    if passed
                    else f"存在 {len(missing_ids)} 篇文章可能未成功导入"
                )
            }

        except Exception as exc:
            logger.error("验证失败: %s", exc)
            return {
                "passed": False,
                "kb_document_count": -1,
                "expected_count": len(expected_article_ids),
                "missing_ids": [],
                "message": f"验证异常: {str(exc)}"
            }

    def reset_knowledge_base(self) -> Dict[str, Any]:
        """
        重置知识库（删除所有文档后重建 collection）
        注意：此操作不可逆！
        """
        if not self.is_ready:
            return {"status": "error", "message": "RAG service not ready"}

        try:
            old_count = self.rag_service.count_documents()

            # 删除 collection 并重建
            if hasattr(self.rag_service, 'client') and self.rag_service.client:
                try:
                    self.rag_service.client.delete_collection(
                        self.rag_service.collection_name
                    )
                except Exception:
                    pass

            # 重新初始化 vector_store
            from langchain_chroma import Chroma
            self.rag_service.vector_store = Chroma(
                client=self.rag_service.client,
                collection_name=self.rag_service.collection_name,
                embedding_function=self.rag_service.embeddings,
                collection_metadata={"hnsw:space": "cosine"}
            )

            new_count = self.rag_service.count_documents()
            logger.warning("知识库已重置: 删除 %d 条记录, 当前文档数: %d", old_count, new_count)

            return {
                "status": "success",
                "message": f"知识库已重置",
                "deleted_count": old_count,
                "current_count": new_count
            }
        except Exception as exc:
            logger.error("重置知识库失败: %s", exc)
            return {"status": "error", "message": str(exc)}
