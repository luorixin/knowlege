package com.sunxin.knowledge.eval.application;

import com.sunxin.knowledge.eval.dto.EvalMetricsResponse;
import com.sunxin.knowledge.eval.dto.EvalRunResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CleaningImpactEvaluator {

    /**
     * Compares the evaluation metrics between a raw (uncleaned) baseline run and a cleaned experiment run.
     * Useful for proving the impact of data cleaning on RAG performance.
     */
    public CleaningImpactReport compare(EvalRunResponse rawRun, EvalRunResponse cleanedRun) {
        if (rawRun == null || cleanedRun == null) {
            throw new IllegalArgumentException("Both raw and cleaned runs must be provided");
        }

        EvalMetricsResponse rawMetrics = rawRun.metrics();
        EvalMetricsResponse cleanedMetrics = cleanedRun.metrics();

        return new CleaningImpactReport(
                rawRun.runId(),
                cleanedRun.runId(),
                cleanedMetrics.recallAtK() - rawMetrics.recallAtK(),
                cleanedMetrics.citationAccuracy() - rawMetrics.citationAccuracy(),
                cleanedMetrics.mrr() - rawMetrics.mrr(),
                cleanedMetrics.averageContextRelevance() - rawMetrics.averageContextRelevance(),
                cleanedMetrics.averageAnswerFaithfulness() - rawMetrics.averageAnswerFaithfulness()
        );
    }

    public record CleaningImpactReport(
            String rawRunId,
            String cleanedRunId,
            double recallRateDelta,
            double citationAccuracyDelta,
            double mrrDelta,
            double contextRelevanceDelta,
            double answerFaithfulnessDelta
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("raw_run_id", rawRunId);
            map.put("cleaned_run_id", cleanedRunId);
            map.put("recall_rate_delta", recallRateDelta);
            map.put("citation_accuracy_delta", citationAccuracyDelta);
            map.put("mrr_delta", mrrDelta);
            map.put("context_relevance_delta", contextRelevanceDelta);
            map.put("answer_faithfulness_delta", answerFaithfulnessDelta);
            map.put("is_positive_impact", recallRateDelta >= 0 && contextRelevanceDelta >= 0);
            return map;
        }
    }
}
