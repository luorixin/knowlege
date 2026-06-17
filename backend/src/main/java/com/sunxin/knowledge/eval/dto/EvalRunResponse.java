package com.sunxin.knowledge.eval.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EvalRunResponse(
        @JsonProperty("run_id")
        String runId,
        @JsonProperty("dataset_id")
        Long datasetId,
        @JsonProperty("case_count")
        int caseCount,
        EvalMetricsResponse metrics,
        List<EvalCaseReportResponse> cases
) {
}
