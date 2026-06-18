package com.sunxin.knowledge.task.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;

public record ParseTaskResponse(
        Long id,

        @JsonProperty("tenant_id")
        Long tenantId,

        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("doc_id")
        Long docId,

        @JsonProperty("version_id")
        Long versionId,

        @JsonProperty("task_type")
        String taskType,

        String status,

        @JsonProperty("retry_count")
        Integer retryCount,

        @JsonProperty("progress_percent")
        Integer progressPercent,

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
        LocalDateTime updatedAt
) {

    public static ParseTaskResponse fromEntity(KbDocumentParseTask task) {
        return new ParseTaskResponse(
                task.getId(),
                task.getTenantId(),
                task.getSpaceId(),
                task.getDocId(),
                task.getVersionId(),
                task.getTaskType(),
                task.getStatus(),
                task.getRetryCount(),
                task.getProgressPercent(),
                task.getWorkerId(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
