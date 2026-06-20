package com.sunxin.knowledge.document.cleaning;

public record DocumentCleaningContext(
        Long docId,
        Long versionId,
        String fileType
) {
}
