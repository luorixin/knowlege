package com.sunxin.knowledge.task.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;

public record UnifiedTaskResponse(
        @JsonProperty("task_key")
        String taskKey,

        @JsonProperty("task_id")
        Long taskId,

        @JsonProperty("task_category")
        String taskCategory,

        @JsonProperty("task_type")
        String taskType,

        @JsonProperty("stage_label")
        String stageLabel,

        @JsonProperty("tenant_id")
        Long tenantId,

        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("doc_id")
        Long docId,

        @JsonProperty("document_title")
        String documentTitle,

        @JsonProperty("version_id")
        Long versionId,

        @JsonProperty("chunk_id")
        Long chunkId,

        String status,

        @JsonProperty("retry_count")
        Integer retryCount,

        @JsonProperty("progress_percent")
        Integer progressPercent,

        @JsonProperty("model_provider")
        String modelProvider,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("embedding_dimension")
        Integer embeddingDimension,

        @JsonProperty("index_name")
        String indexName,

        @JsonProperty("vector_collection")
        String vectorCollection,

        @JsonProperty("worker_id")
        String workerId,

        @JsonProperty("started_at")
        LocalDateTime startedAt,

        @JsonProperty("finished_at")
        LocalDateTime finishedAt,

        @JsonProperty("error_code")
        String errorCode,

        @JsonProperty("error_message")
        String errorMessage,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("retryable")
        boolean retryable,

        @JsonProperty("runnable")
        boolean runnable
) {

    private static final String FAILED = "FAILED";
    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";

    public static UnifiedTaskResponse fromParseTask(KbDocumentParseTask task, KbDocument document) {
        return new UnifiedTaskResponse(
                "parse-" + task.getId(),
                task.getId(),
                "PARSE_CHUNK",
                "PARSE_AND_CHUNK",
                "文档解析 / 内容切片",
                task.getTenantId(),
                task.getSpaceId(),
                task.getDocId(),
                document == null ? null : document.getTitle(),
                task.getVersionId(),
                null,
                task.getStatus(),
                task.getRetryCount(),
                task.getProgressPercent(),
                null,
                null,
                null,
                null,
                null,
                task.getWorkerId(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                FAILED.equals(task.getStatus()),
                PENDING.equals(task.getStatus()) || RUNNING.equals(task.getStatus())
        );
    }

    public static UnifiedTaskResponse fromEmbeddingTask(KbEmbeddingIndexTask task, KbDocument document) {
        return new UnifiedTaskResponse(
                "embedding-" + task.getId(),
                task.getId(),
                "EMBEDDING_INDEX",
                "EMBEDDING_AND_INDEX",
                "Embedding / 检索索引",
                task.getTenantId(),
                task.getSpaceId(),
                task.getDocId(),
                document == null ? null : document.getTitle(),
                task.getVersionId(),
                task.getChunkId(),
                task.getStatus(),
                task.getRetryCount(),
                task.getProgressPercent(),
                task.getModelProvider(),
                task.getModelName(),
                task.getEmbeddingDimension(),
                task.getIndexName(),
                task.getVectorCollection(),
                null,
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                FAILED.equals(task.getStatus()),
                PENDING.equals(task.getStatus()) || RUNNING.equals(task.getStatus())
        );
    }
}
