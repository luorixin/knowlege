package com.sunxin.knowledge.document.dto;

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
        String status,
        String parseStatus,
        boolean duplicated
) {
}
