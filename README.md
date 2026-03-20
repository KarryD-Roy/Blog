## 个人技术博客项目

本项目是一个前后端分离的个人技术博客示例，后端采用 **Java + Spring Boot + MySQL + MyBatis-Plus + Redis**，前端采用 **Vue + HTML/CSS/JavaScript**。

- **backend**：Spring Boot 后端服务，提供 REST API、持久化与缓存。
- **frontend**：Vue 单页应用，提供博客页面展示与常见博客功能。

### 快速开始

1. 安装并启动本地 MySQL 与 Redis（Redis 默认配置：`localhost:6379`，可按需修改 `backend/src/main/resources/application.yml`）。
   - 如果本地有 Docker，可快速启动：

     ```bash
     docker run -d --name blog-redis -p 6379:6379 redis:7.2
     ```
2. 在 `backend/src/main/resources/application.yml` 中配置自己的数据库连接信息。
3. 进入 `backend` 目录，执行 `mvn spring-boot:run` 启动后端。
4. 进入 `frontend` 目录，执行：

   ```bash
   npm install
   # 依赖说明：
   # npm install markdown-it (已集成)
   # npm install echarts (新增图表支持)
   npm run dev
   ```

后端会在启动时自动创建表结构，并通过 `schema.sql` / `data.sql` 向数据库中写入包含「技能」在内的示例初始数据。

## 新增功能：AI 辅助模块 (New AI Assistant Module)

### 1. Python AI Service Setup

首先配置 Python 环境并安装依赖：
```bash
cd ai_service
# 建议使用 venv 虚拟环境
python -m venv venv
# Windows: venv\Scripts\activate
# Linux/Mac: source venv/bin/activate

pip install -r requirements.txt
```

配置环境变量：
复制 `.env.example` 为 `.env`，并在 `.env` 中填入你的 `DASHSCOPE_API_KEY`。

运行 AI 服务：
```bash
python main.py
```
服务运行在 `http://localhost:8000`。

### 2. 功能说明

- **AI 总结**：在文章详情页点击“✨ AI 一键总结”，AI 会通过 SSE 流式输出文章摘要。
- **AI 写作/推荐**：提供 `/api/ai/write` 和 `/api/ai/recommend` 接口供扩展开发。
- **RAG 检索**：文章发布或更新时，会自动同步到向量数据库 (ChromaDB)，支持基于语义的相似文章推荐。

---

### 缓存与 Redis 使用

- 已接入 Spring Cache + Redis：
  - 「热点资讯」接口 `GET /api/hot-news`：按日期或最新结果缓存，定时爬虫及启动爬虫后会自动清理缓存。
  - 「最新文章」接口 `GET /api/posts`：分页结果缓存，文章新增/编辑/删除/置顶会清空该缓存。
  - 「访问过的技能」：使用 Redis List 记录最近 20 条访问（`POST /api/skills/{id}/visit` 记录，`GET /api/skills/visited` 读取），并自动设置 7 天过期。
- 缓存键前缀：`blog::`，默认 TTL 30 分钟，可在 `backend/src/main/java/com/example/blog/config/CacheConfig.java` 调整。

---

### 新增功能概览

- **首页文章管理**：在首页通过弹窗新增、编辑、删除文章。
- **技能图谱管理**：在技能图谱页面通过弹窗新增、编辑、删除技能条目。
- **多内容格式支持**：文章正文支持纯文本、HTML 以及 Markdown 语法（前端自动渲染 Markdown）。
- **附件上传 + OSS**：正文中支持从本地上传图片 / 附件到阿里云 OSS，并自动插入可预览链接。
- **导航扩展**：导航栏新增「搜索」「文章大全」入口。
- **搜索模块**：根据关键词或标签检索文章，并提供标签快捷筛选。
- **文章大全模块**：集中查看所有文章，并按关键词、分类、标签进行筛选。
- **技术热点资讯**：
    - 首页左侧独立模块，展示每日技术热点（每日早上 8 点自动抓取/生成）。
    - 采用 **Swiper** 实现轮播效果，支持手势滑动与自动播放。
    - 点击卡片跳转至外链详情页。
- **文章统计**：
    - 独立的数据统计页面，集中展示博客运营数据（已修复图表显示异常，支持自适应缩放与横向布局）。
    - 使用 **Apache ECharts** 呈现。
    - 支持按 **分类占比**（饼图）和 **热门标签**（柱状图）统计文章。

---

### 首页文章展示与分页策略

- 入口：导航栏点击「首页」或访问 `/`。
- 首页文章展示逻辑：
  - 展示最新的 **12 篇文章**。
  - 采用分页展示，每页固定显示 **4 篇文章**（共 3 页）。
  - 卡片下方提供「上一页」/「下一页」导航按钮。
- **文章管理功能**（通过弹窗实现）：
  - **新增文章**：点击「新增文章」按钮，弹出编辑器对话框。
  - **编辑文章**：点击文章卡片下方的「编辑」按钮，进入编辑模式。
  - **删除文章**：点击文章卡片下方的「删除」按钮并确认。
  - **编辑器支持**：标题（必填）、摘要、标签（英文逗号分隔）、正文（支持 HTML/Markdown）、图片/附件上传。

---

### 技能图谱管理

