from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel
from typing import List, Optional
import os
import re
import html
from dotenv import load_dotenv
from sse_starlette.sse import EventSourceResponse

# Style few-shot examples
STYLE_EXAMPLES = {
    "technical": """# 零信任网关的性能瓶颈与优化\n\n## 压测发现\n高并发场景下，TLS 握手成为主要延迟。通过分析火焰图，我们发现 CPU 主要消耗在加密解密环节。\n\n## 改进方案\n1. 引入会话复用降低握手成本\n2. 使用连接池复用上游长连接\n\n## 结果分析\n优化后 P99 延迟下降 35%，吞吐量提升显著。""",
    "tutorial": """# 三步完成前端灰度发布\n\n## 第一步：准备环境\n首先，准备两套环境：稳定版与灰度版。确保两套环境的配置隔离。\n\n## 第二步：配置网关\n通过网关规则将带有特定标识的用户导向灰度版。\n\n## 第三步：扩大流量\n观察指标（错误率、延迟、留存）。指标健康后扩大流量，最终全量。""",
    "storytelling": """# 修复线上告警的四小时\n\n## 01:00 告警响起\n短信把我从睡梦中拎起：500 错误飙升。打开监控大盘，红色的折线触目惊心。\n\n## 01:30 定位问题\n日志里出现频繁的超时，最终定位到是新上线的缓存键冲突导致的缓存击穿。\n\n## 03:00 危机解除\n快速执行回滚操作，并打上热修补丁，各项核心指标终于逐步回落。""",
    "academic": """# 基于对比学习的文本检索实验\n\n## 研究背景\n现有 BM25 难以捕捉语义同义关系，在长文本匹配上存在局限。\n\n## 实验方法\n通过引入双塔编码器生成句向量，并运用对比学习拉近正样本距离。\n\n## 结论探讨\n实验表明，在 C-MTEB 检索任务上 nDCG@10 提升 8.4%。向量化显著带来了召回率提升。""",
    "casual": """# 周末折腾树莓派的小记录\n\n## 折腾起因\n一直想给客厅做个低成本的家庭媒体中心，刚好翻出吃灰的树莓派。\n\n## 踩坑过程\n刷了最新版系统，结果 Wi-Fi 驱动罢工。换回旧版固件，顺手把散热片重新贴了一下。\n\n## 最终收获\n看电影果然不卡了，周末的时光就是在这些瞎折腾中度过的。""",
}

# Load environment variables
load_dotenv()

app = FastAPI(title="AI Assistant Service", version="1.0.0")

# --- Models ---
class SummaryRequest(BaseModel):
    article_id: str
    content: str

class WritingRequest(BaseModel):
    materials: List[str]
    style: Optional[str] = "technical"
    attachments: Optional[List[str]] = []

class RecommendationRequest(BaseModel):
    query: str
    attachments: Optional[List[str]] = []

class IngestRequest(BaseModel):
    article_id: str
    title: str
    content: str
    tags: Optional[List[str]] = []

class BulkIngestRequest(BaseModel):
    articles: List[IngestRequest]

import sys
import os

# Add current directory to path so imports work
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

from services import ModelService, RAGService

# --- Services ---
model_service = ModelService()
rag_service = RAGService()

