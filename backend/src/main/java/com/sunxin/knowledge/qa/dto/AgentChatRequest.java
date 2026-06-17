package com.sunxin.knowledge.qa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.retrieval.dto.SearchFilters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AgentChatRequest(
        @JsonProperty("space_id")
        @NotNull
        Long spaceId,

        @JsonProperty("session_id")
        Long sessionId,

        @NotBlank
        String query,

        @Valid
        SearchFilters filters
) {
}
