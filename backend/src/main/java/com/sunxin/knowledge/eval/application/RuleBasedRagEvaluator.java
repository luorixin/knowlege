package com.sunxin.knowledge.eval.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.eval.dto.EvalCaseReportResponse;
import com.sunxin.knowledge.eval.dto.EvalCaseSpec;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.qa.dto.AgentCitationResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;

@Component
public class RuleBasedRagEvaluator {

    private static final String NO_EVIDENCE = "未在当前知识库中找到可靠依据";

    public EvalCaseReportResponse evaluate(
            Long caseId,
            String question,
            EvalCaseSpec spec,
            RetrievalSearchResponse retrieval,
            AgentChatResponse answer,
            int inaccessibleExpectedTargetCount,
            int unauthorizedCitationCount,
            int unauthorizedRetrievedCount
    ) {
        List<RetrievalSearchResult> retrievalResults = retrieval == null ? List.of() : retrieval.results();
        List<Long> retrievedChunkIds = retrievalResults.stream().map(RetrievalSearchResult::chunkId).toList();
        List<Long> retrievedDocIds = retrievalResults.stream().map(RetrievalSearchResult::docId).distinct().toList();
        List<AgentCitationResponse> citations = answer == null || answer.citations() == null
                ? List.of()
                : answer.citations();
        List<Long> citedDocIds = citations.stream().map(AgentCitationResponse::docId).distinct().toList();

        boolean recallHit = recallHit(spec, retrievedChunkIds, retrievedDocIds);
        double reciprocalRank = reciprocalRank(spec, retrievalResults);
        boolean expectNoAnswer = Boolean.TRUE.equals(spec.expectNoAnswer());
        boolean noAnswerCorrect = expectNoAnswer
                && retrievalResults.isEmpty()
                && answer != null
                && answer.answer() != null
                && answer.answer().contains(NO_EVIDENCE)
                && citations.isEmpty();
        double citationAccuracy = citationAccuracy(spec, citations, expectNoAnswer);
        boolean permissionViolation = unauthorizedCitationCount > 0 || unauthorizedRetrievedCount > 0;

        return new EvalCaseReportResponse(
                caseId,
                question,
                expectNoAnswer,
                answer == null ? null : answer.answer(),
                retrievedChunkIds,
                retrievedDocIds,
                citedDocIds,
                recallHit,
                citationAccuracy,
                noAnswerCorrect,
                inaccessibleExpectedTargetCount,
                unauthorizedCitationCount,
                unauthorizedRetrievedCount,
                permissionViolation,
                reciprocalRank,
                0.0,
                0.0,
                ""
        );
    }

    private static boolean recallHit(EvalCaseSpec spec, List<Long> retrievedChunkIds, List<Long> retrievedDocIds) {
        Set<Long> chunks = new HashSet<>(safe(spec.expectedChunkIds()));
        if (!chunks.isEmpty()) {
            return retrievedChunkIds.stream().anyMatch(chunks::contains);
        }
        Set<Long> docs = new HashSet<>(safe(spec.expectedDocIds()));
        return !docs.isEmpty() && retrievedDocIds.stream().anyMatch(docs::contains);
    }

    private static double reciprocalRank(EvalCaseSpec spec, List<RetrievalSearchResult> results) {
        if (Boolean.TRUE.equals(spec.expectNoAnswer())) {
            return 0.0;
        }
        Set<Long> expectedChunks = new HashSet<>(safe(spec.expectedChunkIds()));
        Set<Long> expectedDocs = new HashSet<>(safe(spec.expectedDocIds()));
        for (int index = 0; index < results.size(); index++) {
            RetrievalSearchResult result = results.get(index);
            boolean hit = !expectedChunks.isEmpty()
                    ? expectedChunks.contains(result.chunkId())
                    : expectedDocs.contains(result.docId());
            if (hit) {
                return 1.0 / (index + 1);
            }
        }
        return 0.0;
    }

    private static double citationAccuracy(
            EvalCaseSpec spec,
            List<AgentCitationResponse> citations,
            boolean expectNoAnswer
    ) {
        if (expectNoAnswer) {
            return 0.0;
        }
        Set<Long> expectedDocs = new HashSet<>(safe(spec.expectedDocIds()));
        if (expectedDocs.isEmpty()) {
            return citations.isEmpty() ? 0.0 : 1.0;
        }
        if (citations.isEmpty()) {
            return 0.0;
        }
        long hitCount = citations.stream()
                .map(AgentCitationResponse::docId)
                .filter(expectedDocs::contains)
                .count();
        return hitCount / (double) citations.size();
    }

    private static List<Long> safe(List<Long> values) {
        return values == null ? List.of() : values;
    }
}
