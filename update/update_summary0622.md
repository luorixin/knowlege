# 未提交改动汇总（2026-06-22）

本文件基于当前工作区 `git status --short --untracked-files=all` 与 `git diff --stat` 整理，覆盖当前所有未提交改动。当前未提交内容横跨后端、AI 服务、前端、运行态文件与原型素材；提交前建议先按模块拆分 commit，避免把运行态缓存和 UI 原型资产混入核心代码提交。

## 总览

- 后端改动：文档清洗、解析任务状态、语义 chunking、评估链路、Query 改写容错、文档预览响应头、Redis Cache 测试环境兼容。
- AI 服务改动：OCR 默认开关调整。
- 前端改动：整体视觉风格与多页面大幅改版，新增 Office 文档预览依赖，Docker 安装策略调整。
- 新增测试：chunking 单测、清洗器单测。
- 运行态/临时资产：`.omx/` 状态文件、`.pnpm-store/`、`sticth/update0620/` 截图与 React/Vite 原型、`update/walkthrough_phase2.md`。

## 后端改动

### 1. 文档 Chunk 拆分升级

涉及文件：

- `backend/src/main/java/com/sunxin/knowledge/document/chunking/DocumentChunker.java`
- `backend/src/main/java/com/sunxin/knowledge/document/chunking/ChunkingProperties.java`
- `backend/src/main/java/com/sunxin/knowledge/document/application/DocumentChunkingService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/test/java/com/sunxin/knowledge/document/chunking/DocumentChunkerTest.java`
- `backend/src/test/java/com/sunxin/knowledge/document/DocumentChunkingApiTest.java`

主要变化：

- 从原先的标题/页策略后固定字符截断，升级为结构优先的语义切分：
  `ParsedPage -> SourceSegment -> semantic split -> budget pack -> boundary-safe overlap -> ChunkDraft`。
- 长文本优先按标题、段落、句子、列表项边界切分；只有超长单元才字符兜底。
- 增加不同内容类型策略：
  - 普通文本：语义边界切分，并保留边界安全 overlap。
  - PPT：默认按页切，避免机械 overlap。
  - Excel/table：按表格行窗口切，每个 chunk 重复表头。
  - figure/image：caption 独立成 chunk，并保留图片相关 metadata。
- chunk metadata 增加：
  - `section_path`
  - `heading_level`
  - `parent_section`
  - `split_strategy`
  - `chunking_strategy_version`
  - `char_count`
  - `estimated_token_count`
  - `chunk_quality_score`
  - `too_short`
  - `truncated_by_fallback`
  - `source_block_ids`
- 配置增加：
  - `knowledge.chunking.target-size`
  - `knowledge.chunking.max-size`
  - `knowledge.chunking.min-size`
  - `knowledge.chunking.overlap`
  - `knowledge.chunking.sentence-boundary-enabled`
  - `knowledge.chunking.table-header-repeat-enabled`
  - `knowledge.chunking.strategy-version`
- `DocumentChunkingService` 在解析任务 metadata 中记录 `chunking_strategy_version`，为后续判断 embedding/index 是否需要重建预留依据。
- 测试从固定 chunk 数量与机械 overlap 断言，调整为语义行为、metadata 和任务状态断言。

已验证：

```bash
cd backend
mvn -Dmaven.repo.local=../.m2/repository -Dtest=DocumentChunkerTest,DocumentChunkingApiTest test
mvn -Dmaven.repo.local=../.m2/repository test
```

结果：相关测试 10 个通过；后端全量测试 43 个通过。

### 2. 文档清洗管线增强

涉及文件：

- `backend/src/main/java/com/sunxin/knowledge/document/cleaning/DocumentCleaningReport.java`
- `backend/src/main/java/com/sunxin/knowledge/document/cleaning/HeaderFooterCleaner.java`
- `backend/src/main/java/com/sunxin/knowledge/document/cleaning/WhitespaceNormalizationCleaner.java`
- `backend/src/main/java/com/sunxin/knowledge/document/cleaning/DuplicateBlockCleaner.java`
- `backend/src/main/java/com/sunxin/knowledge/document/cleaning/OcrQualityCleaner.java`
- `backend/src/test/java/com/sunxin/knowledge/document/cleaning/DuplicateBlockCleanerTest.java`
- `backend/src/test/java/com/sunxin/knowledge/document/cleaning/HeaderFooterCleanerTest.java`
- `backend/src/test/java/com/sunxin/knowledge/document/cleaning/OcrQualityCleanerTest.java`

主要变化：

- `DocumentCleaningReport` 增加清洗规则版本与更多统计字段：
  - `cleaning_rule_version`
  - `ocr_noise_removed_count`
  - `header_footer_removed_count`
  - `duplicate_removed_count`
  - `cleaned_char_count`
