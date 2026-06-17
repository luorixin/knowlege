package com.sunxin.knowledge.retrieval.rerank;

import java.time.LocalDateTime;

public record RerankedChunk(
        Integer rank,
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
        Double rerankScore,
        String sourceUri
) {

    public static RerankedChunk fromCandidate(Integer rank, RerankCandidate candidate, Double rerankScore) {
        return new RerankedChunk(
                rank,
                candidate.chunkId(),
                candidate.docId(),
                candidate.docTitle(),
                candidate.docType(),
                candidate.industry(),
                candidate.serviceLine(),
                candidate.documentCreatedAt(),
                candidate.chunkIndex(),
                candidate.pageNo(),
                candidate.sectionTitle(),
                candidate.content(),
                candidate.retrievalScore(),
                rerankScore,
                candidate.sourceUri()
        );
    }

    public RerankedChunk withRank(Integer newRank) {
        return new RerankedChunk(
                newRank,
                chunkId,
                docId,
                docTitle,
                docType,
                industry,
                serviceLine,
                documentCreatedAt,
                chunkIndex,
                pageNo,
                sectionTitle,
                content,
                retrievalScore,
                rerankScore,
                sourceUri
        );
    }
}