def format_markdown_structure(text: str) -> str:
    """
    结构化处理Markdown内容，确保格式规范
    采用激进策略：强制修复AI生成的格式问题
    """
    if not text or not text.strip():
        return text
    
    # 移除大模型的思考标签
    text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL)
    text = text.strip()
    
    if not text:
        return text
    
    # === 预处理：修复常见的粘连问题 ===
    
    # 1. 修复标题后直接跟段落的问题
    # 如果标题太长且没有换行，在合适的空格处拆分，不强制按字符位置拆分
    def split_title_paragraph(match):
        prefix = match.group(1)
        content = match.group(2)
        if len(content) < 40:
            return match.group(0)
        
        split_pos = -1
        for punct in ['。', '！', '？', '.', '!', '?']:
            pos = content.find(punct, 0, 30)
            if pos != -1 and (split_pos == -1 or pos < split_pos):
                split_pos = pos + 1

        if split_pos == -1:
            for punct in ['，', '、', ',']:
                pos = content.find(punct, 0, 30)
                if pos != -1 and (split_pos == -1 or pos < split_pos):
                    split_pos = pos + 1

        if split_pos != -1:
            return f'{prefix}{content[:split_pos].strip()}\n\n{content[split_pos:].strip()}'
        return match.group(0)

    text = re.sub(
        r'^(#{1,6}\s+)([^\n]{40,})$',
        split_title_paragraph,
        text,
        flags=re.MULTILINE
    )
    
    # 2. 修复标题后直接跟列表（如：### 标题- 列表项）
    text = re.sub(
        r'^(#{1,6}\s+[^\n]+?)([-*+]\s+)',
        r'\1\n\n\2',
        text,
        flags=re.MULTILINE
    )
    
    # 按行处理，逐行分析和修复
    lines = text.split('\n')
    processed_lines = []
    in_code_block = False
    
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        
        # === 代码块处理 ===
        if stripped.startswith('```'):
            if in_code_block:
                # 代码块结束
                processed_lines.append('```')
                processed_lines.append('')  # 代码块后加空行
                in_code_block = False
                i += 1
                continue
            else:
                # 代码块开始
                # 确保前面有空行
                if processed_lines and processed_lines[-1].strip():
                    processed_lines.append('')
                
                # 提取语言标识符
                rest = stripped[3:].strip()
                
                # 已知语言列表
                known_langs = {
                    'javascript', 'typescript', 'python', 'java', 'cpp', 'c', 'csharp',
                    'go', 'rust', 'ruby', 'php', 'swift', 'kotlin', 'scala', 'bash',
                    'shell', 'sql', 'html', 'css', 'json', 'xml', 'yaml', 'jsx', 'tsx', 'vue'
                }
                
                lang = ''
                code_on_same_line = ''
                
                if rest:
                    rest_lower = rest.lower()
                    # 检查是否是已知语言
                    if rest_lower in known_langs:
                        lang = rest_lower
                    else:
                        # 检查是否语言标识符后直接跟代码（如：javascriptimport）
                        for known_lang in sorted(known_langs, key=len, reverse=True):
                            if rest_lower.startswith(known_lang):
                                # 找到匹配的语言
                                lang = known_lang
                                # 剩余部分是代码
                                remaining = rest[len(known_lang):].strip()
                                if remaining:
                                    code_on_same_line = remaining
                                break
                        
                        # 如果还没找到，可能是未知语言或直接是代码
                        if not lang:
                            # 检查是否看起来像语言标识符（短且只包含字母数字）
                            if len(rest) <= 15 and re.match(r'^[a-z0-9+#\-]+$', rest_lower):
                                lang = rest_lower
                            else:
                                # 可能是代码直接跟在```后面
                                code_on_same_line = rest
                
                # 输出代码块开始标记
                if lang:
                    processed_lines.append(f'```{lang}')
                else:
                    processed_lines.append('```')
                
                # 如果有代码在同一行，输出到下一行
                if code_on_same_line:
                    processed_lines.append(code_on_same_line)
                
                in_code_block = True
                i += 1
                continue
        
        # 在代码块内，直接保留原始内容
        if in_code_block:
            processed_lines.append(line)
            i += 1
            continue
        
        # === 标题处理（只处理真正的Markdown标题：# 后面有空格）===
        # 匹配：行首 + 1-6个# + 空格 + 内容
        title_match = re.match(r'^(#{1,6})\s+(.+)$', stripped)
        if title_match:
            level, title_text = title_match.groups()
            # 确保标题前有空行
            if processed_lines and processed_lines[-1].strip():
                processed_lines.append('')
            processed_lines.append(f'{level} {title_text}')
            # 确保标题后有空行
            if i + 1 < len(lines) and lines[i + 1].strip():
                processed_lines.append('')
            i += 1
            continue
        
        # === 处理没有空格的标题（如：###reactive()）===
        # 匹配：行首 + 1-6个# + 非空格非#字符
        malformed_title_match = re.match(r'^(#{1,6})([^\s#].*)$', stripped)
        if malformed_title_match:
            level, title_text = malformed_title_match.groups()
            # 确保标题前有空行
            if processed_lines and processed_lines[-1].strip():
                processed_lines.append('')
            processed_lines.append(f'{level} {title_text.strip()}')
            # 确保标题后有空行
            if i + 1 < len(lines) and lines[i + 1].strip():
                processed_lines.append('')
            i += 1
            continue
        
        # === 列表处理 ===
        if re.match(r'^[-*+]\s', stripped) or re.match(r'^\d+\.\s', stripped):
            # 确保列表前有空行
            if processed_lines and processed_lines[-1].strip():
                # 检查前一行是否也是列表项
                prev_is_list = re.match(r'^[-*+]\s', processed_lines[-1]) or re.match(r'^\d+\.\s', processed_lines[-1])
                if not prev_is_list:
                    processed_lines.append('')
            processed_lines.append(line)
            i += 1
            continue
        
        # === 表格分隔符 ===
        if re.match(r'^\|[\s\-:|]+\|$', stripped):
            processed_lines.append(line)
            i += 1
            continue
        
        # === 普通行 ===
        if stripped:
            processed_lines.append(line)
        else:
            # 空行：只保留一个
            if not processed_lines or processed_lines[-1].strip():
                processed_lines.append('')
        
        i += 1
    
    # 清理多余的连续空行
    result = []
    empty_count = 0
    for line in processed_lines:
        if not line.strip():
            empty_count += 1
            if empty_count <= 1:
                result.append(line)
        else:
            empty_count = 0
            result.append(line)
    
    return '\n'.join(result).strip()

