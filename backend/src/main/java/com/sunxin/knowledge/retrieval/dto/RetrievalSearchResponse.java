package com.sunxin.knowledge.retrieval.dto;

import java.util.List;

public record RetrievalSearchResponse(
        List<RetrievalSearchResult> results
) {
}
