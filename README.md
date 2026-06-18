# 企业知识库智能体工程骨架

本仓库采用前后端分离 + Python AI 服务的 MVP 工程结构：

- `backend/`：Spring Boot 3 REST API，预留用户权限、知识库、文档、检索、问答、任务、审计模块。
- `ai-service/`：FastAPI AI 服务，预留解析、切片、Embedding、Rerank、LLM 调用接口，默认 mock，可按配置切换 OpenAI-compatible provider。
- `web/`：Vue 3 + TypeScript + Element Plus 前端控制台。
- `deploy/`：本地开发依赖编排，包含 PostgreSQL、MySQL、Redis、MinIO、OpenSearch、Milvus。

## 启动后端

后端已启用 JPA + Flyway，启动时需要可用的元数据库。开发环境可以先启动 PostgreSQL、Redis、MinIO：

```bash
cp deploy/.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d postgres redis minio
```

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

本地启动后端时，Spring Boot 会自动尝试读取 `deploy/.env` 或 `../deploy/.env`。如果你从 IDE 启动，仍然可以直接配置环境变量覆盖：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/knowledge
SPRING_DATASOURCE_USERNAME=knowledge_app
SPRING_DATASOURCE_PASSWORD=change-me-local-postgres
```

健康检查：

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

Redis、MinIO、OpenSearch、Milvus 地址通过 `backend/src/main/resources/application.yml` 和 `application-dev.yml` 的环境变量配置。默认本地开发仍使用数据库/mock 检索；设置 `SEARCH_ENGINE=opensearch`、`VECTOR_STORE_ENGINE=milvus` 后会启用真实索引与检索客户端。

数据库迁移使用 Flyway，默认位置：

```text
backend/src/main/resources/db/migration
```

当前第一版核心表结构见：

- `backend/src/main/resources/db/migration/V1__init_knowledge_schema.sql`
- `docs/db/table-design.md`

文档上传当前使用本地存储实现，默认目录为 `backend/data/uploads` 或启动命令所在目录下的 `data/uploads`。可通过环境变量覆盖：

```bash
export KNOWLEDGE_STORAGE_LOCAL_ROOT=/path/to/uploads
```

文档切片默认按约 800 个中文字符切分，并保留 100 个字符 overlap，可通过环境变量覆盖：

```bash
export KNOWLEDGE_CHUNK_SIZE=800
export KNOWLEDGE_CHUNK_OVERLAP=100
```

### 文档入库 API

创建知识库：

```bash
curl -X POST http://localhost:8080/api/v1/kb-spaces \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":1001,"name":"Delivery Docs"}'
```

查询知识库列表：

```bash
curl 'http://localhost:8080/api/v1/kb-spaces?tenantId=1001'
```

上传文档：

```bash
curl -X POST http://localhost:8080/api/v1/kb-spaces/{spaceId}/documents \
  -F 'file=@/path/to/document.pdf' \
  -F 'title=Document Title' \
  -F 'industry=Education' \
  -F 'serviceLine=Consulting' \
  -F 'confidentialLevel=INTERNAL'
```

查询文档列表、详情、删除和解析状态：

```bash
curl http://localhost:8080/api/v1/kb-spaces/{spaceId}/documents
curl http://localhost:8080/api/v1/documents/{documentId}
curl -X DELETE http://localhost:8080/api/v1/documents/{documentId}
curl http://localhost:8080/api/v1/documents/{documentId}/parse-status
```

当前支持文件扩展名：`pdf`、`doc`、`docx`、`ppt`、`pptx`、`xls`、`xlsx`、`md`、`markdown`、`txt`。

### 文档解析任务执行

上传文档会创建 `kb_document_parse_task`，状态为 `PENDING`。后端现在可以调用 Python AI 服务解析本地文件，并将解析结果继续交给切片、脱敏和 chunk 入库流程。切片完成后会为每个 chunk 创建 `kb_embedding_index_task`，供 Embedding / 关键词索引 / 向量索引任务消费。

默认不会自动轮询执行解析任务，避免本地只启动后端时反复调用未启动的 AI 服务。可以手动运行：

```bash
curl -X POST http://localhost:8080/api/v1/tasks/{taskId}/run \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