async def generate_summary_stream(content: str):
    system_prompt = """你是一位资深的技术内容编辑。你的目标是生成一份排版精美、层次分明、易于扫读的中文摘要。

    **格式指南:**
    1.  **结构**: 以一句有力的**核心洞察**开篇。随后以列表形式呈现**关键亮点**。
    2.  **拒绝通用标签**: 关键点不得以"第一点"、"第二步"等开头。应提取*功能*或*价值*作为标头（例如：使用"**语义检索引擎**"而非"重点三"）。
    3.  **视觉层级**:
        -   对每个要点的核心概念使用**加粗**（如 **...**）。
        -   保持要点说明文字简明扼要。
    4.  **语气**: 专业、深刻且文笔流畅。
    5.  **语言**: **必须全部使用中文输出**。

    **Template:**
    [前面两句关于文章主要成就或论点的高层概括。]

    (空行)

    -   **[描述性标题]**: [简要解释此阶段/功能的作用及其重要性。]
    -   **[描述性标题]**: [简要解释。]
    -   **[描述性标题]**: [简要解释。]

    **Example:**
    该架构将AI Agent从简单的记忆体进化为具备海量检索能力的"学习型大脑"。

    -   **内存原型**: 使用轻量级关键词匹配进行聚焦的、小规模文档处理。
    -   **Elasticsearch扩展性**: 引入倒排索引以低延迟支持数百万文档。
    -   **混合语义搜索**: 将向量嵌入与关键词搜索相结合，捕捉字面匹配之外的用户意图。
    -   **实时Web连接**: 集成实时搜索API，将静态知识与当前世界事件联系起来。
    """
    prompt = f"请严格遵守上述格式要求，用中文总结以下内容：\n\n{content}"

    async for chunk in model_service.generate_stream(prompt, system_prompt=system_prompt):
        yield chunk

async def generate_draft_stream(materials: List[str], style: str):
    """
    生成博客草稿的流式输出
    采用累积缓冲策略：收集完整内容后再进行结构化处理
    """
    context = "\n".join(materials)
    example_block = STYLE_EXAMPLES.get(style, STYLE_EXAMPLES["technical"])
    
    print("\n=== DEBUG: 开始生成草稿 ===")

    system_prompt = f"""**核心规则（必须严格遵守）：**

1. **标题独立成行**：写完标题后立即换行，绝不在标题行后继续写任何内容
2. **标题后空行**：标题下一行必须是空行，然后才能写段落或列表
3. **标题简短**：标题最多20个汉字，只写核心概念

**错误示例（绝对禁止）：**
```
❌ ## Proxy 的优势Vue 3 使用 Proxy 替代了...
❌ ### 核心机制- **effect**：定义副作用函数
```

**正确示例（必须模仿）：**
```
✅ ## Proxy 的优势

Vue 3 使用 Proxy 替代了 Vue 2 的 Object.defineProperty。

✅ ### 核心机制

- **effect**：定义副作用函数
- **track**：追踪依赖关系
```

**完整文章模板：**

# 文章标题

开篇段落介绍文章主题。

## 第一个主题

这里是第一个主题的介绍段落。

### 子主题

这里是子主题的详细说明。

- 列表项1
- 列表项2

### 代码示例

```javascript
const example = 'code'
```

## 第二个主题

这里是第二个主题的介绍。

---

**写作风格参考：**
{example_block}

**重要提醒：每写完一个标题，立即按回车换行，不要继续写内容！**
"""

    prompt = f"""请根据以下核心素材，用指定的风格扩展撰写一篇完整的博客文章。

核心素材：
{context}

请输出符合模板规则的Markdown文章内容本体："""

    # 累积所有chunks
    full_content = ""
    chunk_count = 0
    try:
        async for chunk in model_service.generate_stream(
            prompt,
            system_prompt=system_prompt
        ):
            full_content += chunk
            chunk_count += 1
            if chunk_count % 10 == 0:  # 每10个chunk打印一次进度
                print(f"[DEBUG] 已接收 {chunk_count} 个chunks，当前长度: {len(full_content)}")
    except Exception as e:
        print(f"\n[ERROR] 流式生成过程中发生异常: {e}")
        import traceback
        traceback.print_exc()
    
    print(f"\n=== DEBUG: AI原始输出 ===")
    print(f"总共接收 {chunk_count} 个chunks，总长度: {len(full_content)}")
    print(full_content[:500])  # 打印前500字符
    print("...")
    
    # 对完整内容进行结构化格式化
    formatted_content = format_markdown_structure(full_content)
    
    print(f"\n=== DEBUG: 格式化后输出 ===")
    print(formatted_content[:500])  # 打印前500字符
    print("...")
    
    # 分块流式输出格式化后的内容
    chunk_size = 50  # 每次发送50个字符
    for i in range(0, len(formatted_content), chunk_size):
        yield formatted_content[i:i + chunk_size]


