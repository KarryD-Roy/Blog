# 技能关系图谱与全栈权限体系迭代设计文档

## 1. 概述
本次迭代核心旨在将现存的分栏式“技能图谱”系统重构成兼具视觉冲击力和沉浸式交互的**知识关系拓扑图（力导向图）**。同时，依托此次前端改造引入完整的**JWT鉴权机制与用户角色体系**，实现图谱结构的后台管理隔离与普通用户浏览隔离。系统支持动态查询技能节点与博客文章的多对多关联，打通文章与知识点的通路。

---

## 2. 数据库模型设计

为满足图谱固化、多对多映射以及用户体系，对底层 `schema.sql` 做出如下改造或新增：

### 2.1 新增：`users` (用户表)
存储用于登录系统及权限控制的用户信息。
*   `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
*   `username`: `VARCHAR(50) UNIQUE`
*   `password`: `VARCHAR(100)` (Bcrypt 存储)
*   `role`: `VARCHAR(20)` (枚举/字符串，例如 `ROLE_ADMIN`, `ROLE_CREATOR`, `ROLE_USER`)
*   `created_at`, `updated_at`

### 2.2 修改：`skills` (技能表)
用于支持树状或力导向网状结构的关联以及拖拽坐标保存。
*   (已存在) `id`, `category`, `title`, `description`, `pinned`
*   (新增) `parent_id`: `BIGINT` (可为空，指向上级技能的ID)
*   (新增) `x_axis`: `DOUBLE` (用于固化 ECharts 图形节点X坐标)
*   (新增) `y_axis`: `DOUBLE` (用于固化 ECharts 图形节点Y坐标)
*   (新增) `version`: `INT DEFAULT 0` (乐观锁，用于处理多人同时拖拽节点的坐标覆写冲突)

### 2.3 新增：`post_skill_relation` (文章-技能中间表)
建立文章与多项技能点之间的双向多对多映射。
*   `post_id`: `BIGINT`
*   `skill_id`: `BIGINT`
*   *(联合主键：`post_id, skill_id`)*

---

## 3. 全栈认证与权限架构

### 3.1 基于 JWT 的认证体系
1.  **验证码机制**：后端使用 Captcha 工具生成图形验证码，将 Base64 图片返回前端，并在 Redis 缓存对应答案 (Key 为 `captcha:{uuid}`，TTL为5分钟)。
2.  **登录流程**：前端提交账号、密码、验证码原文及 uuid，Spring Security 校验通过后签发包含 `userId` 和 `role` 的 JWT token（时效如 24 小时）。
3.  **无感刷新与拦截**：
    *   前端使用全局 `axios` 拦截器。
    *   针对 `401 Unauthorized` 状态时，挂起当前所有请求。
    *   触发全局事件总线（Vue EventBus或Pinia），**拉起全局登录悬浮弹窗**（此时不刷新当前已加载好的 Vue 页面生命周期）。
    *   用户在弹窗中重新登录成功后，重设 Token，并重新发送之前被拦截挂起的请求。

### 3.2 角色权限控制 (RBAC)
*   **ADMIN / CREATOR** (管理员/创作者)：可以访问 `POST/PUT/DELETE /api/skills/**` 等编辑接口，前端页面展示图谱的“新增”、“修改”、“拖拽保存”及文章挂载按钮。
*   **USER / GUEST** (普通用户/访客)：仅放行 `GET` 接口请求，前端关闭 ECharts 节点的拖拽写回权限，隐藏全部修改入口。

---

## 4. 前端交互与Echarts视图设计

### 4.1 ECharts 图谱组件改型 (`SKills.vue`)
废弃原有的 `groupedSkills` 列表渲染，引入 Echarts `type: 'graph'` 渲染架构：
*   **布局引擎 (`layout`)**：初始化采用 `force`（力导向），带有缓慢阻尼系数，节点初始化后允许用户自由拖拽排布以达到最佳视觉效果。
*   **坐标固化保护**：管理员拖动节点后，触发防抖函数（Debounce），将当前 `x_axis` 和 `y_axis`通过乐观锁接口批量或单条保存至后端。下次加载默认启用 `layout: 'none'` 并使用预置的固定坐标直接绘制，防止每次刷新图谱节点乱飞。
*   **体量切割与平移放大 (Scale & Roam)**：开启 `roam: true` 支持全局鼠标滚轮缩放与画布平移。若未来节点超过一定数量(如>200)，可在图谱顶端增加分类（如前端、后端）的 Tab 页，实现分图渲染以保证性能。

### 4.2 理论知识与面试题侧边抽屉 (Drawer)
1.  在 Echarts 实例配置 `myChart.on('click', function(params){...})` 拦截点击事件。
2.  当点击事件发生在一个有效的技能点节点时，弹出右侧 Drawer 抽屉。
3.  **理论与面试题聚合**：抽屉上半部分作为该节点的理论知识库，重点展示使用 Markdown 渲染的理论知识和常见面试题汇总，帮助用户进行技术沉淀。
4.  **关联文章**：抽屉下半部分调用后端 `/api/skills/{id}/posts`，展示所有挂载至此知识点的关联实战博客，点击可直接跳转至详细页面。

### 4.3 技能节点关联编辑器
1.  在新建或编辑博客 (`Home.vue` 或未来的编辑专页) 时，引入树形多选组件 (如 `el-tree-select` 或自研平铺标签选择器)。
2.  提供技能库的快捷搜索，文章发布时通过 `PostController` 的事务处理持久化到中间表 `post_skill_relation`。

---

## 5. 后端核心接口设计

| 接口路径 | 方法 | 角色说明 | 功能描述 |
| :--- | :--- | :--- | :--- |
| `/api/auth/captcha` | GET | 所有人 | 获取 Base64 图形验证码及 uuid |
| `/api/auth/login` | POST | 所有人 | 提交账号密码和验证码，返回 Token |
| `/api/skills/graph` | GET | 所有人 | 获取全量技能关系(Node+Link数组) |
| `/api/skills/{id}/posts` | GET | 所有人 | 获取挂载至此技能点的文章分页列表 |
| `/api/skills` | POST | ADMIN/CREATOR | 新增知识点节点（定义 parent_id） |
| `/api/skills/coords` | PUT | ADMIN/CREATOR | 批量更新拖拽后的散点图坐标 (带 version 检查) |

---

## 6. 核心冲突机制：乐观锁设计 (Optimistic Locking)
为解决多个创作者同时对 `Skill` 节点进行坐标拖动导致的互相覆写问题：
*   后端使用 MyBatis-Plus 的 `@Version` 注解映射表中的 `version` 字段。
*   当获取图谱时，前端缓存各节点当前的 `version` 值。
*   当触发坐标 `PUT` 修改时，若传入的 `version` 和数据库当前 `version` 不一致（表示中间已被其它人修改），抛出 `StaleObjectStateException`。
*   前端捕获异常，提示“画布已被其他用户更新，坐标保存失败并拉取最新图谱”，保障多端协作数据最终一致。
