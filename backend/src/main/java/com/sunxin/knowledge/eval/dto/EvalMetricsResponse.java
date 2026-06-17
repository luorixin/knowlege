package com.sunxin.knowledge.eval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EvalMetricsResponse(
        @JsonProperty("recall_at_k")
        double recallAtK,
        @JsonProperty("precision_at_k")
        double precisionAtK,
        double mrr,
        @JsonProperty("citation_accuracy")
        double citationAccuracy,
        @JsonProperty("no_answer_accuracy")
        double noAnswerAccuracy,
        @JsonProperty("permission_violation_count")
        int permissionViolationCount
) {
}