def search_recommendations(query: str):
    if not rag_service.is_ready():
        return {
            "explanation": "RAG 未启用，请检查 DASHSCOPE_API_KEY 与向量库配置。",
            "related_chunks": [],
            "error": "rag_not_ready"
        }

    results_with_score = rag_service.search_with_score(query, k=3)

    # Extract titles and reasons
    if not results_with_score:
        return {
            "explanation": "未检索到匹配文章，请先导入文章到知识库。",
            "related_chunks": []
        }

    context_str = "\n".join([f"Article: {doc.metadata.get('title', 'Unknown')}\nContent: {doc.page_content[:200]}..." for doc, score in results_with_score])

    prompt = f"Based on the following search results for query '{query}', recommend top articles and explain why in professional Chinese:\n\n{context_str}"
    explanation = model_service.generate_response(prompt)

    return {
        "explanation": explanation,
        "related_chunks": [
             {
                 "content": doc.page_content,
                 "metadata": doc.metadata,
                 "score": score
             }
             for doc, score in results_with_score
        ]
    }

# --- Endpoints ---

@app.post("/api/ai/ingest")
async def ingest_article(request: IngestRequest):
    """
    Ingest a new article into the vector database.
    """
    if not rag_service.is_ready():
        raise HTTPException(status_code=503, detail="RAG not ready. Check DASHSCOPE_API_KEY.")

    metadata = {
        "article_id": request.article_id,
        "title": request.title,
        "tags": ",".join(request.tags)
    }
    try:
        rag_service.add_document(request.content, metadata)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
    return {"status": "success", "message": f"Article {request.article_id} ingested."}

@app.post("/api/ai/ingest/bulk")
async def ingest_articles_bulk(request: BulkIngestRequest):
    """
    Ingest multiple articles into the vector database.
    """
    if not rag_service.is_ready():
        raise HTTPException(status_code=503, detail="RAG not ready. Check DASHSCOPE_API_KEY.")

    ingested = 0
    skipped = 0
    errors = []
    for article in request.articles:
        if not article.content or not article.content.strip():
            skipped += 1
            continue
        metadata = {
            "article_id": article.article_id,
            "title": article.title,
            "tags": ",".join(article.tags)
        }
        try:
            added = rag_service.add_document(article.content, metadata)
            if added > 0:
                ingested += 1
            else:
                skipped += 1
        except Exception as exc:
            errors.append({"article_id": article.article_id, "error": str(exc)})

    return {"status": "success", "ingested": ingested, "skipped": skipped, "errors": errors}

@app.get("/api/ai/rag/status")
async def rag_status():
    """
    Report RAG status for diagnostics.
    """
    peek = rag_service.peek_documents(1)
    return {
        "ready": rag_service.is_ready(),
        "persist_directory": rag_service.persist_directory,
        "collection_name": rag_service.collection_name,
        "document_count": rag_service.count_documents(),
        "embedding": rag_service.test_embedding(),
        "fresh_collection": rag_service.fresh_collection_info(),
        "peek": {
            "ids": peek.get("ids", []),
            "metadatas": peek.get("metadatas", [])
        }
    }

@app.post("/api/ai/search")
async def search_articles(request: RecommendationRequest):
    """
    Search for articles using vector similarity.
    """
    if not rag_service.is_ready():
        raise HTTPException(status_code=503, detail="RAG not ready. Check DASHSCOPE_API_KEY.")

    results = rag_service.search(request.query, k=5)
    return {
        "results": [
            {
                "content": doc.page_content,
                "metadata": doc.metadata,
                "score": 0.0 # Simplify score for now
            }
            for doc in results
        ]
    }

@app.post("/api/ai/summary")
async def summarize(request: SummaryRequest):
    """
    Generate a summary for an article. Returns a Server-Sent Events (SSE) stream.
    """
    generator = generate_summary_stream(request.content)
    return EventSourceResponse(generator)

@app.post("/api/ai/write")
async def write_draft(request: WritingRequest):
    """
    Generate a blog draft based on materials. Returns a Server-Sent Events (SSE) stream.
    """
    generator = generate_draft_stream(request.materials, request.style)
    return EventSourceResponse(generator)

@app.post("/api/ai/recommend")
async def recommend(request: RecommendationRequest):
    """
    Recommend related articles based on query.
    """
    recommendations = search_recommendations(request.query)
    return {"recommendations": recommendations}

@app.get("/health")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