查询某个知识库的解析任务：

```bash
curl 'http://localhost:8080/api/v1/tasks/parse?spaceId={spaceId}&status=PENDING&limit=50' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

失败任务可以重试，重试会清理错误信息并把状态重置为 `PENDING`：

```bash
curl -X POST http://localhost:8080/api/v1/tasks/{taskId}/retry \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

如需让后端定时消费 `PENDING` 任务，可开启：

```bash
export PARSE_TASK_EXECUTOR_AUTO_RUN=true
export PARSE_TASK_EXECUTOR_FIXED_DELAY=10s
export PARSE_TASK_WORKER_ID=backend-local
```

### Embedding 与索引任务执行

`kb_embedding_index_task` 会通过 `EmbeddingProvider` 生成向量，并通过 `KeywordSearchClient`、`VectorStoreClient` 写入索引。默认配置使用本地 mock/database，适合离线开发；开启真实链路后：

- `EMBEDDING_PROVIDER=ai-service`：后端调用 FastAPI `/api/v1/embeddings`。
- `SEARCH_ENGINE=opensearch`：任务执行时写 OpenSearch，检索时知识库 owner 走 OpenSearch，非 owner 回退数据库权限检索。
- `VECTOR_STORE_ENGINE=milvus`：任务执行时通过 Milvus REST 写入向量，检索时知识库 owner 走 Milvus，非 owner 回退 mock 权限检索。

Embedding mock 统一为 `mock-embedding-v1`、16 维，避免 Java/Python 两边维度漂移。

查询某个知识库的 Embedding 索引任务：

```bash
curl 'http://localhost:8080/api/v1/tasks/embedding?spaceId={spaceId}&status=PENDING&limit=50' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

手动执行单个任务：

```bash
curl -X POST http://localhost:8080/api/v1/tasks/embedding/{taskId}/run \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

失败任务可以重试：

```bash
curl -X POST http://localhost:8080/api/v1/tasks/embedding/{taskId}/retry \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

如需让后端定时消费 Embedding 任务，可开启：

```bash
export EMBEDDING_TASK_EXECUTOR_AUTO_RUN=true
export EMBEDDING_TASK_EXECUTOR_FIXED_DELAY=10s
```

启用真实检索索引链路的关键环境变量：

```bash
export EMBEDDING_PROVIDER=ai-service
export AI_SERVICE_EMBEDDING_PATH=/api/v1/embeddings
export AI_SERVICE_EMBEDDING_MODEL=mock-embedding-v1
export EMBEDDING_DIMENSION=16

export SEARCH_ENGINE=opensearch
export OPENSEARCH_ENDPOINT=http://localhost:9200
export SEARCH_INDEX_PREFIX=knowledge

export VECTOR_STORE_ENGINE=milvus
export MILVUS_ENDPOINT=http://localhost:19530
export MILVUS_TOKEN=root:Milvus
export VECTOR_COLLECTION_PREFIX=knowledge
```

AI 服务配置：

```bash
export AI_SERVICE_ENDPOINT=http://localhost:8001
export AI_SERVICE_PARSE_PATH=/api/parse/document
export AI_SERVICE_EMBEDDING_PATH=/api/v1/embeddings
export AI_SERVICE_TIMEOUT=60s
```

AI 服务的 Embedding / Rerank / LLM 默认都是 `mock`，可按环境变量切到真实服务：

```bash
# OpenAI-compatible / Qwen / 私有兼容服务
export EMBEDDING_PROVIDER=openai-compatible
export EMBEDDING_MODEL_NAME=text-embedding-v3
export EMBEDDING_DIMENSION=1024
export AI_EMBEDDING_ENDPOINT=https://api.example.com/v1
export EMBEDDING_API_KEY=change-me

export LLM_PROVIDER=openai-compatible
export LLM_MODEL_NAME=qwen-plus
export AI_LLM_ENDPOINT=https://api.example.com/v1
export LLM_API_KEY=change-me
export LLM_TEMPERATURE=0
export LLM_MAX_TOKENS=2048

