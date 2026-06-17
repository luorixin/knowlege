package com.sunxin.knowledge.retrieval.rerank;

import java.util.List;

public record ContextBuildRequest(
        List<RerankedChunk> chunks,
        Integer maxContextChars
) {
}
