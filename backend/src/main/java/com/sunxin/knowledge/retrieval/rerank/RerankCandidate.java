package com.sunxin.knowledge.retrieval.rerank;

import java.time.LocalDateTime;

public record RerankCandidate(
        Long chunkId,
        Long docId,
        String docTitle,
        String docType,
        String industry,
        String serviceLine,
        LocalDateTime documentCreatedAt,
        Integer chunkIndex,
        Integer pageNo,
        String sectionTitle,
        String content,
        Double retrievalScore,
        String sourceUri
) {
}
