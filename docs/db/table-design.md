# 企业知识库元数据库表设计 V1

本版本面向第一阶段 MVP：文档上传、解析、切片、Embedding、检索、问答和引用溯源闭环。表结构保留 RBAC、ABAC、审计和评估扩展点，但不在本阶段实现复杂策略引擎。

## 设计约定

- 所有业务表统一包含 `id`、`created_at`、`updated_at`、`created_by`、`updated_by`。
- `id` 使用应用侧生成的 `BIGINT`，后续可接入雪花 ID、号段服务或分布式 ID 服务。
- 当前 migration 使用逻辑关联和索引，不强制物理外键，优先保障大规模文档入库、切片写入和任务重试吞吐。
- `metadata_json`、`condition_json`、`detail_json` 先用 `TEXT`，保持 PostgreSQL / MySQL / H2 测试兼容；生产可在数据库方言稳定后升级为 JSON/JSONB。
- 权限过滤入口由 `kb_permission_policy` 表承载，后续检索前必须先解析可访问的 space/doc/chunk 范围，再调用 OpenSearch/Milvus。

## 表用途

| 表名 | 用途 |
| --- | --- |
| `kb_space` | 知识库空间，承载租户下的知识库分组、可见性、负责人和状态。 |
| `kb_document` | 文档主表，保存文档业务属性、分类标签、密级、来源 URI、文件哈希、当前版本和生命周期状态。 |
| `kb_document_version` | 文档版本表，记录同一文档的不同上传版本、解析状态、切片数量和 token 统计。 |
| `kb_document_chunk` | 文档切片表，保存检索最小文本单元，并保留 `doc_id`、`version_id`、`page_no`、`section_title` 等引用溯源字段。 |
| `kb_document_parse_task` | 文档解析任务表，记录解析、清洗、切片等异步任务的状态、进度、重试和错误信息。 |
| `kb_embedding_index_task` | Embedding 索引任务表，记录 chunk 向量化与写入向量库/索引集合的进度、模型和错误信息。 |
| `kb_permission_policy` | 权限策略表，使用 subject/resource/action/effect 支持 RBAC，使用 `condition_json` 预留 ABAC 条件。 |
| `kb_query_session` | 问答会话表，记录用户在某个知识库空间下的多轮问答上下文。 |
| `kb_query_message` | 问答消息表，记录 user/assistant/system 消息、模型、token、耗时和状态。 |
| `kb_answer_citation` | 回答引用表，记录回答消息引用的文档、版本、chunk、页码、章节、摘录、分数和排序。 |
| `kb_audit_log` | 审计日志表，记录用户行为、资源、请求、结果、trace 和详情，支撑合规追踪。 |
| `kb_eval_dataset` | 评估数据集表，组织一批用于检索/问答质量评估的问题集合。 |
| `kb_eval_case` | 评估问题表，保存单个评估问题、参考答案、期望命中文档和标签。 |
| `kb_eval_result` | 评估结果表，记录某次评估运行的答案、得分、引用命中和评估器信息。 |

## 扩展方向

- 百万级 chunk：按 `tenant_id`、`space_id`、`doc_id`、`version_id` 做冷热分层和分区规划。
- 千万级 chunk：引入分片键、归档表、异步批写、检索索引别名和向量集合滚动重建。
- 权限：`subject_type` 可表示 `USER`、`ROLE`、`GROUP`、`DEPARTMENT`；`condition_json` 可保存行业、密级、地区、时间窗等 ABAC 条件。
- 评估：`kb_eval_result.detail_json` 可保存命中文档、LLM judge 细节、召回率、MRR、faithfulness 等指标。