# Rerank 使用私有 HTTP 服务时，endpoint 是完整 rerank 地址
export RERANK_PROVIDER=private-http
export RERANK_MODEL_NAME=bge-reranker
export AI_RERANK_ENDPOINT=http://localhost:8002/api/v1/rerank
export RERANK_API_KEY=change-me
```

协议约定：

- Embedding 调用 OpenAI-compatible `POST {AI_EMBEDDING_ENDPOINT}/embeddings`，请求字段为 `model`、`input`、可选 `dimensions`。
- LLM 调用 OpenAI-compatible `POST {AI_LLM_ENDPOINT}/chat/completions`，请求字段为 `model`、`messages`、`temperature`、`max_completion_tokens`。
- Rerank 私有 HTTP 调用 `AI_RERANK_ENDPOINT`，请求字段为 `query`、`documents`、`model`、`top_k`，返回可使用 `documents` 或 `results`。

### 文档切片 API

后端当前提供基础切片与入库能力，接收 Python 解析服务返回的 `pages` 结构后写入 `kb_document_chunk`，并更新文档版本的 `chunk_count`、`total_tokens`、`parse_status` 以及最新解析任务状态。重新切片会删除当前版本旧 chunk 后按 `chunk_index` 从 0 重建。

```bash
curl -X POST http://localhost:8080/api/v1/documents/{documentId}/chunks/rebuild \
  -H 'Content-Type: application/json' \
  -d '{
    "chunkSize": 800,
    "overlap": 100,
    "pages": [
      {
        "pageNo": 1,
        "sectionTitle": "项目背景",
        "contentType": "text",
        "content": "# 项目背景\n这里是解析后的正文...",
        "metadata": {"parser": "fastapi-mvp"}
      }
    ]
  }'
```

MVP 切片策略：

- 普通文档优先识别 Markdown 标题、中文编号标题、数字编号标题和 SOW 常见标题，再按字符长度继续切分。
- PPT 和 Excel 预留按页、sheet 或表格区域切分；当前会优先保持每个解析页为独立来源段，超长时继续按长度切。
- `metadata_json` 会保留 `content_type`、`page_no`、`section_title`、`chunk_size`、`overlap`、分片序号和原始解析 metadata，供后续检索引用溯源使用。

### 文档脱敏

文档脱敏发生在“解析结果 -> 切片”之间。`DocumentChunkingService` 会先调用 `Desensitizer`，当前实现为 `RegexDesensitizer`，再将脱敏后的内容写入 `kb_document_chunk`。原始文件仍保留在文件系统或对象存储中，访问必须走后端权限校验。

MVP 支持：

- 手机号：`13812348888` -> `138****8888`
- 邮箱：`alice@company.com` -> `a***@company.com`
- 身份证号：替换为 `身份证号：已脱敏`
- 金额：按区间替换，例如 `250万元` -> `金额区间：100万-300万`
- 客户联系人：`客户联系人：张三` -> `客户联系人：客户联系人A`
- 客户名称：通过 `knowledge.desensitization.customer-names` 词典匹配，例如 `平安银行` -> `某金融客户`

脱敏前后映射会保存到 `kb_desensitization_mapping`，并且只允许具备 `admin_manage` 权限的用户查看：

```bash
curl http://localhost:8080/api/v1/documents/{documentId}/desensitization-mappings \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

版本表 `kb_document_version` 会记录 `desensitize_status` 和 `desensitized_at`。

### 混合检索 API

当前提供可切换混合检索：默认使用数据库关键词检索和 mock 向量检索；设置 `SEARCH_ENGINE=opensearch` 后关键词召回走 OpenSearch；设置 `VECTOR_STORE_ENGINE=milvus` 后向量召回走 Milvus REST。权限过滤仍在检索链路前计算，非知识库 owner 的真实外部检索会回退到后端数据库权限检索，避免外部索引绕过文档权限。

