# KARRY TECH BLOG

前后端分离的多用户个人技术博客系统。后端基于 **Spring Boot 3.2.5 + MySQL + MyBatis-Plus + Redis + RabbitMQ + Spring Security + JWT**，前端基于 **Vue 3 + Vite + Vue Router**。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 运行时 | Java | 17 |
| 框架 | Spring Boot | 3.2.5 |
| 持久层 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | — |
| 缓存 | Redis | — |
| 消息队列 | RabbitMQ | — |
| 全文检索 | Elasticsearch | 8.x（可选） |
| 安全 | Spring Security + JWT | jjwt 0.12.5 |
| 前端 | Vue 3 + Vite | 3.4.21 / 5.2.0 |
| 路由 | Vue Router | 4.3.0 |
| AI 服务 | Python FastAPI + ChromaDB | — |

## 目录结构

```
Blog/
├── backend/                          # Spring Boot 后端服务
│   └── src/main/
│       ├── java/com/example/blog/
│       │   ├── config/               # 配置（缓存、ES、RabbitMQ、数据初始化）
│       │   ├── controller/           # REST 控制器
│       │   ├── dto/                  # 数据传输对象
│       │   ├── entity/               # 数据库实体类
│       │   ├── mapper/               # MyBatis-Plus Mapper
│       │   ├── search/               # ES 搜索策略
│       │   ├── security/             # JWT + Security 配置
│       │   └── service/              # 业务服务层
│       └── resources/
│           ├── application.yml       # 主配置
│           ├── schema.sql            # 建表脚本
│           └── data.sql              # 示例数据
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # Axios API 封装层
│       ├── components/               # 可复用组件
│       ├── directives/               # 自定义指令
│       ├── router/                   # 路由配置与守卫
│       ├── static/                   # 静态资源
│       ├── stores/                   # 响应式状态管理
│       ├── views/                    # 页面视图组件
│       │   ├── auth/                 # 登录/注册
│       │   └── user/                 # 个人中心/消息
│       ├── App.vue                   # 根组件（导航栏 + 用户菜单）
│       ├── main.js                   # 入口
│       └── styles.css                # 全局暗色主题样式
├── ai_service/                       # Python AI 服务
├── nginx.conf                        # Nginx 部署配置
├── plan-blogMultiUserRefactoring.prompt.md  # 多用户改造设计文档
└── README.md
```

---

## 快速开始

### 前置依赖

确保以下服务已启动：

| 服务 | 默认地址 | 说明 |
|------|----------|------|
| MySQL | `localhost:3306` | 需预先创建 `blog_db` 数据库 |
| Redis | `localhost:6379` | 用于缓存与技能访问记录 |
| RabbitMQ | `localhost:5672` | 用于审核通知异步解耦（启动失败时自动降级 Fallback） |
| Elasticsearch | `localhost:9201` | 全文检索（Docker Compose 默认启用） |

快速启动基础依赖（Docker）：

