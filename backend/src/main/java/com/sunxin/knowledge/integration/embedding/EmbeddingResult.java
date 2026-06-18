package com.sunxin.knowledge.integration.embedding;

import java.util.List;

public record EmbeddingResult(
        String modelProvider,
        String modelName,
        int dimension,
        List<Double> vector
) {
}
