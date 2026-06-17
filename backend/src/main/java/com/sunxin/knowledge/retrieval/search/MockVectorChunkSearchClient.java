package com.sunxin.knowledge.retrieval.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;

@Service
public class MockVectorChunkSearchClient implements VectorChunkSearchClient {

    private static final String ACTIVE = "ACTIVE";

    private final KbDocumentChunkRepository chunkRepository;

    public MockVectorChunkSearchClient(KbDocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Override
    public String storeName() {
        return "mock-vector-store";
    }

    @Override
    public List<ScoredChunk> search(String query, Collection<Long> allowedDocIds, int limit) {
        if (allowedDocIds == null || allowedDocIds.isEmpty() || limit <= 0) {
            return List.of();
        }

        Set<String> querySignals = QueryText.similaritySignals(query);
        if (querySignals.isEmpty()) {
            return List.of();
        }

        return chunkRepository.findByDocIdInAndStatus(allowedDocIds, ACTIVE).stream()
                .map(chunk -> new ScoredChunk(chunk, vectorScore(querySignals, chunk)))
                .filter(result -> result.score() > 0.0)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(limit)
                .toList();
    }

    private static double vectorScore(Set<String> querySignals, KbDocumentChunk chunk) {
        Set<String> contentSignals = QueryText.similaritySignals(chunk.getContent());
        if (contentSignals.isEmpty()) {
            return 0.0;
        }

        int intersection = 0;
        for (String signal : querySignals) {
            if (contentSignals.contains(signal)) {
                intersection++;
            }
        }
        if (intersection == 0) {
            return 0.0;
        }
        double recall = (double) intersection / querySignals.size();
        double precisionHint = (double) intersection / contentSignals.size();
        return Math.min(0.88, 0.35 + recall * 0.4 + precisionHint * 0.2);
    }
}
