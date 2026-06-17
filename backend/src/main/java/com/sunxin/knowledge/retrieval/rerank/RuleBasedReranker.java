package com.sunxin.knowledge.retrieval.rerank;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.retrieval.dto.SearchFilters;

@Service("ruleBasedReranker")
public class RuleBasedReranker implements Reranker {

    private static final double RETRIEVAL_WEIGHT = 0.4;
    private static final double KEYWORD_HIT_BONUS = 0.12;
    private static final double DOC_TYPE_BONUS = 0.12;
    private static final double INDUSTRY_BONUS = 0.10;
    private static final double SERVICE_LINE_BONUS = 0.10;

    @Override
    public String name() {
        return "rule-based";
    }

    @Override
    public List<RerankedChunk> rerank(RerankRequest request) {
        List<RerankCandidate> candidates = request == null || request.candidates() == null
                ? List.of()
                : request.candidates();
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<String> queryTerms = queryTerms(request.query());
        LocalDateTime latestDocumentTime = candidates.stream()
                .map(RerankCandidate::documentCreatedAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<RerankedChunk> sorted = candidates.stream()
                .map(candidate -> RerankedChunk.fromCandidate(0, candidate, score(
                        candidate,
                        request.filters(),
                        queryTerms,
                        latestDocumentTime
                )))
                .sorted(Comparator
                        .comparingDouble(RerankedChunk::rerankScore).reversed()
                        .thenComparing(chunk -> nullToMax(chunk.docId()))
                        .thenComparing(chunk -> nullToMax(chunk.chunkIndex())))
                .toList();

        List<RerankedChunk> ranked = new ArrayList<>();
        for (RerankedChunk chunk : sorted) {
            ranked.add(chunk.withRank(ranked.size() + 1));
        }
        return ranked;
    }

    private static double score(
            RerankCandidate candidate,
            SearchFilters filters,
            List<String> queryTerms,
            LocalDateTime latestDocumentTime
    ) {
        double score = safeScore(candidate.retrievalScore()) * RETRIEVAL_WEIGHT;
        score += keywordScore(candidate.content(), queryTerms);
        score += metadataScore(candidate, filters);
        score += freshnessScore(candidate.documentCreatedAt(), latestDocumentTime);
        return round(Math.min(1.0, score));
    }

    private static double keywordScore(String content, List<String> queryTerms) {
        if (content == null || content.isBlank() || queryTerms.isEmpty()) {
            return 0.0;
        }
        String normalizedContent = normalize(content);
        int hits = 0;
        for (String term : queryTerms) {
            if (normalizedContent.contains(term)) {
                hits++;
            }
        }
        return Math.min(0.36, hits * KEYWORD_HIT_BONUS);
    }

    private static double metadataScore(RerankCandidate candidate, SearchFilters filters) {
        if (filters == null) {
            return 0.0;
        }
        double score = 0.0;
        if (equalsIfPresent(candidate.docType(), filters.docType())) {
            score += DOC_TYPE_BONUS;
        }
        if (equalsIfPresent(candidate.industry(), filters.industry())) {
            score += INDUSTRY_BONUS;
        }
        if (equalsIfPresent(candidate.serviceLine(), filters.serviceLine())) {
            score += SERVICE_LINE_BONUS;
        }
        return score;
    }

    private static double freshnessScore(LocalDateTime documentCreatedAt, LocalDateTime latestDocumentTime) {
        if (documentCreatedAt == null || latestDocumentTime == null) {
            return 0.0;
        }
        long months = Math.max(0, ChronoUnit.MONTHS.between(documentCreatedAt, latestDocumentTime));
        if (months <= 6) {
            return 0.10;
        }
        if (months <= 24) {
            return 0.08;
        }
        if (months <= 48) {
            return 0.04;
        }
        return 0.0;
    }

    private static boolean equalsIfPresent(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return actual != null && normalize(actual).equals(normalize(expected));
    }

    private static List<String> queryTerms(String value) {
        Set<String> terms = new LinkedHashSet<>();
        for (String token : normalize(value).split("[\\s,，。；;：:、?？!！()（）\\[\\]{}<>《》\"']+")) {
            if (token.length() >= 2) {
                terms.add(token);
            }
        }
        if (terms.isEmpty() && value != null && !value.isBlank()) {
            terms.add(normalize(value));
        }
        return new ArrayList<>(terms);
    }

    private static double safeScore(Double value) {
        if (value == null || value.isNaN()) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static long nullToMax(Long value) {
        return value == null ? Long.MAX_VALUE : value;
    }

    private static int nullToMax(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
