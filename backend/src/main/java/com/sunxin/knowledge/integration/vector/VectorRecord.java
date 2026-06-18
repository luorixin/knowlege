package com.sunxin.knowledge.integration.vector;

import java.util.List;

public record VectorRecord(
        Long tenantId,
        Long spaceId,
        Long docId,
        Long versionId,
        Long chunkId,
        String docTitle,
        String docType,
        String industry,
        String serviceLine,
        String collectionName,
        List<Double> embedding,
        String metadataJson
) {
}