```bash
curl -X POST http://localhost:8080/api/retrieval/search \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001' \
  -d '{
    "query": "金融行业数据治理 proposal 有哪些类似案例？",
    "space_id": 123,
    "filters": {
      "doc_type": "proposal",
      "industry": "金融",
      "service_line": "数据治理",
      "year_from": 2022
    },
    "top_k": 20
  }'
```

响应仍使用统一格式，`data.results` 中包含引用溯源字段：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "results": [
      {
        "chunk_id": 1,
        "doc_id": 2,
        "doc_title": "金融数据治理 Proposal",
        "page_no": 3,
        "section_title": "项目背景",
        "content": "...",
        "score": 0.89,
        "source_uri": "local://..."
      }
    ]
  }
}
```

权限 MVP 规则：

- 支持 `kb_user`、`kb_role`、`kb_user_role`、`kb_permission_policy`，当前用户通过 `X-User-Id` 和 `X-Tenant-Id` 解析。
- 支持 `kb_permission_policy` 中 `USER` / `ROLE` 主体、`SPACE` / `DOCUMENT` 资源、`ALLOW` / `DENY` 效果。
- MVP 动作包括 `space_read`、`document_read`、`document_upload`、`document_delete`、`agent_chat`、`admin_manage`。历史兼容动作 `READ` / `RETRIEVE` / `SEARCH` 会映射为文档读取能力。
- `DENY` 优先于 `ALLOW`。
- 空间 owner 默认拥有该空间及其文档权限。
- 非 owner 必须匹配有效 `ALLOW`，没有策略时不再默认放行。
- 检索前先计算当前用户可访问的 `document_id` 范围，再执行关键词检索和 mock 向量检索。
- 文档详情、删除、解析状态、重切片、Agent 问答均在后端强校验；前端权限判断只作为体验优化。
- 权限拒绝和关键访问动作会写入 `kb_audit_log`。

### Rerank 与上下文组装

后端已预留检索后处理服务，用于后续问答链路：

- `RerankService`：调用 `Reranker` 对候选 chunk 重排序，并限制单个文档最多贡献的 chunk 数量。
- `RuleBasedReranker`：基于 query 关键词命中、`doc_type`、`industry`、`service_line` 和文档新鲜度加分。
- `CrossEncoderReranker`：占位实现，后续可接入真实 cross-encoder / rerank 模型。
- `ContextBuilderService`：合并同一文档相邻 chunk，并输出带 `[引用1]`、`[引用2]` 的 LLM 上下文，同时返回结构化 citation。

### Agent 问答 API

当前 MVP 问答链路为：问题理解 -> 权限过滤后的混合检索 -> RuleBased Rerank -> 上下文组装 -> Mock LLM 生成 -> 保存会话、消息和引用来源。真实 Qwen / OpenAI / 私有模型后续可替换 `LlmProvider` 实现。

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001' \
  -d '{
    "space_id": 123,
    "session_id": null,
    "query": "请根据历史资料总结金融行业数据治理 proposal 的常见结构",
    "filters": {
      "doc_type": "proposal",
      "industry": "金融"
    }
  }'
```

响应仍使用统一格式：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session_id": 456,
    "answer": "基于当前知识库资料，... [引用1]",
    "citations": [
      {
        "citation_id": 1,
        "doc_id": 2,
        "doc_title": "金融数据治理 Proposal",
        "page_no": 12,
        "section_title": "解决方案"
      }
    ]
  }
}
```

若检索不到可用 chunk，Mock LLM 会返回“未在当前知识库中找到可靠依据”，并且不会生成引用记录，避免编造来源。

### RAG 问答评估 API

评估模块用于构建测试集并批量评估当前知识库 Agent 的检索、回答、引用和权限安全表现。当前 MVP 使用规则评估，不依赖大模型裁判。

创建评估数据集：

```bash
curl -X POST http://localhost:8080/api/eval/dataset \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001' \
  -d '{
    "tenant_id": 1001,
    "space_id": 123,
    "name": "金融 proposal 回归集",
    "description": "检索、问答和引用准确率评估"
  }'
