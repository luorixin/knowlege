package com.sunxin.knowledge.audit.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbTokenUsage;

public record TokenUsageResponse(
        Long id,
        @JsonProperty("session_id")
        Long sessionId,
        @JsonProperty("usage_type")
        String usageType,
        @JsonProperty("model_provider")
        String modelProvider,
        @JsonProperty("model_name")
        String modelName,
        @JsonProperty("prompt_tokens")
        Integer promptTokens,
        @JsonProperty("completion_tokens")
        Integer completionTokens,
        @JsonProperty("total_tokens")
        Integer totalTokens,
        @JsonProperty("latency_ms")
        Long latencyMs,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static TokenUsageResponse fromEntity(KbTokenUsage usage) {
        return new TokenUsageResponse(
                usage.getId(),
                null, // View doesn't have session_id to unify Chat and Embedding
                usage.getUsageType(),
                usage.getModelProvider(),
                usage.getModelName(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                (usage.getPromptTokens() == null ? 0 : usage.getPromptTokens()) + 
                (usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens()),
                usage.getLatencyMs(),
                usage.getCreatedAt()
        );
    }
}
