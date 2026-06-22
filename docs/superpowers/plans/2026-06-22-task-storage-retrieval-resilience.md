# Task, Storage, and Retrieval Resilience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make background task execution idempotent, allow MinIO-backed documents to reach the AI parser, and keep hybrid retrieval responsive when one search branch fails or times out.

**Architecture:** Repository-level conditional updates are the task claim authority, so only one worker can transition a task from `PENDING` to `RUNNING`. Storage resolution returns a closeable local file handle; local files are borrowed while MinIO objects are downloaded to temporary files and deleted after parsing. Retrieval branches execute concurrently behind one timeout/fallback helper and merge every successful branch.

**Tech Stack:** Spring Boot 3, Spring Data JPA, MinIO Java SDK, JUnit 5, Mockito, AssertJ.

---

### Task 1: Atomic and idempotent task claims

**Files:**
- Modify: `backend/src/main/java/com/sunxin/knowledge/persistence/repository/KbDocumentParseTaskRepository.java`
- Modify: `backend/src/main/java/com/sunxin/knowledge/persistence/repository/KbEmbeddingIndexTaskRepository.java`
- Modify: `backend/src/main/java/com/sunxin/knowledge/task/DocumentParseTaskExecutionService.java`
- Modify: `backend/src/main/java/com/sunxin/knowledge/task/EmbeddingIndexTaskExecutionService.java`
- Test: `backend/src/test/java/com/sunxin/knowledge/task/TaskClaimExecutionServiceTest.java`

- [x] Write tests proving a duplicate `process(id)` call does not invoke parsing or indexing when the conditional claim returns zero.
- [x] Run the focused test and confirm it fails because conditional claim methods do not exist.
- [x] Add repository `update ... where status = PENDING` claim queries and make execution continue only after one updated row.
- [x] Run focused tests and the task package tests.

### Task 2: MinIO upload and parse-file resolution

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/sunxin/knowledge/document/storage/MinioStorageProperties.java`
- Create: `backend/src/main/java/com/sunxin/knowledge/document/storage/MinioClientConfiguration.java`
- Create: `backend/src/main/java/com/sunxin/knowledge/document/storage/MinioFileStorageService.java`
- Create: `backend/src/main/java/com/sunxin/knowledge/document/storage/StoredFileResolver.java`
- Create: `backend/src/main/java/com/sunxin/knowledge/document/storage/ResolvedStoredFile.java`
- Create: `backend/src/main/java/com/sunxin/knowledge/document/storage/MinioStoredFileResolver.java`
- Modify: `backend/src/main/java/com/sunxin/knowledge/document/storage/LocalStoredFileResolver.java`
- Modify: `backend/src/main/java/com/sunxin/knowledge/task/DocumentParseTaskExecutionService.java`
- Test: `backend/src/test/java/com/sunxin/knowledge/document/storage/StoredFileResolverTest.java`

- [x] Write tests for safe local resolution, MinIO URI parsing, temporary download cleanup, and parser cleanup on failure.
- [x] Run focused tests and confirm failure because the resolver contract and MinIO implementation are absent.
- [x] Add the official MinIO Java SDK and conditional local/MinIO storage beans.
- [x] Resolve `local://` directly and `minio://bucket/key` through a temporary local file with deterministic cleanup.
- [x] Run focused storage/task tests.

### Task 3: Retrieval timeout and branch fallback

**Files:**
- Create: `backend/src/main/java/com/sunxin/knowledge/retrieval/application/RetrievalProperties.java`
- Create: `backend/src/main/java/com/sunxin/knowledge/retrieval/application/SearchBranchExecutor.java`
- Modify: `backend/src/main/java/com/sunxin/knowledge/retrieval/application/RetrievalSearchService.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-dev.yml`
- Test: `backend/src/test/java/com/sunxin/knowledge/retrieval/application/SearchBranchExecutorTest.java`

- [x] Write tests proving exceptions and timeouts become empty branch results while successful branches remain available.
- [x] Run the focused test and confirm it fails because the helper is absent.
- [x] Implement configurable per-branch timeout and structured warning logs.
- [x] Route keyword and vector futures through the helper without changing permission-before-search behavior.
- [x] Run retrieval unit/integration tests.

### Task 4: Full verification and documentation

**Files:**
- Modify: `README.md`
- Modify: `deploy/.env.example`

- [x] Document `KNOWLEDGE_STORAGE_TYPE=minio`, MinIO credentials, and retrieval timeout configuration.
- [x] Run `mvn test` for the backend.
- [x] Review `git diff --check`, changed files, and remaining operational risks.
