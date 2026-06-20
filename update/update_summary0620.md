# 系统更新说明（未提交改动汇总）

本次系统迭代包含了大面积的代码重构与新功能引入，主要聚焦于**多模态数据清洗管线**、**Query改写理解**，以及**LLM-as-a-Judge 离线评测门禁系统**三大核心模块。以下为当前工作区未提交代码（Uncommitted Changes）的具体改动汇总：

## 1. 核心功能新增与重构

### 1.1 多模态文档清洗管线 (Document Cleaning Pipeline)
* **涉及路径**: `backend/src/main/java/com/sunxin/knowledge/document/cleaning/` (全新模块)
* **核心改动**:
  * **抽取独立清洗模块**：将原先散落在各处的清洗逻辑正式抽象为了 `DocumentCleaningService`，并定义了标准化的 `DocumentCleaner` 接口。
  * **新增白名单/空白字符清洗**：实现了具体的 `WhitespaceNormalizationCleaner`，对文档内容中残留的无用空字符进行统一规范化处理。
  * **结构化清洗报告**：清洗服务现统一返回全新的 Record `CleanedDocumentResult`（包含处理后的 Pages 与 `DocumentCleaningReport`）。它不仅保证了数据流的纯粹，还便于下游在 metadata 中安全挂载清洗结果统计（如被清洗字符数、噪声清理等）。

### 1.2 Query 改写与意图理解 (Query Rewrite)
* **涉及路径**:
  * `QuestionUnderstandingService.java`
  * `LlmProvider.java` / `OpenAiCompatibleLlmProvider.java`
  * `QueryRewriteResult.java` (新增)
* **核心改动**:
  * **意图与检索词优化**：引入专门的 Query 重写逻辑，向 LLM 下发重写提示词，剥离上下文中的无效信息，生成更贴合向量检索和关键词检索逻辑的重写词（`QueryRewriteResult`）。
  * **模型侧适配**：拓展了 `LlmProvider` 接口，支持原生的改写方法。同时优化了 `OpenAiCompatibleLlmProvider` 的相关配置，并为了避免企业数据与用户隐私外泄，将含有原始 Query/重写结果的日志级别由 `INFO` 降级为 `DEBUG`。

### 1.3 LLM-as-a-Judge 离线评测门禁 (LLM Judge Evaluation Gate)
* **涉及路径**: 
  * `LlmAsAJudgeRagEvaluator.java` (新增)
  * `EvalService.java`, `EvalReportAggregator.java`
  * `RuleBasedRagEvaluator.java`, `RuleBasedParserEvaluator.java`
  * `EvalCaseReportResponse.java`, `EvalMetricsResponse.java`
* **核心改动**:
  * **新增法官评测器**：引入大模型来作为离线裁判进行双重打分：上下文相关性分数 (`contextRelevanceScore`) 和 回答忠实度分数 (`answerFaithfulnessScore`)。
  * **更新评测模型 DTO**：在基础的评测报告体中新增了由模型给出的 `llmJudgeReason` 以及决定是否符合上线标准的 `qualityGatePassed` 门禁阈值判定属性。
  * **管线闭环**：`EvalService` 现在实现了“规则评测”+“模型评测”的无缝衔接。每一条测试案例跑完后，会自动将两组指标进行合并汇总并传递给 Aggregator。

---

## 2. 工程安全与架构演进

### 2.1 深度防御性编程修复
* **涉及路径**: `DocumentParseTaskExecutionService.java` 等。
* **修复内容**：在进行文档元数据 (metadata) 合并/清洗时，全面移除了直接操作对象引用的行为。改为使用 `new LinkedHashMap<>(originalMap)` 进行安全的浅拷贝，彻底排雷了由于 Java `Map.of()` 生成不可变集合所诱发的运行时 `UnsupportedOperationException`。

### 2.2 基础检索设施的方法签名对齐
* **涉及路径**: 
  * `VectorStoreClient.java`, `MockVectorStoreClient.java`, `MilvusVectorStoreClient.java`
  * `KeywordSearchClient.java`, `NoopKeywordSearchClient.java`, `OpenSearchKeywordSearchClient.java`
* **修复内容**：为了适配前沿的多路召回能力，对向量检索、关键字检索底层客户端的所有统一接口和存根实现进行了整理，扩充了必要的方法签名或重载接口（包括对新的 Search API 需求参数的支持）。

---

## 3. 测试与环境清理

* 代码格式化统一：清理了部分文件（如 `OpenAiCompatibleLlmProvider.java`）遗留的 Trailing Whitespaces (行尾空格)。
* 临时文件删除：执行期间产生的一次性批量 Patch 测试脚本以及各种游离状态记录文件均已清理，维持本地仓库树的整洁。
* **本地状态**：代码均已通过 `mvn clean compile test-compile` 纯编译检验流程，不存在任何因强类型变更引发的 Build Break 风险。

> [!TIP]
> **后续建议**：
> 目前代码树已在功能层面闭环。如果 Review 完毕，您可以直接通过 `git add backend/src` 以及 `git commit -m "feat(rag): 引入数据清洗、Query改写与 LLM-as-a-Judge 离线门禁体系"` 进行代码提交。
