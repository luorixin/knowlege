package com.sunxin.knowledge.retrieval.rerank;

import java.util.List;

public record ContextCitation(
        Integer citationNo,
        Long docId,
        String docTitle,
        List<Long> chunkIds,
        Integer startPageNo,
        Integer endPageNo,
        String sectionTitle,
        Double score,
        String sourceUri
) {
}