- `HeaderFooterCleaner` 从简单相邻页首行匹配升级为统计多页页眉/页脚频率，按阈值移除重复头尾行，并写入 `header_footer_cleaned` metadata。
- `WhitespaceNormalizationCleaner` 改为先复制 metadata，再写入 `whitespace_cleaned`，避免修改不可变 `Map.of()` 导致运行时异常。
- 新增 `DuplicateBlockCleaner`：
  - 使用字符 trigram + Jaccard 相似度识别近重复页面/块。
  - 默认相似度阈值为 `0.85`。
- 新增 `OcrQualityCleaner`：
  - 根据 OCR confidence 过滤低质量页面。
  - 根据乱码字符比例过滤 OCR 噪声。
  - 移除常见水印，如 `Internal Use Only`、`confidential`、`机密`、`内部使用`。
- 新增对应单测覆盖重复块、页眉页脚和 OCR 质量过滤。

### 3. 解析任务清洗状态接入

涉及文件：

- `backend/src/main/java/com/sunxin/knowledge/task/DocumentParseTaskExecutionService.java`
- `backend/src/main/java/com/sunxin/knowledge/task/domain/TaskStatus.java`

主要变化：

- 新增任务状态 `CLEANING`。
- 文档解析完成后、切片前，将任务状态更新为 `CLEANING`。
- 清洗报告不再作为字符串嵌套 JSON，而是以结构化 Map 写入第一页 metadata 的 `cleaning_report`。

### 4. 评估与问答链路增强

涉及文件：

- `backend/src/main/java/com/sunxin/knowledge/eval/application/EvalService.java`
- `backend/src/main/java/com/sunxin/knowledge/eval/application/CleaningImpactEvaluator.java`
- `backend/src/main/java/com/sunxin/knowledge/qa/application/QuestionUnderstandingService.java`
- `backend/src/main/java/com/sunxin/knowledge/qa/llm/OpenAiCompatibleLlmProvider.java`
- `backend/src/test/java/com/sunxin/knowledge/auth/PermissionEnforcementApiTest.java`

主要变化：

- `EvalService` 从纯规则评估扩展为规则评估 + LLM Judge 评估合并：
  - 保存类型从 `RULE_BASED` 调整为 `RULE_BASED_AND_LLM`。
  - 将上下文相关性、回答忠实度和 judge reason 写入 case report。
- 新增 `CleaningImpactEvaluator`：
  - 对比原始数据集评估结果与清洗后评估结果。
  - 输出 Recall、Citation Accuracy、MRR、Context Relevance、Answer Faithfulness 的 delta。
- `QuestionUnderstandingService` 增加 Query rewrite 容错：
  - rewrite 失败时回退原始 query。
  - 记录 fallback、失败次数和平均延迟指标。
  - 避免 rewrite 异常中断问答链路。
- `OpenAiCompatibleLlmProvider` 增加 `knowledge.llm.enable-json-format-response` 开关。
  - 默认向 OpenAI-compatible 请求体加入 `response_format: {"type":"json_object"}`。
- 权限测试的用户插入 SQL 增加 `password` 字段，适配 `kb_user` 表新增密码列。

### 5. 文档预览与缓存配置修复

涉及文件：

- `backend/src/main/java/com/sunxin/knowledge/document/api/DocumentController.java`
- `backend/src/main/java/com/sunxin/knowledge/config/CacheConfig.java`

主要变化：

- 文档下载接口从强制 `attachment` 调整为 `inline`，并根据文件名推断 `Content-Type`。
- text 类型无 charset 时补 UTF-8，改善浏览器内联预览体验。
- `RedisCacheManager` 增加 `@ConditionalOnBean(RedisConnectionFactory.class)`，避免 test profile 排除 Redis 自动配置后 Spring 上下文启动失败。

## AI 服务改动

涉及文件：

- `ai-service/app/config.py`

主要变化：

- `parser_enable_ocr` 默认值从 `False` 改为 `True`。

注意：

- 这会让本地/容器环境更倾向于走 OCR 路径。若未安装 OCR 可选依赖，或只希望纯文本解析，应在 `.env` 中显式设置 `PARSER_ENABLE_OCR=false`。

## 前端改动

涉及文件：

- `web/package.json`
- `web/Dockerfile`
- `web/.dockerignore`
- `web/src/App.vue`
- `web/src/styles/main.scss`
- `web/src/views/ChatView.vue`
- `web/src/views/DocumentUploadView.vue`
- `web/src/views/DocumentsView.vue`
- `web/src/views/EvalDatasetsView.vue`
- `web/src/views/KnowledgeBaseDetailView.vue`
- `web/src/views/KnowledgeBasesView.vue`
- `web/src/views/LoginView.vue`
- `web/src/views/PermissionsView.vue`
- `web/src/views/TasksView.vue`

主要变化：

- 整体 UI 从浅色企业后台风格大幅改成深色科技风控制台：
  - 深色背景、霓虹边框、侧边栏、移动端顶部栏、主题切换。
  - `App.vue` 引入响应式 sidebar 和 light/dark 切换状态。
