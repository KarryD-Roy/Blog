## 个人技术博客项目

本项目是一个前后端分离的个人技术博客示例，后端采用 **Java + Spring Boot + MySQL + MyBatis-Plus + Redis**，前端采用 **Vue + HTML/CSS/JavaScript**。

- **backend**：Spring Boot 后端服务，提供 REST API、持久化与缓存。
- **frontend**：Vue 单页应用，提供博客页面展示与常见博客功能。

### 快速开始

1. 安装并启动本地 MySQL 与 Redis。
2. 在 `backend/src/main/resources/application.yml` 中配置自己的数据库连接信息。
3. 进入 `backend` 目录，执行 `mvn spring-boot:run` 启动后端。
4. 进入 `frontend` 目录，执行 `npm install && npm run dev` 启动前端开发服务器。

后端会在启动时自动创建表结构，并通过 `schema.sql` / `data.sql` 向数据库中写入包含「技能」在内的示例初始数据。
