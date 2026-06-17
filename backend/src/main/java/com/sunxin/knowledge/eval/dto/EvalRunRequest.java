package com.sunxin.knowledge.eval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record EvalRunRequest(
        @JsonProperty("dataset_id")
        @NotNull
        Long datasetId,

        @JsonProperty("top_k")
        Integer topK
) {
}