```bash
# MySQL
docker run -d --name blog-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD= -e MYSQL_DATABASE=blog_db mysql:8.0

# Redis
docker run -d --name blog-redis -p 6379:6379 redis:7.2

# RabbitMQ
docker run -d --name blog-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.12-management

# Elasticsearch（可选）
docker run -d --name blog-es -p 9201:9200 -e "discovery.type=single-node" -e "xpack.security.enabled=false" elasticsearch:8.12.2
```

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后会自动执行 `schema.sql` 创建表结构（需要 `application.yml` 中 `spring.sql.init.mode` 不为 `never`）。同时 `DataInitConfig` 会初始化角色和默认用户：

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | `admin` | `admin123` | ADMIN, USER |
| 普通用户 | `user` | `user123` | USER |

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`，API 请求自动代理至 `localhost:8080`。

### 启动 AI 服务（可选）

```bash
cd ai_service
pip install -r requirements.txt
cp .env.example .env          # 填入 DASHSCOPE_API_KEY
python main.py                # 运行在 http://localhost:8000
```

---

## Docker Compose 一键部署（推荐）

使用 Docker Compose 一键启动全部依赖服务和前后端应用，无需手动安装 MySQL、Redis、RabbitMQ、Elasticsearch。

### 前置要求

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| Docker | 20.10+ | 需安装 Docker Engine |
| Docker Compose | 2.0+ | 通常随 Docker Desktop 一起安装 |
| Git | — | 克隆项目代码 |

### 部署步骤

#### 1. 配置环境变量

```bash
# 复制并编辑 .env 文件
cp .env.example .env    # 如不存在 .env.example，直接编辑项目根目录下的 .env
```

关键配置项说明：

```bash
# -------- 必填项 --------
MYSQL_ROOT_PASSWORD=root123              # MySQL root 密码，生产环境务必修改
JWT_SECRET=YourSuperSecretKey2024!        # JWT 签名密钥，生产环境务必修改为强密钥
DASHSCOPE_API_KEY=sk-xxxxxxxxxxxx         # 通义千问 API Key（AI 服务必须）
SEARCH_ELASTICSEARCH_ENABLED=true         # 启用 Elasticsearch 全文检索

# -------- 可选项 --------
ALIYUN_OSS_ACCESS_KEY_ID=xxxxx            # 阿里云 OSS AK（不使用可留空）
ALIYUN_OSS_ACCESS_KEY_SECRET=xxxxx        # 阿里云 OSS SK（不使用可留空）
```

#### 2. 构建并启动全部服务

```bash
# 在项目根目录（docker-compose.yml 所在目录）执行
docker compose build          # 构建前后端镜像
docker compose up -d           # 后台启动全部服务
```

启动顺序由 Docker Compose 自动编排：
```
MySQL → Redis → RabbitMQ → Elasticsearch  (并行启动)
                ↓ (等待全部健康检查通过)
             Backend (Spring Boot)
                ↓
             Frontend (Nginx)
```

#### 3. 验证服务状态

```bash
# 查看所有容器状态（全部显示 healthy 表示就绪）
docker compose ps

# 预期输出
# NAME              STATUS
# blog-mysql        healthy
# blog-redis        healthy
# blog-rabbitmq     healthy
# blog-es           healthy
# blog-ai-service   healthy
# blog-backend      running
# blog-frontend     running
```

#### 4. 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 博客首页 | `http://localhost` | Nginx 端口 80 |
| 后端 API | `http://localhost:8080` | Spring Boot 端口 8080 |
| AI 服务 | `http://localhost:8000` | FastAPI 端口 8000 |
| Elasticsearch | `http://localhost:9201` | ES HTTP API（内部 9200，宿主机映射 9201） |
| RabbitMQ 管理 | `http://localhost:15672` | guest / guest |
| MySQL | `localhost:3307` | root 密码见 .env |

#### 5. 初始化 Elasticsearch 索引

首次启动后，调用后端 API 将数据库中的文章回填到 ES 索引：

```bash
curl -X POST http://localhost:8080/api/posts/reindex
```

#### 6. 查看日志

```bash
# 查看所有服务日志
docker compose logs -f

# 查看指定服务日志
docker compose logs -f backend
docker compose logs -f elasticsearch

# 检查 ES 启动日志（确认无报错）
docker compose logs elasticsearch | grep -i error
```