```

新增评估问题：

```bash
curl -X POST http://localhost:8080/api/eval/case \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001' \
  -d '{
    "dataset_id": 456,
    "question": "请总结金融行业数据治理 proposal 的常见结构",
    "expected_answer": "项目背景、现状诊断、解决方案、实施路径、交付计划",
    "expected_doc_ids": [2],
    "expected_chunk_ids": [1],
    "expect_no_answer": false,
    "filters": {
      "doc_type": "proposal",
      "industry": "金融"
    },
    "tags": ["proposal", "金融"]
  }'
```

运行评估并查询报告：

```bash
curl -X POST http://localhost:8080/api/eval/run \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001' \
  -d '{"dataset_id":456,"top_k":20}'

curl http://localhost:8080/api/eval/result/{runId} \
  -H 'X-User-Id: 42' \
  -H 'X-Tenant-Id: 1001'
```

评估报告会写入 `kb_eval_result.detail_json`，并返回：

- `recall_at_k`：期望 chunk 或文档是否出现在 TopK 检索结果中。
- `precision_at_k`：MVP 阶段按每个用例是否命中期望结果计算平均命中率，后续可升级为逐结果 precision。
- `mrr`：期望 chunk 或文档首次出现位置的倒数排名。
- `citation_accuracy`：回答引用是否命中期望文档。
- `no_answer_accuracy`：期望拒答用例是否返回“未在当前知识库中找到可靠依据”且不生成引用。
- `permission_violation_count`：检索结果或答案引用中出现当前用户无权限文档的用例数。

单条 case 报告会包含 `retrieved_chunk_ids`、`retrieved_doc_ids`、`cited_doc_ids`、`inaccessible_expected_target_count`、`unauthorized_citation_count`、`unauthorized_retrieved_count` 等字段，方便定位失败原因。

## 启动 Python AI 服务

```bash
cd ai-service
cp .env.example .env
python3 -m venv .venv
source .venv/bin/activate
python -m pip install '.[dev]'
uvicorn app.main:app --reload --port 8001
```

健康检查：

```bash
curl http://localhost:8001/api/v1/health
```

已预留接口：

- `POST /api/v1/documents/parse`
- `POST /api/v1/chunks/split`
- `POST /api/v1/embeddings`
- `POST /api/v1/rerank`
- `POST /api/v1/llm/answer`

## 启动前端

```bash
cd web
pnpm install
pnpm dev --host 0.0.0.0
```

前端 API 地址通过环境变量配置。开发环境默认使用 Vite proxy 转发到 `http://localhost:8080`：

```bash
cp .env.example .env
```

如需直连后端：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

浏览器访问：

```text
http://localhost:5173
```

前端 MVP 页面：

- Mock 登录
- 知识库管理
- 知识库详情
- 文档上传
- 文档列表与详情
- 文档解析任务状态
- 智能问答三栏工作台
- 引用来源展示
- 评估数据集与评估报告

## 一键启动本地全栈

先复制环境变量模板。模板里都是本地占位密码，请按需替换，不要把真实密钥提交到仓库：

```bash
cp deploy/.env.example deploy/.env
cp ai-service/.env.example ai-service/.env
```

默认 MVP 一键启动 PostgreSQL、Redis、MinIO、后端、Python AI 服务和前端：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

### 分环境启动

按需选择环境组合，基础环境是 MVP 最低要求：

```bash
# 基础环境（数据库 + 缓存 + 存储 + 应用）
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build postgres redis minio backend ai-service web

# 搜索环境（基础 + OpenSearch）
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile search up -d --build opensearch

# 向量环境（基础 + Milvus）
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile vector up -d --build etcd milvus-minio milvus
```

访问地址：

```text
前端控制台：http://localhost:5173
后端 API：http://localhost:8080
AI 服务：http://localhost:8001
MinIO 控制台：http://localhost:9001
```

数据库初始化由后端启动时的 Flyway 自动完成。只初始化数据库和中间件，不启动应用时：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d postgres redis minio
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

