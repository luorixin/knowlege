package com.sunxin.knowledge.document.dto;
import com.sunxin.knowledge.document.domain.DocumentStatus;

public record DocumentUploadResponse(
        Long documentId,
        Long versionId,
        Long parseTaskId,
        String title,
        String docType,
        String industry,
        String serviceLine,
        String confidentialLevel,
        String sourceUri,
        String fileHash,
        DocumentStatus status,
        String parseStatus,
        boolean duplicated
) {
}
