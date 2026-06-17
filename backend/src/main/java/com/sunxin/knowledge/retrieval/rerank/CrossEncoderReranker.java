package com.sunxin.knowledge.retrieval.rerank;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service("crossEncoderReranker")
public class CrossEncoderReranker implements Reranker {

    @Override
    public String name() {
        return "cross-encoder-placeholder";
    }

    @Override
    public List<RerankedChunk> rerank(RerankRequest request) {
        AtomicInteger rank = new AtomicInteger(1);
        List<RerankCandidate> candidates = request == null || request.candidates() == null
                ? List.of()
                : request.candidates();
        return candidates.stream()
                .sorted(Comparator
                        .comparing((RerankCandidate candidate) -> safeScore(candidate.retrievalScore()))
                        .reversed())
                .map(candidate -> RerankedChunk.fromCandidate(rank.getAndIncrement(), candidate, safeScore(candidate.retrievalScore())))
                .toList();
    }

    private static double safeScore(Double value) {
        if (value == null || value.isNaN()) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
