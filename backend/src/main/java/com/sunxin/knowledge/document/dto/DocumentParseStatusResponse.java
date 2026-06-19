package com.sunxin.knowledge.document.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.task.domain.TaskStatus;

import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.task.dto.ParseMetadataResponse;

public record DocumentParseStatusResponse(
        Long documentId,
        Long versionId,
        Long parseTaskId,
        String taskType,
        TaskStatus status,
        Integer progressPercent,
        String parseStatus,
        String errorCode,
        String errorMessage,
        ParseMetadataResponse metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentParseStatusResponse fromEntities(Long documentId, KbDocumentVersion version, KbDocumentParseTask task) {
        return new DocumentParseStatusResponse(
                documentId,
                version.getId(),
                task == null ? null : task.getId(),
                task == null ? null : task.getTaskType(),
                task == null ? (version.getParseStatus() != null ? TaskStatus.valueOf(version.getParseStatus()) : null) : task.getStatus(),
                task == null ? null : task.getProgressPercent(),
                version.getParseStatus(),
                task == null ? null : task.getErrorCode(),
                task == null ? null : task.getErrorMessage(),
                task == null ? null : ParseMetadataResponse.safeParse(task.getMetadataJson()),
                task == null ? version.getCreatedAt() : task.getCreatedAt(),
                task == null ? version.getUpdatedAt() : task.getUpdatedAt()
        );
    }
}
