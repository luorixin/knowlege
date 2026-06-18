package com.sunxin.knowledge.integration.search;

import java.time.LocalDateTime;

public record IndexedChunkDocument(
        Long tenantId,
        Long spaceId,
        Long docId,
        Long versionId,
        Long chunkId,
        String docTitle,
        String docType,
        String industry,
        String serviceLine,
        LocalDateTime createdAt,
        Integer chunkIndex,
        Integer pageNo,
        String sectionTitle,
        String content,
        String metadataJson,
        String indexName
) {
}
