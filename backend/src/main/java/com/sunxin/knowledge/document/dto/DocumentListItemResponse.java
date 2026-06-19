package com.sunxin.knowledge.document.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.document.domain.DocumentStatus;

import com.sunxin.knowledge.persistence.entity.KbDocument;

public record DocumentListItemResponse(
        Long id,
        Long spaceId,
        String title,
        String docType,
        String industry,
        String serviceLine,
        String confidentialLevel,
        String fileHash,
        DocumentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentListItemResponse fromEntity(KbDocument document) {
        return new DocumentListItemResponse(
                document.getId(),
                document.getSpaceId(),
                document.getTitle(),
                document.getDocType(),
                document.getIndustry(),
                document.getServiceLine(),
                document.getConfidentialLevel(),
                document.getFileHash(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
