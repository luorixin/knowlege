package com.sunxin.knowledge.retrieval.search;

import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;

public record ScoredChunk(
        KbDocumentChunk chunk,
        double score
) {
}
