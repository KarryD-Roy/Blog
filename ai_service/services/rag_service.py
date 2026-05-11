import os
from langchain_chroma import Chroma
from langchain_community.embeddings import DashScopeEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from typing import List, Dict, Any
from dotenv import load_dotenv
try:
    from chromadb import PersistentClient
except Exception:
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

        if self.api_key:
             self.embeddings = DashScopeEmbeddings(dashscope_api_key=self.api_key, model="text-embedding-v2")
             if PersistentClient:
                 self.client = PersistentClient(path=self.persist_directory)
                 self.vector_store = Chroma(
                     client=self.client,
                     collection_name=self.collection_name,
                     embedding_function=self.embeddings
                 )
             else:
                 # Fallback to legacy initialization when PersistentClient is unavailable
                 self.vector_store = Chroma(
                     persist_directory=self.persist_directory,
                     embedding_function=self.embeddings,
                     collection_name=self.collection_name
                 )
        else:
             self.embeddings = None
             self.vector_store = None
             print("Warning: DASHSCOPE_API_KEY not found. RAG functionality disabled.")

    def is_ready(self) -> bool:
        return self.embeddings is not None and self.vector_store is not None

    def add_document(self, content: str, metadata: Dict[str, Any]) -> int:
        if not self.vector_store:
            return 0
        if not content or not content.strip():
            print(f"Skipping empty content for article {metadata.get('article_id')}")
            return 0

        text_splitter = RecursiveCharacterTextSplitter(
            separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""],
            chunk_size=500,
            chunk_overlap=50
        )
        # Create Document objects with metadata
        docs = [Document(page_content=chunk, metadata=metadata) for chunk in text_splitter.split_text(content)]
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

    def search(self, query: str, k: int = 3) -> List[Document]:
        if not self.vector_store:
            return []

        return self.vector_store.similarity_search(query, k=k)

    def search_with_score(self, query: str, k: int = 3):
        if not self.vector_store:
            return []

        return self.vector_store.similarity_search_with_score(query, k=k)
