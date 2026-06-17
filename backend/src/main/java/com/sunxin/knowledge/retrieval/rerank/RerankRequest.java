package com.sunxin.knowledge.retrieval.rerank;

import java.util.List;

import com.sunxin.knowledge.retrieval.dto.SearchFilters;

public record RerankRequest(
        String query,
        SearchFilters filters,
        List<RerankCandidate> candidates,
        Integer topK,
        Integer maxChunksPerDocument
) {
}
