package com.sunxin.knowledge.retrieval.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;

@Service
public class DatabaseKeywordChunkSearchClient implements KeywordChunkSearchClient {

    private static final String ACTIVE = "ACTIVE";

    private final KbDocumentChunkRepository chunkRepository;

    public DatabaseKeywordChunkSearchClient(KbDocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Override
    public String engineName() {
        return "database-like";
    }

    @Override
    public List<ScoredChunk> search(String query, Collection<Long> allowedDocIds, int limit) {
        if (allowedDocIds == null || allowedDocIds.isEmpty() || limit <= 0) {
            return List.of();
        }

        Map<Long, ScoredChunk> results = new LinkedHashMap<>();
        List<String> terms = QueryText.keywordTerms(query);
        int perTermLimit = Math.max(limit, 20);
        for (String term : terms) {
            List<KbDocumentChunk> chunks = chunkRepository.searchByDocIdsAndContentLike(
                    allowedDocIds,
                    ACTIVE,
                    term,
                    PageRequest.of(0, perTermLimit)
            );
            for (KbDocumentChunk chunk : chunks) {
                double score = scoreKeywordHit(chunk.getContent(), term, terms.size());
                results.merge(
                        chunk.getId(),
                        new ScoredChunk(chunk, score),
                        (left, right) -> new ScoredChunk(left.chunk(), Math.max(left.score(), right.score()))
                );
            }
        }

        return results.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static double scoreKeywordHit(String content, String term, int termCount) {
        String normalizedContent = content == null ? "" : content.toLowerCase();
        String normalizedTerm = term.toLowerCase();
        int occurrences = 0;
        int index = normalizedContent.indexOf(normalizedTerm);
        while (index >= 0) {
            occurrences++;
            index = normalizedContent.indexOf(normalizedTerm, index + normalizedTerm.length());
        }
        double rarityBoost = termCount <= 1 ? 0.1 : 0.0;
        return Math.min(0.95, 0.55 + rarityBoost + Math.min(0.3, occurrences * 0.08));
    }
}
