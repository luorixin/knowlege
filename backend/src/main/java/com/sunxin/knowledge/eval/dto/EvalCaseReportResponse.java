package com.sunxin.knowledge.eval.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EvalCaseReportResponse(
        @JsonProperty("case_id")
        Long caseId,
        String question,
        @JsonProperty("expect_no_answer")
        boolean expectNoAnswer,
        @JsonProperty("actual_answer")
        String actualAnswer,
        @JsonProperty("retrieved_chunk_ids")
        List<Long> retrievedChunkIds,
        @JsonProperty("retrieved_doc_ids")
        List<Long> retrievedDocIds,
        @JsonProperty("cited_doc_ids")
        List<Long> citedDocIds,
        @JsonProperty("recall_hit")
        boolean recallHit,
        @JsonProperty("citation_accuracy")
        double citationAccuracy,
        @JsonProperty("no_answer_correct")
        boolean noAnswerCorrect,
        @JsonProperty("inaccessible_expected_target_count")
        int inaccessibleExpectedTargetCount,
        @JsonProperty("unauthorized_citation_count")
        int unauthorizedCitationCount,
        @JsonProperty("unauthorized_retrieved_count")
        int unauthorizedRetrievedCount,
        @JsonProperty("permission_violation")
        boolean permissionViolation,
        @JsonProperty("reciprocal_rank")
        double reciprocalRank
) {
}
