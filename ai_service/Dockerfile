# ============================================
# AI 服务 (Python FastAPI) - 单阶段构建
# 无 apt-get 依赖：sqlite3 内置于 Python，健康检查用 urllib
# ============================================

FROM python:3.11-slim

WORKDIR /app

# 复制 requirements 并安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制项目源码及入口脚本
COPY . .

# --- 权限准备（在切换用户前完成）---
# 预创建 ChromaDB 数据目录，卷挂载时会继承此权限
RUN mkdir -p /app/chroma_db && \
    groupadd -r ai && useradd -r -g ai ai && \
    chown -R ai:ai /app && \
    chmod +x /app/entrypoint.sh

USER ai

EXPOSE 8000

ENTRYPOINT ["/app/entrypoint.sh"]
