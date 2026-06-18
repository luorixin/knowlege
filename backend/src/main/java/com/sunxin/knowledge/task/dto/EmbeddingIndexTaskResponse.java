package com.sunxin.knowledge.task.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;

public record EmbeddingIndexTaskResponse(
        Long id,

        @JsonProperty("tenant_id")
        Long tenantId,

        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("doc_id")
        Long docId,

        @JsonProperty("version_id")
        Long versionId,

        @JsonProperty("chunk_id")
        Long chunkId,

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

        String status,

        @JsonProperty("retry_count")
        Integer retryCount,

        @JsonProperty("progress_percent")
        Integer progressPercent,

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
        LocalDateTime updatedAt
) {

    public static EmbeddingIndexTaskResponse fromEntity(KbEmbeddingIndexTask task) {
        return new EmbeddingIndexTaskResponse(
                task.getId(),
                task.getTenantId(),
                task.getSpaceId(),
                task.getDocId(),
                task.getVersionId(),
                task.getChunkId(),
                task.getModelProvider(),
                task.getModelName(),
                task.getEmbeddingDimension(),
                task.getIndexName(),
                task.getVectorCollection(),
                task.getStatus(),
                task.getRetryCount(),
                task.getProgressPercent(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
