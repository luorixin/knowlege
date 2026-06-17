package com.sunxin.knowledge.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RetrievalSearchRequest(
        @NotBlank
        String query,

        @JsonProperty("space_id")
        @NotNull
        Long spaceId,

        @Valid
        SearchFilters filters,

        @JsonProperty("top_k")
        @Min(1)
        Integer topK
) {
}
