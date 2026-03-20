import os
from langchain_community.chat_models.tongyi import ChatTongyi
from langchain_core.messages import HumanMessage, SystemMessage
from dotenv import load_dotenv

load_dotenv()

class ModelService:
    def __init__(self):
        self.api_key = os.getenv("DASHSCOPE_API_KEY")
        # Initialize the model if API key is present
        if self.api_key:
            # Using qwen-plus as a default efficient model
            self.llm = ChatTongyi(dashscope_api_key=self.api_key, model="qwen-plus")
        else:
            print("Warning: DASHSCOPE_API_KEY not found. AI features will be mocked.")
            self.llm = None

    def generate_response(self, prompt: str, system_prompt: str = "You are a helpful AI assistant."):
        if not self.llm:
            return "AI Service is in mock mode. Please configure DASHSCOPE_API_KEY."

        messages = [
            SystemMessage(content=system_prompt),
            HumanMessage(content=prompt)
        ]
        response = self.llm.invoke(messages)
        return response.content

    async def generate_stream(self, prompt: str, system_prompt: str = "You are a helpful AI assistant."):
        if not self.llm:
             yield "AI Service is in mock mode. Please configure DASHSCOPE_API_KEY."
             return

        messages = [
            SystemMessage(content=system_prompt),
            HumanMessage(content=prompt)
        ]
        async for chunk in self.llm.astream(messages):
            yield chunk.content
