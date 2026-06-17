# AGENTS.md

## 项目目标

本项目要实现一个企业级知识库智能体系统，面向几百万级 chunk 的企业文档知识库，支持文档入库、解析、清洗、脱敏、切片、Embedding、混合检索、Rerank、权限过滤、引用溯源、智能体问答和评估。

第一阶段目标是 MVP，不追求一次性完成所有能力，但架构必须可扩展到百万级、千万级 chunk。

## 技术栈约定

后端主服务：
- Java Spring Boot 3
- Spring Security
- MyBatis Plus 或 JPA
- MySQL

AI 与文档处理服务：
- Python FastAPI
- 文档解析、切片、Embedding、Rerank、模型调用放在 Python 服务中

检索与存储：
- PostgreSQL / MySQL：业务元数据、权限、任务状态
- Elasticsearch / OpenSearch：关键词检索、元数据检索
- Milvus：向量检索
- Redis：缓存
- OSS / MinIO：原始文档存储
- Kafka / RocketMQ：异步任务队列

前端：
- Vue 3
- TypeScript
- Element Plus
- Pinia
- Axios

## 核心模块

1. 用户与权限模块
2. 知识库管理模块
3. 文档管理模块
4. 文档解析任务模块
5. 脱敏与清洗模块
6. 切片模块
7. Embedding 模块
8. 索引模块
9. 混合检索模块
10. Rerank 模块
11. Agent 问答模块
12. 引用溯源模块
13. 评估模块
14. 审计日志模块

## 开发原则

- 不要一次性实现所有功能。
- 每次任务先阅读项目结构，再制定简短实现计划。
- 优先实现可运行的最小闭环。
- 所有接口要有清晰 DTO、错误码和日志。
- 所有重要模块都要有单元测试或集成测试。
- 新增依赖前先说明用途。
- 不要硬编码模型 Key、数据库密码、OSS 密钥。
- 所有配置放到 application.yml、.env 或配置中心。
- 检索结果必须支持 source_doc_id、page_no、chunk_id、section_title 等引用字段。
- 权限过滤必须发生在检索前，不允许先全库检索再让大模型判断。
- 所有生成型回答必须保留引用来源。
- 不要生成与当前任务无关的大量代码。

## 输出要求

每次完成任务后，请输出：
1. 本次修改了哪些文件
2. 实现了哪些功能
3. 如何运行
4. 如何测试
5. 还有哪些待办