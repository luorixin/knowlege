package com.sunxin.knowledge.retrieval.rerank;

import java.util.List;

public record ContextBuildResult(
        String context,
        List<ContextCitation> citations
) {
}
