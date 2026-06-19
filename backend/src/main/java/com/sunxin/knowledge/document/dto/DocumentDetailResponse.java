package com.sunxin.knowledge.document.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.document.domain.DocumentStatus;

import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;

public record DocumentDetailResponse(
        Long id,
        Long tenantId,
        Long spaceId,
        String title,
        String docType,
        String industry,
        String serviceLine,
        String confidentialLevel,
        String sourceUri,
        String fileHash,
        Long fileSize,
        DocumentStatus status,
        DocumentVersionResponse currentVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentDetailResponse fromEntity(KbDocument document, KbDocumentVersion version) {
        return new DocumentDetailResponse(
                document.getId(),
                document.getTenantId(),
                document.getSpaceId(),
                document.getTitle(),
                document.getDocType(),
                document.getIndustry(),
                document.getServiceLine(),
                document.getConfidentialLevel(),
                document.getSourceUri(),
                document.getFileHash(),
                document.getFileSize(),
                document.getStatus(),
                DocumentVersionResponse.fromEntity(version),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
