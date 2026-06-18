package com.sunxin.knowledge.integration.ai;

import java.util.List;

public record EmbeddingItem(
        int index,
        List<Double> vector
) {
}