- 多个业务页面重写视觉层：
  - 登录页
  - 知识库列表/详情
  - 文档上传/文档列表
  - 智能问答
  - 任务监控
  - 权限管理
  - 评估数据集
- `web/package.json` 新增 Office 文档预览依赖：
  - `@vue-office/docx`
  - `@vue-office/excel`
  - `@vue-office/pdf`
  - `@vue-office/pptx`
  - `vue-demi`
- `web/Dockerfile` 从 `pnpm install --frozen-lockfile` 改为 `pnpm install --no-frozen-lockfile`。

注意：

- 当前 `web/package.json` 已变更，但 `pnpm-lock.yaml` 未出现在未提交 diff 中。建议补跑前端安装并确认 lockfile 是否需要更新，否则 Docker 使用 `--no-frozen-lockfile` 会掩盖依赖锁不一致问题。
- 前端大幅视觉重构尚未在本次汇总中看到对应前端构建/截图验证结果，建议提交前执行：

```bash
cd web
pnpm install
pnpm typecheck
pnpm build
```

## 运行态、缓存与原型资产

### 1. OMX 运行态文件

涉及文件：

- `.omx/metrics.json`
- `.omx/state/session.json`
- `.omx/state/sessions/.../hud-state.json`
- `.omx/state/sessions/.../notify-hook-state.json`
- `.omx/state/subagent-tracking.json`
- `.omx/state/tmux-hook-state.json`

说明：

- 这些是本地 agent/runtime 状态文件，通常不建议进入业务提交。

### 2. pnpm 本地缓存

涉及文件：

- `.pnpm-store/v10/projects/8053a1b193049267e479b7db850384ae`

说明：

- 这是包管理器本地缓存，不建议提交。

### 3. Stitch/UI 原型与截图

涉及路径：

- `sticth/update0620/...`

内容包括：

- 多张页面截图：
  - `admin_permissions_management/screen.png`
  - `ai_intelligent_q_a_chat/screen.png`
  - `enterprise_kb_login/screen.png`
  - `knowledge_base_management_dashboard/screen.png`
  - `system_task_monitoring_console/screen.png`
- 一个独立 React/Vite 原型项目：
  - `sticth/update0620/html-ui-hotlinker/...`

说明：

- 如果这些是设计参考素材，可以单独归档提交。
- 如果只是临时生成物，不建议和主业务代码同 commit。

### 4. 其他 update 文档

涉及文件：

- `update/walkthrough_phase2.md`

说明：

- 属于项目文档资产，可按需要随本次总结文档一起提交。

## 建议提交拆分

建议不要一次性提交所有未提交内容，可以拆成以下几个 commit：

1. `backend: enhance document cleaning pipeline`
   - 文档清洗、OCR 质量过滤、重复块过滤、清洗报告、清洗任务状态、清洗相关测试。

2. `backend: implement semantic document chunking`
   - `DocumentChunker`、`ChunkingProperties`、chunking 配置、chunking 测试、任务 metadata 策略版本。

3. `backend: harden qa eval and runtime config`
   - Query rewrite fallback、LLM judge 接入、OpenAI JSON response 开关、CacheConfig 条件 Bean、权限测试 password 字段。

4. `web: redesign enterprise console pages`
   - `web/src/**`、`web/package.json`、`web/Dockerfile`、`web/.dockerignore`。
   - 提交前建议补 lockfile 或明确为什么不提交 lockfile。

5. `docs: add update summaries and walkthrough`
   - `update/*.md`。

6. 单独决定是否提交 `sticth/update0620`。

## 建议不要提交或需确认后提交

- `.omx/**`：本地运行态，建议忽略。
- `.pnpm-store/**`：本地依赖缓存，建议忽略。
- `sticth/update0620/html-ui-hotlinker/**`：独立 UI 原型项目，建议确认是否作为设计资产保留。
- `sticth/update0620/**/*.png`：截图资产，建议确认是否纳入文档/设计材料。

## 已知风险与待确认点

- 前端依赖新增但 lockfile 未变更，可能导致 CI、本地和 Docker 依赖解析不一致。
- AI 服务 OCR 默认开启后，对没有安装 OCR 可选依赖的环境可能带来启动或运行路径差异。
- 前端大面积视觉改造需要单独做浏览器截图 QA，尤其是移动端、长文本、表格和按钮文本溢出。
- 文档接口改为 inline 预览后，需要确认权限校验仍覆盖原文访问，并确认浏览器展示行为符合企业文档安全策略。
- `.omx`、`.pnpm-store` 和 `sticth` 是否进入版本库，需要在提交前明确。

## 当前验证记录

已确认后端测试通过：

```bash
cd backend
mvn -Dmaven.repo.local=../.m2/repository -Dtest=DocumentChunkerTest,DocumentChunkingApiTest test
mvn -Dmaven.repo.local=../.m2/repository test
```

未在本轮执行：

- AI 服务测试。
- 前端 typecheck/build。
- 浏览器端视觉回归。
