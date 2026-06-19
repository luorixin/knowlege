package com.sunxin.knowledge.eval.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbEvalCase;

public record EvalCaseResponse(
        Long id,
        @JsonProperty("dataset_id")
        Long datasetId,
        String question,
        @JsonProperty("expected_answer")
        String expectedAnswer,
        @JsonProperty("expected_doc_ids")
        List<Long> expectedDocIds,
        @JsonProperty("expected_chunk_ids")
        List<Long> expectedChunkIds,
        @JsonProperty("expect_no_answer")
        Boolean expectNoAnswer,
        @JsonProperty("case_type")
        String caseType,
        String status
) {

    public static EvalCaseResponse fromEntity(KbEvalCase evalCase, EvalCaseSpec spec) {
        return new EvalCaseResponse(
                evalCase.getId(),
                evalCase.getDatasetId(),
                evalCase.getQuestion(),
                evalCase.getExpectedAnswer(),
                spec.expectedDocIds(),
                spec.expectedChunkIds(),
                spec.expectNoAnswer(),
                evalCase.getCaseType(),
                evalCase.getStatus()
        );
    }
}
