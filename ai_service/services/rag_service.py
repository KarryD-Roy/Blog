import os
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import DashScopeEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from typing import List, Dict, Any
from dotenv import load_dotenv

# Get the absolute path of the directory containing this file
current_dir = os.path.dirname(os.path.abspath(__file__))
# Go up one level to ai_service directory
ai_service_dir = os.path.dirname(current_dir)

load_dotenv()

class RAGService:
    def __init__(self):
        self.api_key = os.getenv("DASHSCOPE_API_KEY")
        # Use absolute path for chroma_db to avoid CWD issues
        default_chroma_path = os.path.join(ai_service_dir, "chroma_db")
        self.persist_directory = os.getenv("CHROMA_DB_PATH", default_chroma_path)

        if self.api_key:
             self.embeddings = DashScopeEmbeddings(dashscope_api_key=self.api_key, model="text-embedding-v1")
             # Initialize Chroma
             self.vector_store = Chroma(
                 persist_directory=self.persist_directory,
                 embedding_function=self.embeddings
             )
        else:
             self.embeddings = None
             self.vector_store = None
             print("Warning: DASHSCOPE_API_KEY not found. RAG functionality disabled.")

    def delete_document(self, article_id: str):
        """Delete all vector chunks associated with the given article_id."""
        if not self.vector_store:
            return

        try:
            self.vector_store.delete(where={"article_id": article_id})
        except Exception as e:
            print(f"Warning: Failed to delete document {article_id} from vector store: {e}")

    def add_document(self, content: str, metadata: Dict[str, Any]):
        if not self.vector_store:
            return

        # Delete existing chunks for this article to avoid duplicates on update
        article_id = metadata.get("article_id")
        if article_id:
            self.delete_document(str(article_id))
        else:
            print("Warning: add_document called without 'article_id' in metadata; skipping deduplication.")

        text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        # Create Document objects with metadata
        docs = [Document(page_content=chunk, metadata=metadata) for chunk in text_splitter.split_text(content)]

        self.vector_store.add_documents(docs)
        # In newer Chroma versions, persist() is often automatic, but keeping it for compatibility
        # self.vector_store.persist()

    def search(self, query: str, k: int = 3) -> List[Document]:
        if not self.vector_store:
            return []

        return self.vector_store.similarity_search(query, k=k)

    def search_with_score(self, query: str, k: int = 3):
        if not self.vector_store:
            return []

        return self.vector_store.similarity_search_with_score(query, k=k)
