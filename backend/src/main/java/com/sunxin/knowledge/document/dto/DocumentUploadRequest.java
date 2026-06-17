package com.sunxin.knowledge.document.dto;

public record DocumentUploadRequest(
        String title,
        String industry,
        String serviceLine,
        String confidentialLevel
) {
}
