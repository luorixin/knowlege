package com.sunxin.knowledge.eval.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.retrieval.dto.SearchFilters;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvalCaseCreateRequest(
        @JsonProperty("dataset_id")
        @NotNull
        Long datasetId,

        @NotBlank
        String question,

        @JsonProperty("case_type")
        String caseType,

        @JsonProperty("expected_answer")
        String expectedAnswer,

        @JsonProperty("expected_doc_ids")
        List<Long> expectedDocIds,

        @JsonProperty("expected_chunk_ids")
        List<Long> expectedChunkIds,

        @JsonProperty("expect_no_answer")
        Boolean expectNoAnswer,

        SearchFilters filters,

        List<String> tags
) {
}
