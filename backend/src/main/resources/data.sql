INSERT INTO hot_news (title, url, image_url, source, publish_date) VALUES
('OpenAI 发布新一代模型 GPT-X', 'https://openai.com/blog', 'https://picsum.photos/seed/gptx/800/420', 'OpenAI', NOW()),
('Rust 1.80 正式发布，改进宏与性能', 'https://blog.rust-lang.org', 'https://picsum.photos/seed/rust/800/420', 'Rust', NOW()),
('Kubernetes 1.32 推出新特性', 'https://kubernetes.io/blog/', 'https://picsum.photos/seed/k8s/800/420', 'CNCF', NOW()),
('Vite 6.0 Roadmap 公布', 'https://vitejs.dev/blog/', 'https://picsum.photos/seed/vite/800/420', 'Vite', NOW()),
('Spring Boot 3.3.5 发布', 'https://spring.io/blog', 'https://picsum.photos/seed/spring/800/420', 'Spring', NOW());

INSERT INTO skills (category, title, description, pinned) VALUES
('编程语言与基础能力', 'Java 及面向对象基础', '熟练掌握 Java 语言，深入理解面向对象编程、异常处理、集合框架、并发编程；熟悉常见数据结构及算法；了解常用设计模式，如工厂、责任链、策略等。', 1),
('主流开发框架', 'Spring 生态与微服务', '熟练使用 Spring Boot，熟悉 Spring Cloud 及相关组件如 Nacos、OpenFeign、Gateway 等，实现服务注册、调用与网关路由；熟练使用 MyBatis/MyBatis-Plus 实现 ORM 映射，熟悉 Lambda 查询、分页插件、逻辑删除等高效特性；理解 AOP 面向切面编程思想，能基于注解实现日志记录、权限校验、性能埋点等横切关注点。', 1),
('AI 与智能体开发', 'LangChain 与 RAG', '初步掌握 LangChain 框架，了解核心组件如 Chains、Agents、Tools、Memory 的基本用法；了解检索增强生成（RAG），熟悉 Spring AI 基础用法，熟悉对接大模型调用、Prompt 模板管理及函数调用的封装；熟悉提示词工程，包括指令提示、上下文注入、少样本示例等常用技巧；熟悉 Agent 常见应用场景，如文档问答机器人、知识库对话与上下文管理等。', 0),
('数据库与存储技术', 'MySQL/SQL Server/Oracle', '熟悉 MySQL、SQL Server、Oracle 等关系型数据库，具备 SQL 调优经验，包括执行计划分析、索引优化、慢查询处理、主从复制部署；了解分布式表方案；熟悉 Redis 缓存技术，包括常用数据结构、缓存穿透/击穿/雪崩防护，主从+哨兵架构、分片集群等；熟悉 Elasticsearch，实现全文检索、高亮显示及简易聚合分析等。', 0),
('中间件与消息系统', 'RabbitMQ/ActiveMQ/Redis 消息', '熟悉 RabbitMQ、ActiveMQ，如 Exchange 类型、死信队列、Confirm 机制，用于系统解耦与异步处理；熟练部署与配置 Redis，支持集群模式与持久化策略；熟练使用 Postman、Apifox 等进行接口调试、自动化测试与文档协作。', 0),
('系统与运维能力', 'Linux 运维与容器化', '熟悉 Linux/Unix 常用命令，能够独立完成服务部署，已对排查线上性能波动；熟练使用 Docker 构建镜像、编排容器，完成 MySQL、Redis、Nginx 等中间件的本地化部署。', 0),
('前端与工程化', 'Vue3 / Vite', '熟练使用 Vue 3 组合式 API、Pinia 状态管理、Vue Router；熟悉 Vite 构建与前端工程化；了解 TypeScript 与常见 UI 组件库。', 0),
('测试与质量保障', '单元测试与接口测试', '熟练使用 JUnit 5、Mockito 编写单元测试；熟悉 Postman、Apifox 做接口测试与自动化；了解 JMeter 做简单压测。', 0),
('架构与设计', '分布式与高可用', '了解微服务拆分、服务治理与分布式事务；熟悉常见高可用方案如限流、熔断、降级；具备一定的系统设计能力。', 0),
('开发工具链', 'Git / IDEA / 协作', '熟练使用 Git 分支策略、Code Review 与 CI/CD 流水线；熟练使用 IntelliJ IDEA 等开发工具提升效率。', 0);

