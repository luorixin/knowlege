package com.sunxin.knowledge.retrieval.search;

import java.util.ArrayList;
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
    private static final String DELETED = "DELETED";
    private static final int MAX_DATABASE_TERMS = 3;

    private final KbDocumentChunkRepository chunkRepository;

    public DatabaseKeywordChunkSearchClient(KbDocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Override
    public String engineName() {
        return "database-like";
    }

    @Override
    public List<ScoredChunk> search(String query, ChunkSearchScope scope, int limit) {
        if (scope == null || limit <= 0) {
            return List.of();
        }

        Map<Long, ScoredChunk> results = new LinkedHashMap<>();
        List<String> terms = QueryText.keywordTerms(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        List<String> databaseTerms = terms.stream()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .limit(MAX_DATABASE_TERMS)
                .toList();
        List<KbDocumentChunk> chunks = chunkRepository.searchAccessibleChunksByAnyTerm(
                scope.tenantId(),
                scope.spaceId(),
                scope.userSubjectId(),
                scope.roleSubjects(),
                scope.spaceOwner(),
                scope.docType(),
                scope.industry(),
                scope.serviceLine(),
                scope.createdFrom(),
                ACTIVE,
                DELETED,
                ACTIVE,
                term(databaseTerms, 0),
                term(databaseTerms, 1),
                term(databaseTerms, 2),
                PageRequest.of(0, Math.max(limit * 3, 50))
        );
        for (KbDocumentChunk chunk : chunks) {
            double score = scoreKeywordHit(chunk.getContent(), terms);
            results.put(chunk.getId(), new ScoredChunk(chunk, score));
        }

        return results.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static String term(List<String> terms, int index) {
        return index < terms.size() ? terms.get(index) : null;
    }

    private static double scoreKeywordHit(String content, List<String> terms) {
        String normalizedContent = content == null ? "" : content.toLowerCase();
        int matchedTerms = 0;
        int occurrences = 0;
        for (String term : terms) {
            String normalizedTerm = term.toLowerCase();
            int termOccurrences = 0;
            int index = normalizedContent.indexOf(normalizedTerm);
            while (index >= 0) {
                termOccurrences++;
                index = normalizedContent.indexOf(normalizedTerm, index + normalizedTerm.length());
            }
            if (termOccurrences > 0) {
                matchedTerms++;
                occurrences += termOccurrences;
            }
        }
        if (matchedTerms == 0) {
            return 0.0;
        }
        double coverage = (double) matchedTerms / terms.size();
        double rarityBoost = terms.size() <= 1 ? 0.1 : 0.0;
        return Math.min(0.95, 0.45 + coverage * 0.28 + rarityBoost + Math.min(0.22, occurrences * 0.04));
    }
}
