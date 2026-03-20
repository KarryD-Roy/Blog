from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel
from typing import List, Optional
import os
from dotenv import load_dotenv
from sse_starlette.sse import EventSourceResponse

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

import sys
import os

# Add current directory to path so imports work
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

from services import ModelService, RAGService

# --- Services ---
model_service = ModelService()
rag_service = RAGService()

async def generate_summary_stream(content: str):
    system_prompt = """You are an expert technical curator. Your goal is to produce a beautiful, high-hierarchy summary in Chinese that is easy to scan.

    **Style Guidelines:**
    1.  **Structure**: Start with a single, powerful **Core Insight** sentence. Follow it with a set of **Key Highlights** presented as a list.
    2.  **No Generic Labels**: key points MUST NOT start with "Version 1", "Step 2", etc. Instead, extract the *feature* or *value* as the header (e.g., use "**语义检索引擎**" instead of "Version 3").
    3.  **Visual Hierarchy**:
        -   Use **Bold** for the primary concept of each bullet point(Use **...**).
        -   Keep bullet point text concise.
    4.  **Tone**: Professional, insightful, and polished.
    5.  **Language**: **MUST OUTPUT IN CHINESE**.

    **Template:**
    [核心洞察 - 前面两句关于文章主要成就或论点的高层概括。]

    (空行)

    -   **[描述性标题]**: [简要解释此阶段/功能的作用及其重要性。]
    -   **[描述性标题]**: [简要解释。]
    -   **[描述性标题]**: [简要解释。]

    **Example:**
    该架构将AI Agent从简单的记忆体进化为具备海量检索能力的“学习型大脑”。

    -   **内存原型**: 使用轻量级关键词匹配进行聚焦的、小规模文档处理。
    -   **Elasticsearch扩展性**: 引入倒排索引以低延迟支持数百万文档。
    -   **混合语义搜索**: 将向量嵌入与关键词搜索相结合，捕捉字面匹配之外的用户意图。
    -   **实时Web连接**: 集成实时搜索API，将静态知识与当前世界事件联系起来。
    """
    prompt = f"Summarize the following content in Chinese adhering effectively to the hierarchy rules:\n\n{content}"

    # We need to access the underlying model_service method that accepts system_prompt if available,
    # or prepend it. model_service.generate_stream supports system_prompt.
    async for chunk in model_service.generate_stream(prompt, system_prompt=system_prompt):
        yield chunk

def generate_draft(materials: List[str], style: str):
    context = "\n".join(materials)
    prompt = f"Write a blog post in {style} style based on the following materials:\n\n{context}"
    return model_service.generate_response(prompt)

def search_recommendations(query: str):
    results = rag_service.search(query, k=3)
    recommendations = []

    # Extract titles and reasons
    # Note: RAG results are raw chunks. We need to parse metadata or use LLM to synthesize recommendation.
    # For now, let's just return the chunks and ask LLM to explain why they are relevant.

    if not results:
        return []

    context_str = "\n".join([f"Article: {doc.metadata.get('title', 'Unknown')}\nContent: {doc.page_content[:200]}..." for doc in results])

    prompt = f"Based on the following search results for query '{query}', recommend top articles and explain why:\n\n{context_str}"
    explanation = model_service.generate_response(prompt)

    # Ideally return structured JSON, but for now returning text explanation + raw results
    return {
        "explanation": explanation,
        "related_chunks": [doc.page_content for doc in results]
    }

# --- Endpoints ---

@app.post("/api/ai/ingest")
async def ingest_article(request: IngestRequest):
    """
    Ingest a new article into the vector database.
    """
    metadata = {
        "article_id": request.article_id,
        "title": request.title,
        "tags": ",".join(request.tags)
    }
    rag_service.add_document(request.content, metadata)
    return {"status": "success", "message": f"Article {request.article_id} ingested."}

@app.post("/api/ai/search")
async def search_articles(request: RecommendationRequest):
    """
    Search for articles using vector similarity.
    """
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
    Generate a blog draft based on materials.
    """
    draft = generate_draft(request.materials, request.style)
    return {"draft": draft}

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
