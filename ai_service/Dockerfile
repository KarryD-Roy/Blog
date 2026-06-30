# ============================================
# AI 服务 (Python FastAPI) - 单阶段构建
# ============================================

FROM python:3.11-slim

WORKDIR /app

# 安装系统依赖（curl 用于健康检查，libsqlite3-dev 确保 ChromaDB SQLite 兼容性）
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    libsqlite3-dev \
    && pip install --upgrade pip \
    && rm -rf /var/lib/apt/lists/*

# 复制 requirements 并安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制项目源码
COPY . .

# 创建非 root 用户
RUN groupadd -r ai && useradd -r -g ai ai && \
    chown -R ai:ai /app

USER ai

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
