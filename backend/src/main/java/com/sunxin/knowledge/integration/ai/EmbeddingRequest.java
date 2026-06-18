package com.sunxin.knowledge.integration.ai;

import java.util.List;

public record EmbeddingRequest(
        List<String> texts,
        String model
) {
}
