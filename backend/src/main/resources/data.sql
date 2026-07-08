SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO roles (name) VALUES
('ADMIN'),
('USER');

-- 注意：用户(admin / user)与角色关联改由应用启动时的 DataInitConfig 统一创建，
-- 使用 BCryptPasswordEncoder(10) 对 admin123 / user123 加密，确保哈希与 SecurityConfig 完全一致。
-- 上方已临时关闭外键检查，允许 posts / comments 等依赖 user_id=1 的种子数据在用户创建前插入，
-- DataInitConfig 会自动以 id=1 创建 admin 用户，外键随后自然满足。

INSERT INTO skills (category, title, description, pinned, user_id, status, parent_id, x_axis, y_axis, version) VALUES
('编程语言与基础能力', 'Java 及面向对象基础', '### 核心理论
熟练掌握 Java 语言，深入理解面向对象编程、异常处理、集合框架、并发编程；熟悉常见数据结构及算法；了解常用设计模式，如工厂、责任链、策略等。

### 常见面试题
1. **说说你对 OOP 的理解？**
2. **ArrayList 和 LinkedList 的区别是什么？**
3. **谈谈 Java 中的 `synchronized` 关键字。**', 1, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('主流开发框架', 'Spring 生态与微服务', '### 核心理论
熟练使用 Spring Boot，熟悉 Spring Cloud 及相关组件如 Nacos、OpenFeign、Gateway 等；熟练使用 MyBatis/MyBatis-Plus 实现 ORM 映射；理解 AOP 面向切面编程思想。

### 常见面试题
1. **Spring 中 Bean 的生命周期是怎样的？**
2. **解释一下 Spring AOP 的底层实现原理。**
3. **什么是微服务中的服务熔断与降级？**', 1, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('AI 与智能体开发', 'LangChain 与 RAG', '### 核心理论
掌握 LangChain 框架，了解 RAG (检索增强生成) 与 Spring AI，熟悉提示词工程，包括指令提示与上下文注入。

### 常见面试题
1. **简述 RAG (Retrieval-Augmented Generation) 的核心流程。**
2. **什么是 LangChain 中的 Agent 机制？**
3. **如何解决大模型生成时的"幻觉"问题？**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('数据库与存储技术', 'MySQL/SQL Server/Oracle', '### 核心理论
熟悉关系型数据库，具备 SQL 调优经验、索引优化、慢查询处理；熟悉 Redis 缓存机制；熟悉 Elasticsearch 全文检索。

### 常见面试题
1. **描述 MySQL 索引的最左匹配原则。**
2. **讲讲 Redis 常见的缓存击穿、穿透与雪崩问题及解决思路。**
3. **Elasticsearch 倒排索引的原理是什么？**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('中间件与消息系统', 'RabbitMQ/ActiveMQ/Redis 消息', '### 核心理论
熟悉 RabbitMQ、ActiveMQ，用于系统解耦与异步处理；熟练部署与配置 Redis，支持集群模式与持久化策略。

### 常见面试题
1. **如何保证消息队列的消息不丢失？**
2. **解释 RabbitMQ 的死信队列。**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('系统与运维能力', 'Linux 运维与容器化', '### 核心理论
熟悉 Linux 常用命令；熟练使用 Docker 构建镜像、编排容器，完成 MySQL、Redis、Nginx 等中间件部署。

### 常见面试题
1. **简述 Docker 的核心资源隔离技术。**
2. **常用排查线上系统 CPU 飙高的 Linux 命令有哪些？**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('前端与工程化', 'Vue3 / Vite', '### 核心理论
熟练使用 Vue 3 组合式 API、Vue Router；熟悉 Vite 构建与前端工程化。

### 常见面试题
1. **Vue 3 的响应式原理 (Proxy) 相比 Vue 2 有何提升？**
2. **简述 Pinia 的核心概念。**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('测试与质量保障', '单元测试与接口测试', '### 核心理论
熟练使用 JUnit 5、Mockito 编写单元测试；熟悉 Postman、Apifox 做接口测试；了解 JMeter 做压测。

### 常见面试题
1. **什么是 Mock 测试？它解决了什么问题？**
2. **你在项目中是如何进行单元测试覆盖率检查的？**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('架构与设计', '分布式与高可用', '### 核心理论
了解微服务拆分、服务治理与分布式事务；熟悉常见高可用方案如限流、熔断、降级。

### 常见面试题
1. **谈谈著名的 CAP 定理。**
2. **常见分布式锁的实现方案有哪些？**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0),
('开发工具链', 'Git / IDEA / 协作', '### 核心理论
熟练使用 Git 分支策略、Code Review 与 CI/CD 流水线。

### 常见面试题
1. **`git merge` 与 `git rebase` 有什么区别？**
2. **你如何理解和实践 CI/CD 流程？**', 0, 1, 'PUBLISHED', NULL, NULL, NULL, 0);

INSERT INTO categories (name, description) VALUES
('后端开发', 'Java、Spring 生态和服务端技术'),
('AI 与大模型', '大语言模型、智能体、RAG 等'),
('数据库与中间件', '数据库、Redis、消息队列等技术分享'),
('系统与运维', 'Linux、Docker、运维与部署'),
('前端开发', 'Vue、前端工程化与交互'),
('架构与软技能', '设计模式、架构思路与团队协作');

INSERT INTO posts (title, summary, content, user_id, category_id, tags, view_count, pinned, status, created_at, updated_at) VALUES
('基于 Spring Boot 和 MyBatis-Plus 搭建个人博客后端', '从零搭建一个支持分页、分类的博客后端服务，实现常见 CRUD 能力。', '本文将介绍如何使用 Spring Boot 3.x + MyBatis-Plus 搭建一个简洁易扩展的博客后端，包括实体设计、分页查询、统一返回结果以及基础的异常处理思路。', 1, 1, 'Spring Boot,MyBatis-Plus,后端实战', 12, 1, 'PUBLISHED', NOW(), NOW()),
('使用 Redis 为博客系统加速：缓存常见实践', '讲解如何在个人博客中使用 Redis 做缓存，提高首页和文章详情的响应速度。', '在真实场景中，博客首页文章列表和热门技能信息访问频率很高，我们可以借助 Redis 将这些数据缓存起来，并设计合理的失效策略以避免缓存雪崩。', 1, 3, 'Redis,缓存,性能优化', 8, 0, 'PUBLISHED', NOW(), NOW()),
('初探 LangChain 与 Spring AI：让博客具备问答能力', '将 AI 能力集成到博客中，让读者可以就文章内容进行自然语言问答。', '本文会演示如何使用 LangChain 与 Spring AI 封装大模型调用，将博客文章内容索引到向量库中，实现一个简单的检索增强生成（RAG）问答功能的雏形设计思路。', 1, 2, 'LangChain,Spring AI,RAG', 5, 0, 'PUBLISHED', NOW(), NOW()),
('使用 Docker 一键启动博客技术栈', '通过 Docker Compose 将 MySQL、Redis、Nginx 等组件整合在一起，提升环境搭建效率。', '对于经常需要在不同机器上部署博客的开发者，通过 Docker 化整个技术栈可以极大缩短环境准备时间，并保证环境一致性。', 1, 4, 'Docker,Docker Compose,运维', 3, 0, 'PUBLISHED', NOW(), NOW()),
('MySQL 索引原理与慢查询优化实战', '深入理解 B+ 树索引，并结合实际慢查询案例做优化。', '从索引数据结构、最左前缀、覆盖索引到执行计划解读，帮助你在生产环境中快速定位并优化慢 SQL。', 1, 3, 'MySQL,索引,性能优化', 15, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('Spring Cloud Gateway 网关路由与过滤器', '使用 Gateway 统一入口，实现路由、限流与鉴权。', '介绍 Spring Cloud Gateway 基本配置、Predicate、Filter 以及如何与 Nacos 集成做动态路由。', 1, 1, 'Spring Cloud,Gateway,微服务', 7, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('RAG 检索增强生成：从原理到落地', '梳理 RAG 流程中的分块、向量化与检索策略。', '讲解文档分块、Embedding 模型选型、向量库检索以及如何与 LLM 组合成完整 RAG 链路。', 1, 2, 'RAG,向量数据库,LangChain', 9, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('Linux 常用命令与 Shell 脚本入门', '日常运维必备的 Linux 命令与简单脚本编写。', '文件查找、日志分析、进程管理、定时任务与简单的 Shell 脚本示例，适合作为运维入门参考。', 1, 4, 'Linux,Shell,运维', 6, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
('MyBatis-Plus 条件构造器与分页插件', '熟练使用 Lambda 条件构造器与分页查询。', 'QueryWrapper、LambdaQueryWrapper、分页配置与逻辑删除，让 CRUD 开发更高效。', 1, 1, 'MyBatis-Plus,Java,后端', 11, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('Redis 持久化：RDB 与 AOF 选型', '理解 RDB 与 AOF 的优缺点及生产环境配置建议。', '介绍两种持久化机制的原理、配置项以及如何根据业务场景选择或组合使用。', 1, 3, 'Redis,持久化,中间件', 4, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
('Prompt 工程：让大模型更听话', '常用提示词技巧与 few-shot 示例设计。', '系统提示、角色设定、思维链与少样本示例，提升与大模型对话的可控性与效果。', 1, 2, 'Prompt,大模型,AI', 8, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
('Nginx 反向代理与负载均衡配置', '使用 Nginx 做前后端分离与多实例负载均衡。', 'upstream、proxy_pass、静态资源与 HTTPS 配置，适合博客与小型项目部署。', 1, 4, 'Nginx,负载均衡,运维', 5, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
('Java 并发：synchronized 与 Lock', '理解线程安全与常见锁机制。', 'synchronized、ReentrantLock、volatile 与常见并发场景下的正确用法与注意事项。', 1, 1, 'Java,并发,多线程', 10, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
('Elasticsearch 全文检索入门', '使用 ES 实现博客搜索与高亮。', '索引 mapping、match 查询、高亮与简单聚合，为博客增加搜索能力。', 1, 3, 'Elasticsearch,搜索,中间件', 7, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
('LangChain Agent 与 Tools 实践', '用 Agent 调用工具完成多步任务。', 'Agent 执行流程、Tool 定义与注册，实现一个简单的文档问答 Agent 示例。', 1, 2, 'LangChain,Agent,AI', 6, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
('RabbitMQ 消息确认与死信队列', '保证消息可靠投递与异常处理。', 'Publisher Confirm、Consumer ACK、死信队列与延迟队列的常见用法。', 1, 3, 'RabbitMQ,消息队列,中间件', 9, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
('Docker 多阶段构建与镜像优化', '减小镜像体积并加快构建速度。', '多阶段构建、精简基础镜像与 .dockerignore 使用，让镜像更小更安全。', 1, 4, 'Docker,镜像,运维', 4, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
('Spring AOP 切面编程实战', '用 AOP 实现日志、权限与性能监控。', '@Aspect、@Around、切点表达式与在 Spring Boot 中统一处理横切逻辑。', 1, 1, 'Spring,AOP,Java', 8, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
('Vue 3 组合式 API 与响应式原理', 'Composition API 与 ref、reactive 使用要点。', '从 Options API 到 Composition API，理解响应式原理与组合式开发实践。', 1, 1, 'Vue,前端,组合式API', 12, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
('向量数据库选型：Milvus 与 Chroma', '对比常见向量库在 RAG 场景下的适用性。', 'Milvus、Chroma、PGVector 等向量库的特点与在 LangChain 中的集成方式。', 1, 2, '向量数据库,RAG,AI', 5, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),
('MySQL 主从复制与读写分离', '搭建主从复制并配合 Sharding-JDBC 做读写分离。', 'binlog、主从延迟与读写分离配置，提升读多写少场景下的性能与可用性。', 1, 3, 'MySQL,主从,高可用', 6, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
('设计模式：工厂与策略模式', '常用设计模式在业务代码中的运用。', '简单工厂、工厂方法、抽象工厂与策略模式，减少 if-else 并提高可扩展性。', 1, 1, '设计模式,Java,架构', 7, 0, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY));

INSERT IGNORE INTO post_skill_relation (post_id, skill_id) VALUES
(1, 2),
(2, 4),
(3, 3),
(4, 6),
(5, 4),
(6, 2),
(7, 3),
(8, 6),
(9, 2),
(10, 4),
(13, 1),
(14, 4),
(15, 3),
(16, 5),
(18, 2),
(19, 7),
(20, 3),
(22, 9);

INSERT INTO hot_news (title, url, image_url, source, publish_date) VALUES
('OpenAI 发布新一代模型 GPT-X', 'https://openai.com/blog', 'https://picsum.photos/seed/gptx/800/420', 'OpenAI', NOW()),
('Rust 1.80 正式发布，改进宏与性能', 'https://blog.rust-lang.org', 'https://picsum.photos/seed/rust/800/420', 'Rust', NOW()),
('Kubernetes 1.32 推出新特性', 'https://kubernetes.io/blog/', 'https://picsum.photos/seed/k8s/800/420', 'CNCF', NOW()),
('Vite 6.0 Roadmap 公布', 'https://vitejs.dev/blog/', 'https://picsum.photos/seed/vite/800/420', 'Vite', NOW()),
('Spring Boot 3.3.5 发布', 'https://spring.io/blog', 'https://picsum.photos/seed/spring/800/420', 'Spring', NOW());

INSERT INTO comments (post_id, user_id, parent_id, content, created_at) VALUES
(1, 1, NULL, '这是博客搭建的开篇文章，欢迎大家评论交流！', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(5, 1, NULL, 'MySQL 索引优化是每个后端开发者的必修课。', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 1, 2, '补充一点：实际生产环境建议先用 EXPLAIN 分析执行计划，再决定建立哪些索引。', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 1, NULL, 'RAG 是目前最热门的 AI 应用方向之一，后续会持续更新实践案例。', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(9, 1, NULL, 'MyBatis-Plus 的 Lambda 条件构造器可以让代码更安全、更优雅。', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(12, 1, NULL, 'Nginx 不仅是反向代理，还能做静态资源服务器，非常实用。', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(15, 1, NULL, 'LangChain Agent 让 AI 有了"手脚"，可以完成更复杂的任务。', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(19, 1, NULL, 'Vue 3 的组合式 API 让逻辑复用更加灵活，强烈推荐升级。', DATE_SUB(NOW(), INTERVAL 15 DAY));

INSERT IGNORE INTO post_likes (post_id, user_id, created_at) VALUES
(1, 1, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(5, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(9, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(12, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(13, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(14, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(15, 1, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(16, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(18, 1, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(19, 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(20, 1, DATE_SUB(NOW(), INTERVAL 16 DAY));

INSERT INTO messages (recipient_id, sender_id, title, content, type, is_read, related_post_id, created_at) VALUES
(1, NULL, '系统欢迎通知', '欢迎加入 Karry Tech Blog！您的管理员账户已创建，默认密码为 admin123，请及时修改。', 'SYSTEM', 0, NULL, NOW()),
(1, NULL, '博客系统升级通知', '博客已升级至 v2.0 多用户版本，新增评论、点赞、消息通知与技能树功能，欢迎体验！', 'SYSTEM', 0, NULL, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, NULL, '文章发布成功', '文章《基于 Spring Boot 和 MyBatis-Plus 搭建个人博客后端》已成功发布。', 'SYSTEM', 1, 1, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(1, NULL, '文章发布成功', '文章《MySQL 索引原理与慢查询优化实战》已成功发布。', 'SYSTEM', 1, 5, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO theories (skill_id, content) VALUES
(1, '## Java 面向对象核心\n\n### 封装、继承与多态\n封装通过访问修饰符（private/protected/public）隐藏内部实现细节，仅暴露必要的接口。继承允许子类复用父类的属性和方法，Java 通过 `extends` 关键字实现单继承。\n\n多态分为编译时多态（方法重载）和运行时多态（方法重写），后者依赖动态绑定机制——JVM 在运行时根据对象的实际类型决定调用哪个方法。\n\n### 抽象类与接口\n抽象类可以包含非抽象方法和成员变量，而接口在 Java 8+ 中允许 `default` 和 `static` 方法。主要区别在于：一个类只能继承一个抽象类，但可以实现多个接口。\n\n### JVM 内存模型\nJVM 运行时数据区域包括：堆（存储对象实例）、方法区（存储类信息/常量/静态变量）、虚拟机栈（方法调用栈帧）、本地方法栈和程序计数器。\n\n### 垃圾回收\nJava 通过可达性分析判断对象是否可回收。常见 GC 算法：标记-清除、标记-复制、标记-整理。G1 和 ZGC 是当前主流的低延迟收集器。'),
(2, '## Spring 框架内核\n\n### IoC 容器与控制反转\nSpring IoC 容器通过依赖注入（DI）管理 Bean 的生命周期和依赖关系。核心接口是 `BeanFactory` 和 `ApplicationContext`，后者提供了事件发布、国际化等企业级功能。\n\n### Bean 生命周期\n实例化 → 属性赋值（依赖注入）→ `BeanNameAware`/`BeanFactoryAware` → `BeanPostProcessor.postProcessBeforeInitialization` → `@PostConstruct` → `InitializingBean.afterPropertiesSet` → 自定义 `init-method` → `BeanPostProcessor.postProcessAfterInitialization` → 使用中 → `@PreDestroy` → `DisposableBean.destroy` → 自定义 `destroy-method`。\n\n### AOP 原理\nSpring AOP 基于动态代理实现：JDK 动态代理（基于接口）和 CGLIB 代理（基于类）。核心概念包括 JoinPoint、Pointcut、Advice、Aspect。常用注解：`@Aspect`、`@Before`、`@After`、`@Around`、`@AfterReturning`、`@AfterThrowing`。\n\n### Spring Boot 自动配置\n通过 `@EnableAutoConfiguration` 和 `spring.factories` 机制，Spring Boot 根据 classpath 中的依赖自动配置 Bean。核心注解 `@SpringBootApplication` 包含 `@Configuration`、`@EnableAutoConfiguration`、`@ComponentScan`。'),
(3, '## LangChain 与 RAG 深入\n\n### RAG 核心流程\n1. **文档加载**：从多种数据源（PDF、网页、数据库）加载文档\n2. **文档分块（Chunking）**：按固定大小或语义边界分割文档，常用的 `RecursiveCharacterTextSplitter` 会递归按段落/句子/字符切割\n3. **向量化（Embedding）**：将文本块转为高维向量存储到向量数据库\n4. **检索**：用户查询向量化后，在向量库中通过余弦相似度等算法检索 Top-K 相关文档\n5. **生成增强**：将检索结果嵌入 Prompt 模板，交给 LLM 生成最终回答\n\n### LangChain 核心组件\n- **Model I/O**：与 LLM/ChatModel 交互的标准化接口\n- **Retrieval**：文档加载器、文本分割器、向量存储、检索器\n- **Chains**：将多个组件串联形成处理流水线\n- **Agents**：根据用户输入动态选择工具和执行步骤的智能体\n- **Memory**：对话上下文记忆管理\n\n### Spring AI\nSpring AI 提供了与 Spring 生态一致的 AI 接入方式：`ChatClient`（对话）、`EmbeddingClient`（向量化）、`VectorStore`（向量存储抽象），支持 OpenAI、Azure、Ollama 等多个模型供应商。'),
(4, '## MySQL 核心原理\n\n### InnoDB 存储引擎\nInnoDB 是 MySQL 默认存储引擎，支持事务（ACID）、行级锁、外键约束。数据以 B+ 树索引组织，主键索引的叶子节点存储完整行数据（聚簇索引），二级索引存储主键值。\n\n### B+ 树索引\nB+ 树所有数据存储在叶子节点，非叶子节点只存键值用于导航。特性：高度平衡、叶子节点通过双向链表连接支持范围查询。每次磁盘 I/O 读取一个节点，3-4 层的 B+ 树可索引千万级数据。\n\n### 最左匹配原则\n联合索引 `(a, b, c)` 在查询条件中按列顺序匹配：`WHERE a=1 AND b=2` 走索引；`WHERE b=2` 不走索引；`WHERE a>1 AND b=2` 只用到 a 列的范围扫描。\n\n### 慢查询优化\n1. 开启 `slow_query_log` 记录慢 SQL\n2. 使用 `EXPLAIN` 分析执行计划（关注 type/rows/Extra 列）\n3. 合理设计索引，避免 SELECT *\n4. 大表考虑垂直/水平拆分'),
(5, '## RabbitMQ 消息中间件\n\n### 核心概念\n- **Producer**：消息生产者，向 Exchange 发送消息\n- **Exchange**：交换机，根据路由规则将消息分发到队列\n- **Queue**：消息队列，存储待消费的消息\n- **Consumer**：消费者，从队列获取并处理消息\n- **Binding**：Exchange 与 Queue 的绑定关系\n\n### Exchange 类型\n1. **Direct**：精确匹配 Routing Key\n2. **Topic**：通配符匹配（`*` 匹配一个词，`#` 匹配零个或多个词）\n3. **Fanout**：广播到所有绑定的队列\n4. **Headers**：基于 Header 属性匹配（较少用）\n\n### 消息可靠性\n- **生产者确认**：Publisher Confirm 机制确保消息到达 Broker\n- **消费者确认**：手动 ACK 确保消息被正确处理\n- **持久化**：Exchange、Queue、Message 都设为 durable\n- **死信队列（DLX）**：处理消费失败/超时/队列满的消息\n\n### Redis 持久化\n- **RDB（快照）**：定期将内存数据快照保存到磁盘，恢复快但可能丢失最近数据\n- **AOF（追加文件）**：记录每次写操作，数据安全性高但文件更大\n- **混合持久化（4.0+）**：RDB 全量 + AOF 增量'),
(6, '## Linux 与容器化\n\n### 常用命令\n- **文件操作**：`ls`/`find`/`grep`/`awk`/`sed`/`tar`\n- **权限管理**：`chmod`/`chown`/`umask`\n- **进程管理**：`ps`/`top`/`htop`/`kill`/`nohup`\n- **网络**：`netstat`/`ss`/`curl`/`tcpdump`\n- **磁盘**：`df`/`du`/`iostat`\n\n### Docker 核心概念\n- **镜像（Image）**：分层存储的只读模板，通过 Dockerfile 构建\n- **容器（Container）**：镜像的可运行实例，拥有独立的文件系统和网络\n- **数据卷（Volume）**：持久化存储，生命周期独立于容器\n- **网络**：Bridge（默认）/Host/Overlay/Macvlan 模式\n\n### Docker Compose\n通过 YAML 文件定义多容器应用，一个命令启动整个技术栈：`docker-compose up -d`。支持环境变量、网络配置、健康检查和依赖管理。\n\n### 容器化最佳实践\n1. 使用多阶段构建减小镜像体积\n2. 以非 root 用户运行容器进程\n3. 合理使用 `.dockerignore` 排除无关文件\n4. 为每个容器设置 CPU/内存限制'),
(7, '## Vue 3 深度解析\n\n### 响应式系统\nVue 3 使用 ES6 `Proxy` 替代 Vue 2 的 `Object.defineProperty`，解决了新增属性监听和数组变更检测的问题。`ref()` 用于基本类型值的响应式包装（通过 `.value` 访问），`reactive()` 用于对象类型。\n\n### Composition API\n- **setup()** 函数是组合式 API 的入口，在 `beforeCreate` 和 `created` 之前执行\n- **生命周期钩子**：`onMounted`/`onUpdated`/`onUnmounted` 等\n- **`<script setup>`**：编译时语法糖，无需手动 `return`\n- **`watch`/`watchEffect`**：响应式数据监听与副作用执行\n\n### 虚拟 DOM 与 Diff\nVue 的虚拟 DOM 通过 `h()` 函数创建 VNode。Diff 算法采用双端比较，通过静态标记（PatchFlags）跳过静态节点的对比，显著提升性能。\n\n### Vite 构建工具\n基于浏览器原生 ES Module，开发时无需打包，借助 esbuild 做预构建。生产构建使用 Rollup，配合 Tree-shaking 和代码分割实现极致优化。HMR（热模块替换）极快，大型项目也能秒级更新。'),
(8, '## 测试体系构建\n\n### JUnit 5 架构\nJUnit 5 = JUnit Platform（运行基础） + JUnit Jupiter（新编程模型） + JUnit Vintage（兼容 JUnit 3/4）。核心注解：`@Test`/`@BeforeEach`/`@AfterEach`/`@BeforeAll`/`@AfterAll`/`@DisplayName`/`@ParameterizedTest`。\n\n### Mockito 核心\n- `@Mock`：创建模拟对象，方法调用返回默认值\n- `@InjectMocks`：自动注入模拟依赖\n- `when().thenReturn()`：设置方法行为\n- `verify()`：验证方法调用次数和参数\n- `@Spy`：部分模拟，保留真实行为\n\n### 测试覆盖率\n使用 JaCoCo 插件生成覆盖率报告，关注：行覆盖率、分支覆盖率、方法覆盖率。实践建议：核心业务逻辑 > 80%，工具类 100%，控制器层重点测试异常路径。\n\n### API 测试工具\n- **Postman**：图形化接口测试，支持环境变量、前置脚本、集合运行器\n- **Apifox**：API 文档、调试、Mock、测试一体化\n- **JMeter**：性能压测，配置线程组/HTTP 请求/监听器，支持分布式压测'),
(9, '## 分布式系统理论\n\n### CAP 定理\n在一个分布式系统中，一致性（Consistency）、可用性（Availability）、分区容错性（Partition Tolerance）三者不可同时满足。\n- **CP**：发生网络分区时牺牲可用性保证一致性（如 ZooKeeper）\n- **AP**：牺牲一致性保证可用性（如 Eureka）\n- 在实际系统中，通常会根据场景做取舍，如通过最终一致性（BASE）来平衡\n\n### 分布式锁\n1. **Redis 实现**：`SET NX PX` 原子命令 + Lua 脚本释放，Redisson 提供看门狗自动续期\n2. **ZooKeeper 实现**：临时顺序节点 + Watcher 机制，天然支持阻塞等待\n3. **数据库实现**：基于唯一索引，性能较差一般不推荐\n\n### 微服务治理\n- **服务注册与发现**：Nacos/Consul/Eureka\n- **配置中心**：Nacos/Apollo，支持动态刷新\n- **网关**：Spring Cloud Gateway/Zuul，统一鉴权和限流\n- **熔断降级**：Sentinel/Resilience4j，防止级联故障\n- **分布式事务**：Seata（AT/TCC/Saga 模式）'),
(10, '## Git 协作与 CI/CD\n\n### Git 分支策略\n- **Git Flow**：main（生产）+ develop（开发）+ feature/release/hotfix 分支，适合有固定发布周期的项目\n- **GitHub Flow**：main + feature 分支，通过 PR 合并，适合持续部署\n- **Trunk-Based**：所有开发者直接在主干提交，通过 Feature Flag 控制功能开关\n\n### 常用 Git 操作\n`git rebase` 将分支变基到目标分支，保持提交历史整洁线性；`git merge` 保留分支历史但产生合并提交。`git stash` 暂存未提交修改。`git cherry-pick` 选择特定提交应用到当前分支。\n\n### CI/CD 流水线\n1. **CI（持续集成）**：代码提交后自动触发构建、单元测试、代码扫描\n2. **CD（持续交付/部署）**：通过测试后自动部署到预发布/生产环境\n3. 工具链：GitHub Actions / GitLab CI / Jenkins + Docker + Kubernetes\n\n### Code Review 最佳实践\n关注逻辑正确性、代码可读性、安全漏洞、性能问题。建议 PR 粒度控制在 200 行以内。使用 Conventional Commits 规范提交信息（如 `feat:`/`fix:`/`refactor:`）。');

SET FOREIGN_KEY_CHECKS = 1;