### 停止与清理

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 将清除所有持久化数据）
docker compose down -v
```

### 端口映射总览

| 容器 | 容器内端口 | 宿主机端口 | 说明 |
|------|-----------|-----------|------|
| MySQL | 3306 | 3307 | 避免与主机 MySQL 冲突 |
| Redis | 6379 | 6379 | — |
| RabbitMQ | 5672 | 5672 | AMQP 协议 |
| RabbitMQ Management | 15672 | 15672 | Web 管理面板 |
| Elasticsearch HTTP | 9200 | 9201 | REST API |
| Elasticsearch Transport | 9300 | 9301 | 集群内部通信 |
| AI Service | 8000 | 8000 | Python FastAPI |
| Backend | 8080 | 8080 | Spring Boot |
| Frontend | 80 | 80 | Nginx |

> **注意**：容器间通过 Docker 内部网络 `blog-network` 通信，使用容器名作为主机名（如 `elasticsearch:9200`），与宿主机映射端口无关。

---

## 多用户系统

> 本次重大更新将单用户博客全面升级为多用户系统，引入身份认证、权限控制、社交互动与消息通知机制。

### 核心变更

| 维度 | 变更内容 |
|------|----------|
| 认证 | Spring Security + JWT 无状态 Token 认证，24h 过期 |
| 授权 | 文章/技能按属主校验；Admin 拥有全部管理权限 |
| 用户 | 注册/登录，默认分配 USER 角色，支持昵称/邮箱/简介 |
| 评论 | 文章详情页支持嵌套评论与回复，作者或 Admin 可删除 |
| 点赞 | 文章点赞/取消，实时计数，用户去重 |
| 消息 | 站内信通知，审核结果 RabbitMQ 异步推送，前端 30s 轮询红点 |
| 审核 | Admin 专区审核 PENDING 文章/技能，消息通过 RabbitMQ 异步触达 |

### 新增 API 端点

#### 认证（公开）

```
POST /api/auth/register         注册新用户（Body: username, password, email?, nickname?）
POST /api/auth/login            用户登录，返回 JWT Token 与用户信息
GET  /api/auth/profile          获取当前登录用户信息（需 Token）
```

#### 用户管理

```
GET  /api/users/profile         当前用户个人信息
GET  /api/users                 所有用户列表（Admin only）
```

#### 评论

```
GET    /api/posts/{postId}/comments           获取文章评论列表（公开）
POST   /api/posts/{postId}/comments           添加评论/回复（需登录，Body: content, parentId?）
DELETE /api/posts/{postId}/comments/{id}      删除评论（作者或 Admin）
```

#### 点赞

```
GET  /api/posts/{postId}/like    获取点赞状态与总数
POST /api/posts/{postId}/like    切换点赞/取消点赞（需登录）
```

#### 消息通知

```
GET  /api/messages                分页获取当前用户消息
GET  /api/messages/unread-count   获取未读消息数（用于前端红点轮询）
PUT  /api/messages/{id}/read      标记单条消息已读
PUT  /api/messages/read-all       全部标记已读
```

#### 管理员审核

```
GET  /api/admin/review/posts          待审核文章列表
GET  /api/admin/review/skills         待审核技能列表
PUT  /api/admin/review/posts/{id}     审核文章（Body: status, reason?）
PUT  /api/admin/review/skills/{id}    审核技能（Body: status, reason?）
```

#### 已有接口行为变更

| 接口 | 变更 |
|------|------|
| `POST /api/posts` | 需登录，自动设置 `userId` 和 `status=PUBLISHED` |
| `PUT /api/posts/{id}` | 需属主或 Admin 权限 |
| `DELETE /api/posts/{id}` | 需属主或 Admin 权限 |
| `POST/PUT/DELETE /api/skills/*` | 写操作需认证 |

### 安全架构

```
请求 → JwtAuthenticationFilter（解析 Bearer Token）
     → SecurityContextHolder（认证信息注入）
     → UserContext（ThreadLocal 透传 userId）
     → AuthInterceptor（preHandle 记录 / afterCompletion 清理 ThreadLocal）
     → Controller（@PreAuthorize 方法级权限校验 + canModify 属主校验）
```

- `UserContext.clear()` 在拦截器 `afterCompletion()` 中强制调用，**杜绝内存泄漏**。
- 登录/注册端点放行，GET 类查询接口公开，写操作需认证。

---

## 数据库表结构（共 12 张表）

| 表名 | 说明 | 新增/变更 |
|------|------|-----------|
| `users` | 用户表 | 🆕 本次新增 |
| `roles` | 角色表（ADMIN / USER） | 🆕 本次新增 |
| `user_roles` | 用户-角色关联 | 🆕 本次新增 |
| `messages` | 站内信通知 | 🆕 本次新增 |
| `comments` | 文章评论（支持嵌套） | 🆕 本次新增 |
| `post_likes` | 文章点赞（唯一约束防重复） | 🆕 本次新增 |
| `posts` | 文章表 | 🔄 新增 `user_id`, `status` 字段 |
| `skills` | 技能树节点 | 🔄 新增 `user_id`, `status` 字段 |
| `categories` | 文章分类 | — 未变更 |
| `hot_news` | 热点资讯 | — 未变更 |
| `theories` | 技能理论知识 | — 未变更 |
| `post_skill_relation` | 文章-技能多对多关联 | — 未变更 |

---

## 配置参数

### application.yml 新增配置

```yaml
# JWT 配置
jwt:
  secret: YourSuperSecretKeyForJWTTokenGenerationAndValidation2024!
  expiration: 86400000        # Token 有效期 24 小时（毫秒）

# RabbitMQ 配置（spring.rabbitmq 下）
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3         # 消费失败重试 3 次
          initial-interval: 1000  # 初始重试间隔 1 秒
```

### Cache 配置

- 缓存键前缀：`blog:v2::`
- 默认 TTL：30 分钟（`CacheConfig.java` 中配置）
- 文章列表缓存：文章新增/编辑/删除/置顶自动驱逐
- 热点资讯缓存：按日期缓存，爬虫更新时驱逐

---

## 前端架构

### 路由表

| 路径 | 页面 | 认证要求 |
|------|------|----------|
| `/` | 首页（热点资讯 + 文章列表） | 公开 |
| `/login` | 登录 | 仅游客 |
| `/register` | 注册 | 仅游客 |
| `/posts` | 文章大全（筛选 + 分页） | 公开 |
| `/posts/:id` | 文章详情（含评论 + 点赞 + AI 总结） | 公开（评论/点赞需登录） |
| `/skills` | 技能树力导向图 | 公开 |
| `/search` | 搜索（关键词 + 标签筛选） | 公开 |
| `/statistics` | 数据统计图表 | 公开 |
| `/profile` | 个人中心 | 需登录 |
| `/messages` | 我的消息 | 需登录 |
| `/ai/writer` | AI 帮写 | 公开 |
| `/ai/recommendation` | AI 文章推荐 | 公开 |
| `/theory/:skillId` | 理论知识详情 | 公开 |

### 路由守卫

- `requiresAuth`：未登录自动跳转 `/login`，登录后重定向回原页面
- `guest`：已登录用户访问登录/注册页时自动跳转首页

### Axios 拦截器

- **请求拦截**：自动从 `localStorage` 读取 Token 附加到 `Authorization: Bearer xxx` 请求头
- **响应拦截**：`401/403` 时自动清除本地 Token 并跳转登录页

### 消息红点轮询

- 用户登录后，前端每 **30 秒** 调用 `GET /api/messages/unread-count` 更新导航栏红点数
- 登出或离开页面时自动停止轮询

### 新增前端文件

```
src/api/
├── index.js          # Axios 实例 + 拦截器
├── auth.js           # 认证 API
├── comments.js       # 评论 API
├── like.js           # 点赞 API
└── messages.js       # 消息 API

src/stores/
├── auth.js           # 用户登录态管理（localStorage 持久化）
└── message.js        # 未读消息数 + 轮询控制

src/components/
├── CommentSection.vue  # 评论区（树形嵌套、回复、删除）
└── LikeButton.vue      # 点赞按钮（心形动画、乐观更新）

src/views/
├── auth/
│   ├── Login.vue       # 登录页面
│   └── Register.vue    # 注册页面
└── user/
    ├── Profile.vue     # 个人中心
    └── Messages.vue    # 消息中心
```

---

## RabbitMQ 审核通知流程

```
[Admin 审核] → PUT /api/admin/review/posts/{id}
             → ReviewService.reviewPost()
             → {{rabbitTemplate}}.convertAndSend("review.exchange", "review.notification", ...)
             → [RabbitMQ Broker]
             → NotificationListener.handleReviewNotification()
             → MessageService.sendMessage() → 写入 messages 表
             → 前端轮询 GET /api/messages/unread-count → 红点提醒
```

**可靠投递保障**：
- 消息队列声明为 `durable`（持久化）
- 配置死信队列 `review.dlx.exchange`
- RabbitTemplate 配置了 `ConfirmCallback` 与 `ReturnsCallback`
- RabbitMQ 不可用时，ReviewService 直接降级写入 Message，不丢失通知

---

## AI 辅助模块

> 配置与启动详见上方「启动 AI 服务」章节。

- **AI 总结**：文章详情页点击 "✨ AI 一键总结"，通过 SSE 流式输出摘要，支持思考过程可视化
- **AI 写作助手**：集成阿里 **Qwen-Max** 旗舰模型，前端支持风格选择与暗色玻璃拟态界面
- **RAG 检索**：文章发布/更新自动同步至 ChromaDB 向量数据库，支持语义相似文章推荐

---

## Elasticsearch 全文检索

### 版本兼容

| 组件 | 版本 | 说明 |
|------|------|------|
| Elasticsearch Server | 8.12.2 | Docker 镜像 `elasticsearch:8.12.2` |
| spring-data-elasticsearch | 5.2.5 | Spring Boot 3.2.5 自带 |
| elasticsearch-java | 8.10.4+ | 新版 Java API Client |

> **兼容性说明**：Elasticsearch Server 8.12.x 向后兼容 8.10+ 客户端，上述版本组合已经过验证可正常工作。

### Docker Compose 部署（默认启用）

Docker Compose 中 ES 服务已默认启用，无需额外配置：
- 单节点模式（`discovery.type=single-node`）
- 安全认证关闭（`xpack.security.enabled=false`）
- 宿主机端口映射：`9201:9200`（HTTP）、`9301:9300`（Transport）
- 后端通过容器内部网络访问：`http://elasticsearch:9200`

### 本地开发

如果不在 Docker 中运行，手动启动 ES：

```bash
docker run -d --name blog-es -p 9201:9200 -p 9301:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:8.12.2
```

### 启用/禁用

- **启用**：`.env` 中设置 `SEARCH_ELASTICSEARCH_ENABLED=true`（默认）
- **禁用**：设置为 `false`，搜索将自动回退到 MySQL `LIKE` 查询
- 首次启用后调用 `POST /api/posts/reindex` 全量回填索引
- 失败降级：ES 不可用时自动回退 MySQL `LIKE` 查询

---

## Markdown 支持

- 前端通过 `markdown-it` + `highlight.js` 自动渲染 Markdown 为语法高亮 HTML
- 正文以 `<` 开头时按原始 HTML 渲染，否则按 Markdown 渲染
- 已集成 `DOMPurify` XSS 过滤防护

---

## Nginx 部署

```bash
cd frontend
npm run build

# 将 nginx.conf 覆盖至 Nginx conf 目录，或指定配置文件启动
nginx -c C:/path/to/Blog/nginx.conf
```

---

## 项目资源

| 项 | 说明 |
|-----|------|
| 后端端口 | `8080` |
| 前端 dev 端口 | `5173`（`npm run dev`） |
| AI 服务端口 | `8000`（Python FastAPI） |
| Elasticsearch | `localhost:9201`（Docker，容器内 `elasticsearch:9200`） |
| MySQL 数据库 | `blog_db`，用户名 `root` |
| Redis | `localhost:6379` |
| RabbitMQ 管理面板 | `http://localhost:15672`（guest/guest） |
| Docker Compose | `docker compose up -d` 一键启动全部服务 |
