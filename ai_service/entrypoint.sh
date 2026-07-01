#!/bin/bash
# ============================================
# AI Service 容器入口脚本
# 确保 ChromaDB 持久化目录在卷挂载后仍可写入
# ============================================

set -e

CHROMA_DIR="${CHROMA_DB_PATH:-/app/chroma_db}"

echo "[ENTRYPOINT] Ensuring ChromaDB directory: $CHROMA_DIR"

# 如果目录不存在则创建（卷挂载可能清除了构建时的目录）
if [ ! -d "$CHROMA_DIR" ]; then
    mkdir -p "$CHROMA_DIR"
    echo "[ENTRYPOINT] Created directory: $CHROMA_DIR"
fi

# 修复权限：确保当前用户（ai）有读写权
# —— 卷挂载可能由 docker daemon 以 root 创建，这里兜底修复
if [ ! -w "$CHROMA_DIR" ]; then
    echo "[ENTRYPOINT] Directory not writable, attempting chown..."
    # chown 需要 root 权限，若当前非 root 则跳过并警告
    if [ "$(id -u)" -eq 0 ]; then
        chown -R ai:ai "$CHROMA_DIR" 2>/dev/null || true
        echo "[ENTRYPOINT] chown completed."
    else
        echo "[ENTRYPOINT] WARNING: Running as non-root user and $CHROMA_DIR is not writable!"
        echo "[ENTRYPOINT] ChromaDB will fail with 'unable to open database file'."
        echo "[ENTRYPOINT] Please ensure the volume is owned by UID $(id -u) before starting."
    fi
fi

# 输出目录状态
ls -ld "$CHROMA_DIR" 2>/dev/null || echo "[ENTRYPOINT] Cannot stat directory"

echo "[ENTRYPOINT] Starting application..."
exec python -m uvicorn main:app --host 0.0.0.0 --port 8000
