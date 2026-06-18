package com.sunxin.knowledge.integration.ai;

import java.util.List;

public record EmbeddingResponse(
        String model,
        int dimension,
        List<EmbeddingItem> embeddings
) {
}