INSERT INTO categories (name, description) VALUES
('后端开发', '所有与 Java、Spring 生态和服务端相关的技术文章'),
('AI 与大模型', '与大语言模型、智能体、RAG 等相关的文章'),
('数据库与中间件', '围绕数据库、Redis、消息队列等的技术分享'),
('系统与运维', 'Linux、Docker、运维与部署相关经验'),
('前端开发', 'Vue、前端工程化与交互相关文章'),
('架构与软技能', '设计模式、架构思路与团队协作');

INSERT INTO posts (title, summary, content, category_id, tags, view_count, pinned, created_at, updated_at) VALUES
('基于 Spring Boot 和 MyBatis-Plus 搭建个人博客后端', '从零搭建一个支持分页、分类的博客后端服务，实现常见 CRUD 能力。', '本文将介绍如何使用 Spring Boot 3.x + MyBatis-Plus 搭建一个简洁易扩展的博客后端，包括实体设计、分页查询、统一返回结果以及基础的异常处理思路。', 1, 'Spring Boot,MyBatis-Plus,后端实战', 12, 1, NOW(), NOW()),
('使用 Redis 为博客系统加速：缓存常见实践', '讲解如何在个人博客中使用 Redis 做缓存，提高首页和文章详情的响应速度。', '在真实场景中，博客首页文章列表和热门技能信息访问频率很高，我们可以借助 Redis 将这些数据缓存起来，并设计合理的失效策略以避免缓存雪崩。', 3, 'Redis,缓存,性能优化', 8, 0, NOW(), NOW()),
('初探 LangChain 与 Spring AI：让博客具备问答能力', '将 AI 能力集成到博客中，让读者可以就文章内容进行自然语言问答。', '本文会演示如何使用 LangChain 与 Spring AI 封装大模型调用，将博客文章内容索引到向量库中，实现一个简单的检索增强生成（RAG）问答功能的雏形设计思路。', 2, 'LangChain,Spring AI,RAG', 5, 0, NOW(), NOW()),
('使用 Docker 一键启动博客技术栈', '通过 Docker Compose 将 MySQL、Redis、Nginx 等组件整合在一起，提升环境搭建效率。', '对于经常需要在不同机器上部署博客的开发者，通过 Docker 化整个技术栈可以极大缩短环境准备时间，并保证环境一致性。', 4, 'Docker,Docker Compose,运维', 3, 0, NOW(), NOW()),
('MySQL 索引原理与慢查询优化实战', '深入理解 B+ 树索引，并结合实际慢查询案例做优化。', '从索引数据结构、最左前缀、覆盖索引到执行计划解读，帮助你在生产环境中快速定位并优化慢 SQL。', 3, 'MySQL,索引,性能优化', 15, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('Spring Cloud Gateway 网关路由与过滤器', '使用 Gateway 统一入口，实现路由、限流与鉴权。', '介绍 Spring Cloud Gateway 基本配置、Predicate、Filter 以及如何与 Nacos 集成做动态路由。', 1, 'Spring Cloud,Gateway,微服务', 7, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('RAG 检索增强生成：从原理到落地', '梳理 RAG 流程中的分块、向量化与检索策略。', '讲解文档分块、Embedding 模型选型、向量库检索以及如何与 LLM 组合成完整 RAG 链路。', 2, 'RAG,向量数据库,LangChain', 9, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('Linux 常用命令与 Shell 脚本入门', '日常运维必备的 Linux 命令与简单脚本编写。', '文件查找、日志分析、进程管理、定时任务与简单的 Shell 脚本示例，适合作为运维入门参考。', 4, 'Linux,Shell,运维', 6, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
('MyBatis-Plus 条件构造器与分页插件', '熟练使用 Lambda 条件构造器与分页查询。', 'QueryWrapper、LambdaQueryWrapper、分页配置与逻辑删除，让 CRUD 开发更高效。', 1, 'MyBatis-Plus,Java,后端', 11, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('Redis 持久化：RDB 与 AOF 选型', '理解 RDB 与 AOF 的优缺点及生产环境配置建议。', '介绍两种持久化机制的原理、配置项以及如何根据业务场景选择或组合使用。', 3, 'Redis,持久化,中间件', 4, 0, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
('Prompt 工程：让大模型更听话', '常用提示词技巧与 few-shot 示例设计。', '系统提示、角色设定、思维链与少样本示例，提升与大模型对话的可控性与效果。', 2, 'Prompt,大模型,AI', 8, 0, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
('Nginx 反向代理与负载均衡配置', '使用 Nginx 做前后端分离与多实例负载均衡。', 'upstream、proxy_pass、静态资源与 HTTPS 配置，适合博客与小型项目部署。', 4, 'Nginx,负载均衡,运维', 5, 0, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
('Java 并发：synchronized 与 Lock', '理解线程安全与常见锁机制。', 'synchronized、ReentrantLock、volatile 与常见并发场景下的正确用法与注意事项。', 1, 'Java,并发,多线程', 10, 0, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
('Elasticsearch 全文检索入门', '使用 ES 实现博客搜索与高亮。', '索引 mapping、match 查询、高亮与简单聚合，为博客增加搜索能力。', 3, 'Elasticsearch,搜索,中间件', 7, 0, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
('LangChain Agent 与 Tools 实践', '用 Agent 调用工具完成多步任务。', 'Agent 执行流程、Tool 定义与注册，实现一个简单的文档问答 Agent 示例。', 2, 'LangChain,Agent,AI', 6, 0, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
('RabbitMQ 消息确认与死信队列', '保证消息可靠投递与异常处理。', 'Publisher Confirm、Consumer ACK、死信队列与延迟队列的常见用法。', 3, 'RabbitMQ,消息队列,中间件', 9, 0, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
('Docker 多阶段构建与镜像优化', '减小镜像体积并加快构建速度。', '多阶段构建、精简基础镜像与 .dockerignore 使用，让镜像更小更安全。', 4, 'Docker,镜像,运维', 4, 0, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
('Spring AOP 切面编程实战', '用 AOP 实现日志、权限与性能监控。', '@Aspect、@Around、切点表达式与在 Spring Boot 中统一处理横切逻辑。', 1, 'Spring,AOP,Java', 8, 0, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
('Vue 3 组合式 API 与响应式原理', 'Composition API 与 ref、reactive 使用要点。', '从 Options API 到 Composition API，理解响应式原理与组合式开发实践。', 1, 'Vue,前端,组合式API', 12, 0, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
('向量数据库选型：Milvus 与 Chroma', '对比常见向量库在 RAG 场景下的适用性。', 'Milvus、Chroma、PGVector 等向量库的特点与在 LangChain 中的集成方式。', 2, '向量数据库,RAG,AI', 5, 0, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),
('MySQL 主从复制与读写分离', '搭建主从复制并配合 Sharding-JDBC 做读写分离。', 'binlog、主从延迟与读写分离配置，提升读多写少场景下的性能与可用性。', 3, 'MySQL,主从,高可用', 6, 0, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
('K8s 入门：Pod 与 Deployment', '在 Kubernetes 中部署无状态应用。', 'Pod、Deployment、Service 基本概念与 YAML 配置，为博客容器化部署做准备。', 4, 'Kubernetes,容器,运维', 3, 0, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
('设计模式：工厂与策略模式', '常用设计模式在业务代码中的运用。', '简单工厂、工厂方法、抽象工厂与策略模式，减少 if-else 并提高可扩展性。', 1, '设计模式,Java,架构', 7, 0, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),
('Spring AI 接入 OpenAI 与国产大模型', '统一接口调用多模型。', 'Spring AI 的 ChatClient、Embedding 与国产模型适配，实现可切换的 LLM 调用层。', 2, 'Spring AI,大模型,AI', 11, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY));
