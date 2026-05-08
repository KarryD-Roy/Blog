import os
import time
from langchain_community.chat_models.tongyi import ChatTongyi
from langchain_core.messages import HumanMessage, SystemMessage
from dotenv import load_dotenv

load_dotenv()

class ModelService:
    def __init__(self):
        self.api_key = os.getenv("DASHSCOPE_API_KEY")
        self.default_model = os.getenv("DEFAULT_MODEL", "qwen-turbo")
        self.alias_map = {
            "qwen-3.5-plus": "qwen-plus",
            "qwen3.5-plus": "qwen-plus",
            "qwen-3.5": "qwen-plus",
        }
        self._llm_cache = {}
        if not self.api_key:
            print("Warning: DASHSCOPE_API_KEY not found. AI features will be mocked.")

    def _resolve_model(self, model_name: str | None) -> str:
        if not model_name:
            return self.default_model
        return self.alias_map.get(model_name, model_name)

    def _get_llm(self, model_name: str | None):
        resolved = self._resolve_model(model_name)
        if not self.api_key:
            return None
        if resolved not in self._llm_cache:
            self._llm_cache[resolved] = ChatTongyi(
                dashscope_api_key=self.api_key,
                model=resolved
            )
        return self._llm_cache[resolved]

    def generate_response_with_feedback(self, prompt: str, system_prompt: str = "You are a helpful AI assistant.", model_name: str | None = None):
        """
        Generates a response including thinking process and total time.
        Returns a dict: { "content": str, "thinking_process": str, "total_time": str }
        """
        llm = self._get_llm(model_name)
        if not llm:
            return {
                "content": "AI Service is in mock mode. Please configure DASHSCOPE_API_KEY.",
                "thinking_process": "Mock thinking process...",
                "total_time": "0.0s"
            }

        start_time = time.time()

        enhanced_system_prompt = (
            f"{system_prompt}\n"
            "IMPORTANT: You must first engage in deep thinking before answering. "
            "Enclose your thinking process within <think>...</think> tags. "
            "Then provide your final answer in Chinese."
        )

        messages = [
            SystemMessage(content=enhanced_system_prompt),
            HumanMessage(content=prompt)
        ]

        try:
            response = llm.invoke(messages)
            full_content = response.content

            end_time = time.time()
            duration = f"{end_time - start_time:.2f}s"

            thinking_process = ""
            final_content = full_content

            if "<think>" in full_content and "</think>" in full_content:
                start_idx = full_content.find("<think>") + 7
                end_idx = full_content.find("</think>")
                thinking_process = full_content[start_idx:end_idx].strip()
                final_content = full_content[end_idx + 8:].strip()

            return {
                "content": final_content,
                "thinking_process": thinking_process,
                "total_time": duration
            }

        except Exception as e:
            print(f"Error generating response: {e}")
            return {
                "content": "Error generating response. Please try again.",
                "thinking_process": "",
                "total_time": "0.0s"
            }

    def generate_response(self, prompt: str, system_prompt: str = "You are a helpful AI assistant.", model_name: str | None = None):
        llm = self._get_llm(model_name)
        if not llm:
            return "AI Service is in mock mode. Please configure DASHSCOPE_API_KEY."

        messages = [
            SystemMessage(content=system_prompt),
            HumanMessage(content=prompt)
        ]
        response = llm.invoke(messages)
        return response.content

    async def generate_stream(self, prompt: str, system_prompt: str = "You are a helpful AI assistant.", model_name: str | None = None):
        llm = self._get_llm(model_name)
        if not llm:
             yield "AI Service is in mock mode. Please configure DASHSCOPE_API_KEY."
             return

        enhanced_system_prompt = (
            f"{system_prompt}\n"
            "IMPORTANT: Start by thinking deeply about the user's request. ENCLOSE YOUR ENTIRE THINKING PROCESS WITHIN <think>...</think> TAGS. Think step-by-step.\n"
            "After thinking, provide the final response. Use standard Markdown formatting: use '# ' for title once, '## ' for main sections, '### ' for subsections; avoid '####'."
        )

        messages = [
            SystemMessage(content=enhanced_system_prompt),
            HumanMessage(content=prompt)
        ]

        try:
            async for chunk in llm.astream(messages):
                yield chunk.content
        except Exception as e:
            print(f"Error streaming response: {e}")
            yield f"Error: {str(e)}"
