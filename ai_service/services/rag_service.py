import os
import logging
from langchain_chroma import Chroma
from langchain_community.embeddings import DashScopeEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from typing import List, Dict, Any
from dotenv import load_dotenv

logger = logging.getLogger(__name__)

try:
    from chromadb import PersistentClient
except Exception as exc:
    logger.warning("PersistentClient import failed: %s. Falling back to legacy Chroma( persist_directory=... ) mode.", exc)
    PersistentClient = None

# Get the absolute path of the directory containing this file
current_dir = os.path.dirname(os.path.abspath(__file__))
# Go up one level to ai_service directory
ai_service_dir = os.path.dirname(current_dir)

load_dotenv()

class RAGService:
    def __init__(self):
        self.api_key = os.getenv("DASHSCOPE_API_KEY")
        # Use absolute path for chroma_db to avoid CWD issues
        default_chroma_path = os.path.join(ai_service_dir, "chroma_db_v2")
        self.persist_directory = os.getenv("CHROMA_DB_PATH", default_chroma_path)
        self.collection_name = os.getenv("CHROMA_COLLECTION", "blog_posts")
        self.client = None
        self._init_error = None

        if self.api_key:
            try:
                self.embeddings = DashScopeEmbeddings(dashscope_api_key=self.api_key, model="text-embedding-v2")
            except Exception as exc:
                logger.error("Failed to initialize DashScope embeddings: %s", exc)
                self.embeddings = None
                self.vector_store = None
                self._init_error = f"Embedding init failed: {exc}"
                return

            try:
                if PersistentClient:
                    self.client = PersistentClient(path=self.persist_directory)
                    self.vector_store = Chroma(
                        client=self.client,
                        collection_name=self.collection_name,
                        embedding_function=self.embeddings,
                        collection_metadata={"hnsw:space": "cosine"}
                    )
                    logger.info("ChromaDB initialized with PersistentClient at %s, collection=%s (cosine distance)",
                                self.persist_directory, self.collection_name)
                else:
                    # Fallback to legacy initialization when PersistentClient is unavailable
                    self.vector_store = Chroma(
                        persist_directory=self.persist_directory,
                        embedding_function=self.embeddings,
                        collection_name=self.collection_name,
                        collection_metadata={"hnsw:space": "cosine"}
                    )
                    logger.info("ChromaDB initialized in legacy mode at %s, collection=%s (cosine distance)",
                                self.persist_directory, self.collection_name)
            except Exception as exc:
                logger.error("Failed to initialize ChromaDB vector store: %s", exc)
                self.vector_store = None
                self.client = None
                self._init_error = f"ChromaDB init failed: {exc}"
        else:
            self.embeddings = None
            self.vector_store = None
            self._init_error = "DASHSCOPE_API_KEY not set"
            logger.warning("DASHSCOPE_API_KEY not found. RAG functionality disabled.")

    def is_ready(self) -> bool:
        return self.embeddings is not None and self.vector_store is not None

    @property
    def init_error(self) -> str | None:
        """Return the last initialization error, if any."""
        return self._init_error

    def test_embedding(self) -> dict:
        """Quick embedding smoke test."""
        if not self.embeddings:
            return {"status": "skipped", "error": self._init_error or "No embeddings configured"}
        try:
            result = self.embeddings.embed_query("test")
            return {"status": "ok", "dimensions": len(result)}
        except Exception as exc:
            return {"status": "error", "error": str(exc)}

    def fresh_collection_info(self) -> dict:
        """Return collection-level info directly from the ChromaDB client."""
        if not self.client:
            return {"status": "unavailable", "reason": "PersistentClient not available"}
        try:
            collection = self.client.get_collection(self.collection_name)
            return {
                "name": collection.name,
                "count": collection.count(),
            }
        except Exception as exc:
            return {"status": "error", "error": str(exc)}

    def count_documents(self) -> int:
        if not self.vector_store:
            return 0
        try:
            return self.vector_store._collection.count()
        except Exception:
            return -1

    def peek_documents(self, limit: int = 1) -> dict:
        if not self.vector_store:
            return {"ids": [], "metadatas": []}
        try:
            return self.vector_store._collection.peek(limit=limit)
        except Exception:
            return {"ids": [], "metadatas": []}

    def add_document(self, content: str, metadata: Dict[str, Any]) -> int:
        if not self.vector_store:
            return 0
        if not content or not content.strip():
            print(f"Skipping empty content for article {metadata.get('article_id')}")
            return 0

        # 将标题和标签拼入正文，使其参与 embedding，提升主题类查询（如"前端文章"）的召回质量
        title = metadata.get("title", "")
        tags = metadata.get("tags", "")
        parts = []
        if title:
            parts.append(f"标题: {title}")
        if tags:
            parts.append(f"标签: {tags}")
        parts.append(content)
        enriched_content = " | ".join(parts)

        text_splitter = RecursiveCharacterTextSplitter(
            separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""],
            chunk_size=500,
            chunk_overlap=50
        )
        # Create Document objects with metadata
        docs = [Document(page_content=chunk, metadata=metadata) for chunk in text_splitter.split_text(enriched_content)]
        if not docs:
            print(f"No chunks created for article {metadata.get('article_id')}")
            return 0

        try:
            self.vector_store.add_documents(docs)
            if hasattr(self.vector_store, "persist"):
                self.vector_store.persist()
            print("Successfully called add_documents")
        except Exception as exc:
            print(f"Failed to add docs for article {metadata.get('article_id')}: {exc}")
            raise RuntimeError(f"failed to add documents: {exc}") from exc

        return len(docs)

    def delete_documents_by_article_id(self, article_id: str) -> None:
        """根据文章ID删除已存在的向量片段，用于增量更新/去重。"""
        if not self.vector_store or not article_id:
            return
        try:
            self.vector_store._collection.delete(where={"article_id": article_id})
            logger.info("已删除文章 %s 的旧向量片段", article_id)
        except Exception as exc:
            logger.warning("删除文章 %s 旧向量片段失败（可能不存在）: %s", article_id, exc)

    def search(self, query: str, k: int = 3) -> List[Document]:
        if not self.vector_store:
            return []

        return self.vector_store.similarity_search(query, k=k)

    def search_with_score(self, query: str, k: int = 3):
        if not self.vector_store:
            return []

        return self.vector_store.similarity_search_with_score(query, k=k)
