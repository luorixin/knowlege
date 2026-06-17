package com.sunxin.knowledge.eval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvalDatasetCreateRequest(
        @JsonProperty("tenant_id")
        @NotNull
        Long tenantId,

        @JsonProperty("space_id")
        @NotNull
        Long spaceId,

        @NotBlank
        String name,

        String description
) {
}
