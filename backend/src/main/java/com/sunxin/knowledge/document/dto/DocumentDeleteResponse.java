package com.sunxin.knowledge.document.dto;
import com.sunxin.knowledge.document.domain.DocumentStatus;

public record DocumentDeleteResponse(
        Long documentId,
        DocumentStatus status
) {
}