- 入口：导航栏点击「技能图谱」或访问 `/skills`。
- 分页策略：每页展示 **6 个技能**，支持翻页查看更多。
- 排序逻辑：支持 **置顶优先**，置顶的技能会始终显示在列表最前端。
- 操作功能：
  - 点击「新增技能」打开弹窗添加。
  - 每个技能项支持 **编辑**、**置顶/取消置顶**、**删除** 操作。

---

### 搜索模块

- 入口：导航栏点击「搜索」或访问 `/search`。
- 功能集成：
  - **关键词搜索**：支持标题、摘要、正文、标签的全文检索（默认命中文档优先按照置顶、发布时间排序）。
  - **快捷标签**：点击标签云中的标签可快速过滤内容。
  - **分页策略**：搜索结果每页展示 **6 篇内容**。
- **ElasticSearch 支持（可选）**：
  - 后端已接入 Spring Data Elasticsearch，默认 `search.elasticsearch.enabled=false`，保持数据库模糊查询兼容。
  - 启用步骤：
    1) 本地启动 ES（示例：`docker run -d --name blog-es -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.12.2`）。
    2) 将 `backend/src/main/resources/application.yml` 中 `search.elasticsearch.enabled` 置为 `true`，按需填写 `uris`/`username`/`password`/`index`。
    3) 首次启用后调用 `POST /api/posts/reindex` 完成本地数据的全量回填，后续增删改会自动同步索引。
  - 失败降级：如果 ES 不可用或未启用，搜索接口自动回退到 MySQL 的 `LIKE` 检索。

---

### 文章大全模块

- 入口：导航栏点击「文章大全」或访问 `/posts`。
- 页面结构：
  - 顶部筛选区包含：
    - 关键词输入框：匹配标题 / 摘要 / 正文；
    - 分类下拉框：从获取所有分类；
    - 标签输入框：单个标签；
    - 「筛选」按钮：按当前条件过滤；
    - 「重置」按钮：清空所有条件并重新加载。
  - 下方是 **文章列表**，支持分页与置顶：
    - 展示标题、浏览量、发布时间、分类 ID（回显）、摘要及标签；
    - 文章列表使用统一分页大小：每页 10 条；
    - 列表按 **置顶优先** + 发布时间倒序 排序；
    - 支持在列表中对文章执行「置顶 / 取消置顶」；
    - 点击任意文章卡片进入详情页。
- **附件管理**：
    - 在详情页底部自动解析正文中的附件，提供「下载」与「在线预览」功能。

---

### 技术热点资讯（首页左侧）

- 展示每日更新的「技术热点资讯」轮播，可点击外链查看详情。
- 交互：自动轮播 + 手动点击指示点切换；支持滑动选择（移动端可横向滚动）。
- 数据来源：调用 `GET /api/hot-news`，默认返回最新 10 条，可按日期过滤（`?date=2026-03-16`）。
- 数据结构：`title`、`url`、`imageUrl`、`source`、`publishDate`。

---

### 首页文章展示与分页策略说明

- 首页（`/`）只展示「最新文章」，不再展示分页控件：
  - 前端固定请求 `page=1&size=10`，展示最新 10 条文章；
  - 新发布的文章会自动出现在首页顶部。
- 搜索与文章大全的分页策略：
  - 使用 `GET /api/posts/query`；
  - 统一分页大小为每页 10 条；
  - 排序规则：先按是否置顶，再按发布时间倒序。

---

### 前端通过 Nginx 部署

为了方便部署，项目根目录下已经准备了一个参考配置文件 `nginx.conf`。

1. **打包前端**：
   在 `frontend` 目录执行构建命令，生成 `dist` 静态资源目录：

   ```bash
   cd frontend
   npm install
   npm run build
   ```

2. **配置 Nginx**：
   - 确保已安装 Nginx（推荐下载 Windows 版解压即可）。
   - 打开项目根目录下的 `nginx.conf` 文件。
   - 确认 `root` 路径是否正确指向了你的本地 `dist` 目录（当前配置为 `C:/Users/Karry/Desktop/Trae01/Blog/frontend/dist`）。
   - 将 `nginx.conf` 中的内容 **覆盖** 到 Nginx 安装目录下的 `conf/nginx.conf` 文件中；或者并在启动时指定该配置文件。

3. **启动 Nginx**：

   ```bash
   # 在 Nginx 安装目录下
   start nginx.exe
   # 或者指定配置文件启动
   # nginx -c C:/Users/Karry/Desktop/Trae01/Blog/nginx.conf
   ```

4. **验证**：
   确保后端服务已启动（`mvn spring-boot:run`），访问 `http://localhost/` 即可。

---

### Markdown 与内容格式支持说明

前端通过 `markdown-it` 自动将 Markdown 文本渲染为 HTML，在文章详情页中展示。

- 依赖安装：

  ```bash
  cd frontend
  npm install markdown-it
  ```

- 使用建议：
  - 在首页「新增 / 编辑文章」的正文输入框中直接输入 Markdown；
  - 如果正文以 `<` 开头（例如以 `<p>` `<h1>` `<img>` 等 HTML 标签开头），系统会按 **原始 HTML** 渲染；
  - 否则将按 Markdown 语法渲染。
- 图片支持：
  - 可以直接使用 Markdown 语法：`![说明文字](图片地址)`；
  - 或者在正文中写 `<img src="图片地址" alt="说明文字" />`。

> 安全提示：目前为个人博客示例，前端会直接渲染后端返回的 HTML / Markdown 渲染结果，上线到公网时请补充 XSS 等安全防护。
