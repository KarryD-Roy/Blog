# ============================================
# 前端 Vue + Vite - 多阶段构建
# ============================================

# ---- Stage 1: Node 构建 ----
FROM node:20-alpine AS build
WORKDIR /build

# 复制依赖文件并安装（利用缓存）
COPY package.json package-lock.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ---- Stage 2: Nginx 运行 ----
FROM nginx:alpine

# 复制构建产物
COPY --from=build /build/dist /usr/share/nginx/html

# 复制 Nginx 配置（容器内路径，覆盖默认配置）
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
