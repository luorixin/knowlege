package com.sunxin.knowledge.qa.llm;

public record LlmResponse(
        String answer,
        String provider,
        String modelName,
        Integer promptTokens,
        Integer completionTokens,
        Long latencyMs
) {
    public LlmResponse(String answer) {
        this(answer, "system", "none", 0, 0, 0L);
    }
}
