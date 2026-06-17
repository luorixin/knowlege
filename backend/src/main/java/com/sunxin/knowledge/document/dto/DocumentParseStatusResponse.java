package com.sunxin.knowledge.document.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;

public record DocumentParseStatusResponse(
        Long documentId,
        Long versionId,
        Long parseTaskId,
        String taskType,
        String status,
        Integer progressPercent,
        String parseStatus,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentParseStatusResponse fromEntities(Long documentId, KbDocumentVersion version, KbDocumentParseTask task) {
        return new DocumentParseStatusResponse(
                documentId,
                version.getId(),
                task == null ? null : task.getId(),
                task == null ? null : task.getTaskType(),
                task == null ? version.getParseStatus() : task.getStatus(),
                task == null ? null : task.getProgressPercent(),
                version.getParseStatus(),
                task == null ? null : task.getErrorCode(),
                task == null ? null : task.getErrorMessage(),
                task == null ? version.getCreatedAt() : task.getCreatedAt(),
                task == null ? version.getUpdatedAt() : task.getUpdatedAt()
        );
    }
}
