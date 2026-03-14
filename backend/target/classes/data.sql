INSERT INTO skills (category, title, description) VALUES
('编程语言与基础能力', 'Java 及面向对象基础', '熟练掌握 Java 语言，深入理解面向对象编程、异常处理、集合框架、并发编程；熟悉常见数据结构及算法；了解常用设计模式，如工厂、责任链、策略等。'),
('主流开发框架', 'Spring 生态与微服务', '熟练使用 Spring Boot，熟悉 Spring Cloud 及相关组件如 Nacos、OpenFeign、Gateway 等，实现服务注册、调用与网关路由；熟练使用 MyBatis/MyBatis-Plus 实现 ORM 映射，熟悉 Lambda 查询、分页插件、逻辑删除等高效特性；理解 AOP 面向切面编程思想，能基于注解实现日志记录、权限校验、性能埋点等横切关注点。'),
('AI 与智能体开发', 'LangChain 与 RAG', '初步掌握 LangChain 框架，了解核心组件如 Chains、Agents、Tools、Memory 的基本用法；了解检索增强生成（RAG），熟悉 Spring AI 基础用法，熟悉对接大模型调用、Prompt 模板管理及函数调用的封装；熟悉提示词工程，包括指令提示、上下文注入、少样本示例等常用技巧；熟悉 Agent 常见应用场景，如文档问答机器人、知识库对话与上下文管理等。'),
('数据库与存储技术', 'MySQL/SQL Server/Oracle', '熟悉 MySQL、SQL Server、Oracle 等关系型数据库，具备 SQL 调优经验，包括执行计划分析、索引优化、慢查询处理、主从复制部署；了解分布式表方案；熟悉 Redis 缓存技术，包括常用数据结构、缓存穿透/击穿/雪崩防护，主从+哨兵架构、分片集群等；熟悉 Elasticsearch，实现全文检索、高亮显示及简易聚合分析等。'),
('中间件与消息系统', 'RabbitMQ/ActiveMQ/Redis 消息', '熟悉 RabbitMQ、ActiveMQ，如 Exchange 类型、死信队列、Confirm 机制，用于系统解耦与异步处理；熟练部署与配置 Redis，支持集群模式与持久化策略；熟练使用 Postman、Apifox 等进行接口调试、自动化测试与文档协作。'),
('系统与运维能力', 'Linux 运维与容器化', '熟悉 Linux/Unix 常用命令，能够独立完成服务部署，已对排查线上性能波动；熟练使用 Docker 构建镜像、编排容器，完成 MySQL、Redis、Nginx 等中间件的本地化部署。');

INSERT INTO categories (name, description) VALUES
('后端开发', '所有与 Java、Spring 生态和服务端相关的技术文章'),
('AI 与大模型', '与大语言模型、智能体、RAG 等相关的文章'),
('数据库与中间件', '围绕数据库、Redis、消息队列等的技术分享'),
('系统与运维', 'Linux、Docker、运维与部署相关经验');

INSERT INTO posts (title, summary, content, category_id, tags, view_count, created_at, updated_at) VALUES
('基于 Spring Boot 和 MyBatis-Plus 搭建个人博客后端', '从零搭建一个支持分页、分类的博客后端服务，实现常见 CRUD 能力。', '本文将介绍如何使用 Spring Boot 3.x + MyBatis-Plus 搭建一个简洁易扩展的博客后端，包括实体设计、分页查询、统一返回结果以及基础的异常处理思路。', 1, 'Spring Boot,MyBatis-Plus,后端实战', 12, NOW(), NOW()),
('使用 Redis 为博客系统加速：缓存常见实践', '讲解如何在个人博客中使用 Redis 做缓存，提高首页和文章详情的响应速度。', '在真实场景中，博客首页文章列表和热门技能信息访问频率很高，我们可以借助 Redis 将这些数据缓存起来，并设计合理的失效策略以避免缓存雪崩。', 3, 'Redis,缓存,性能优化', 8, NOW(), NOW()),
('初探 LangChain 与 Spring AI：让博客具备问答能力', '将 AI 能力集成到博客中，让读者可以就文章内容进行自然语言问答。', '本文会演示如何使用 LangChain 与 Spring AI 封装大模型调用，将博客文章内容索引到向量库中，实现一个简单的检索增强生成（RAG）问答功能的雏形设计思路。', 2, 'LangChain,Spring AI,RAG', 5, NOW(), NOW()),
('使用 Docker 一键启动博客技术栈', '通过 Docker Compose 将 MySQL、Redis、Nginx 等组件整合在一起，提升环境搭建效率。', '对于经常需要在不同机器上部署博客的开发者，通过 Docker 化整个技术栈可以极大缩短环境准备时间，并保证环境一致性。', 4, 'Docker,Docker Compose,运维', 3, NOW(), NOW());

