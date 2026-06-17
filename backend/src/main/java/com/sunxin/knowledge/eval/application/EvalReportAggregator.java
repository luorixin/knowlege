package com.sunxin.knowledge.eval.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.eval.dto.EvalCaseReportResponse;
import com.sunxin.knowledge.eval.dto.EvalMetricsResponse;

@Component
public class EvalReportAggregator {

    public EvalMetricsResponse aggregate(List<EvalCaseReportResponse> reports) {
        if (reports == null || reports.isEmpty()) {
            return new EvalMetricsResponse(0.0, 0.0, 0.0, 0.0, 0.0, 0);
        }

        long answerableCount = reports.stream()
                .filter(report -> !report.expectNoAnswer())
                .count();
        long noAnswerCaseCount = reports.stream()
                .filter(EvalCaseReportResponse::expectNoAnswer)
                .count();

        double recall = answerableCount == 0
                ? 0.0
                : reports.stream().filter(report -> !report.expectNoAnswer() && report.recallHit()).count()
                / (double) answerableCount;
        double precision = reports.stream()
                .mapToDouble(report -> report.recallHit() ? 1.0 : 0.0)
                .average()
                .orElse(0.0);
        double mrr = answerableCount == 0
                ? 0.0
                : reports.stream()
                .filter(report -> !report.expectNoAnswer())
                .mapToDouble(EvalCaseReportResponse::reciprocalRank)
                .average()
                .orElse(0.0);
        double citationAccuracy = answerableCount == 0
                ? 0.0
                : reports.stream()
                .filter(report -> !report.expectNoAnswer())
                .mapToDouble(EvalCaseReportResponse::citationAccuracy)
                .average()
                .orElse(0.0);
        double noAnswerAccuracy = noAnswerCaseCount == 0
                ? 0.0
                : reports.stream()
                .filter(EvalCaseReportResponse::expectNoAnswer)
                .filter(EvalCaseReportResponse::noAnswerCorrect)
                .count() / (double) noAnswerCaseCount;
        int permissionViolationCount = (int) reports.stream()
                .filter(EvalCaseReportResponse::permissionViolation)
                .count();

        return new EvalMetricsResponse(
                round(recall),
                round(precision),
                round(mrr),
                round(citationAccuracy),
                round(noAnswerAccuracy),
                permissionViolationCount
        );
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