如果需要 MySQL，可启用 `mysql` profile：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile mysql up -d mysql
```

如果需要 OpenSearch，可启用 `search` profile：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile search up -d opensearch
```

如果需要 Milvus，可启用 `vector` profile：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile vector up -d etcd milvus-minio milvus
```

同时启动全部可选中间件：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml \
  --profile mysql --profile search --profile vector \
  up -d --build
```

### 中间件初始化

启动 OpenSearch 或 Milvus profile 后，运行初始化脚本创建 index template / collection / bucket（幂等，可重复执行）：

```bash
# 仅 MinIO bucket（基础环境初始化）
bash deploy/scripts/init-all.sh

# + OpenSearch index template
bash deploy/scripts/init-all.sh --search

# + Milvus collection（需要先安装 pymilvus）
cd ai-service && .venv/bin/python -m pip install '.[deploy]' && cd ..
bash deploy/scripts/init-all.sh --search --vector
```

### 健康检查分组

后端 Actuator 健康检查按组件分组，每个分组独立报告状态，可选中间件未启动不会拖垮整体健康：

```bash
# 整体健康（db + redis 必须可用；storage/ai-service/search/vector 软探测）
curl http://localhost:8080/actuator/health

# 分组查看
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/redis
curl http://localhost:8080/actuator/health/storage
curl http://localhost:8080/actuator/health/ai-service
curl http://localhost:8080/actuator/health/search
curl http://localhost:8080/actuator/health/vector
```

- `db` / `redis`：不可用时该分组报告 DOWN，同时影响整体健康。
- `storage` / `ai-service` / `search` / `vector`：始终报告 UP，探测失败仅在 detail 中记录 warning，保证本地不启动可选中间件时整体仍为 UP。

### 本地压测脚本

```bash
# 批量上传种子文档到知识库（自动创建知识空间）
./scripts/seed-documents.sh

# 检索延迟测试（10 轮，输出 p50/p95）
./scripts/bench-retrieval.sh

# 自定义参数
ROUNDS=20 TOP_K=10 SPACE_ID=42 ./scripts/bench-retrieval.sh
```

停止服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

清空本地数据卷后重建数据库：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down -v
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

如果 Docker 拉取或构建时报类似 `failed size validation`、`failed commit on ref "unknown-sha256:..."`，通常是镜像 tag 的 blob 下载/校验异常、镜像代理返回不完整 blob，或本地 BuildKit content store 缓存损坏。已验证 `minio/minio:latest` 可正常拉取；如果你的 `deploy/.env` 里仍是固定 MinIO release tag，请先改为：

```bash
MINIO_IMAGE=minio/minio:latest
MILVUS_MINIO_IMAGE=minio/minio:latest
```

然后清理缓存并重拉：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
docker builder prune -f
docker image prune -f
docker compose --env-file deploy/.env -f deploy/docker-compose.yml pull postgres redis minio
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

如果问题仍然指向某个可选中间件，先只启动 MVP 必需服务，再单独启用对应 profile 排查：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d postgres redis minio backend ai-service web
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile search up -d opensearch
docker compose --env-file deploy/.env -f deploy/docker-compose.yml --profile vector up -d etcd milvus-minio milvus
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build backend
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build ai-service
```

## 验证命令

后端：

```bash
mvn -Dmaven.repo.local=.m2/repository -f backend/pom.xml test
```

AI 服务：

```bash
cd ai-service
.venv/bin/python -m pytest
```

前端：

```bash
cd web
pnpm build
```

Docker Compose 配置静态校验：

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml \
  --profile mysql --profile search --profile vector config
```

## 当前阶段边界

当前已经具备文档上传、解析入库任务、脱敏、切片、混合检索、Rerank、Agent 问答、权限过滤、评估和前端 MVP 控制台。下一步建议按顺序补：

1. 文档解析任务异步调度和前端轮询。
2. OpenSearch / Milvus 真实索引写入适配。
3. 前端权限菜单、成员管理和策略配置。
4. 评估数据集列表、case 列表和历史运行记录。
5. 真实 LLM、Embedding、Rerank 模型配置页面。
