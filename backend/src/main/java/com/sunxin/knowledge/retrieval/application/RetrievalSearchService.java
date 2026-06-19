package com.sunxin.knowledge.retrieval.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.audit.AuditLogRecorder;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchRequest;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;
import com.sunxin.knowledge.retrieval.search.ChunkSearchScope;
import com.sunxin.knowledge.retrieval.search.KeywordChunkSearchClient;
import com.sunxin.knowledge.retrieval.search.ScoredChunk;
import com.sunxin.knowledge.retrieval.search.VectorChunkSearchClient;
import com.sunxin.knowledge.retrieval.security.DocumentAccessFilter;

@Service
public class RetrievalSearchService {

    private static final String ACTIVE = "ACTIVE";
    private static final int DEFAULT_TOP_K = 20;
    private static final int MAX_TOP_K = 100;

    private final KbSpaceRepository spaceRepository;
    private final KbDocumentRepository documentRepository;
    private final DocumentAccessFilter documentAccessFilter;
    private final KeywordChunkSearchClient keywordSearchClient;
    private final VectorChunkSearchClient vectorSearchClient;
    private final AuditLogRecorder auditLogRecorder;

    public RetrievalSearchService(
            KbSpaceRepository spaceRepository,
            KbDocumentRepository documentRepository,
            DocumentAccessFilter documentAccessFilter,
            KeywordChunkSearchClient keywordSearchClient,
            VectorChunkSearchClient vectorSearchClient,
            AuditLogRecorder auditLogRecorder
    ) {
        this.spaceRepository = spaceRepository;
        this.documentRepository = documentRepository;
        this.documentAccessFilter = documentAccessFilter;
        this.keywordSearchClient = keywordSearchClient;
        this.vectorSearchClient = vectorSearchClient;
        this.auditLogRecorder = auditLogRecorder;
    }

    @Transactional(readOnly = true)
    public RetrievalSearchResponse search(RetrievalSearchRequest request, CurrentUser user) {
        KbSpace space = spaceRepository.findByIdAndStatus(request.spaceId(), ACTIVE)
                .orElseThrow(() -> new NotFoundException("Knowledge space not found"));
        CurrentUser resolvedUser = resolveTenant(user, space);

        ChunkSearchScope searchScope = documentAccessFilter.searchScope(space, resolvedUser, request.filters());
        int topK = topK(request.topK());
        int candidateLimit = Math.min(MAX_TOP_K * 2, Math.max(topK * 3, topK));
        auditLogRecorder.record(
                resolvedUser,
                space.getTenantId(),
                "retrieval_search",
                "SPACE",
                space.getId(),
                AuditLogRecorder.SUCCESS,
                auditLogRecorder.detail("candidate_limit", candidateLimit)
        );

        List<String> allQueries = new java.util.ArrayList<>();
        allQueries.add(request.query());
        if (request.expandedQueries() != null) {
            allQueries.addAll(request.expandedQueries());
        }

        List<ScoredChunk> keywordResults = new java.util.ArrayList<>();
        List<ScoredChunk> vectorResults = new java.util.ArrayList<>();

        for (String q : allQueries) {
            keywordResults.addAll(keywordSearchClient.search(q, searchScope, candidateLimit));
            vectorResults.addAll(vectorSearchClient.search(q, searchScope, candidateLimit));
        }

        List<RetrievalCandidate> candidates = merge(keywordResults, vectorResults);
        if (candidates.isEmpty()) {
            return new RetrievalSearchResponse(List.of());
        }

        Map<Long, KbDocument> documentsById = documentsById(candidates);
        List<RetrievalSearchResult> results = candidates.stream()
                .sorted(Comparator
                        .comparingDouble(RetrievalCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.chunk().getDocId())
                        .thenComparing(candidate -> candidate.chunk().getChunkIndex()))
                .limit(topK)
                .map(candidate -> toResult(candidate, documentsById))
                .filter(result -> result != null)
                .toList();
        return new RetrievalSearchResponse(results);
    }

    private Map<Long, KbDocument> documentsById(List<RetrievalCandidate> candidates) {
        List<Long> docIds = candidates.stream()
                .map(candidate -> candidate.chunk().getDocId())
                .distinct()
                .toList();
        Map<Long, KbDocument> documentsById = new LinkedHashMap<>();
        documentRepository.findAllById(docIds)
                .forEach(document -> documentsById.put(document.getId(), document));
        return documentsById;
    }

    private static CurrentUser resolveTenant(CurrentUser user, KbSpace space) {
        Long userId = user == null || user.userId() == null ? 0L : user.userId();
        Long tenantId = user == null ? null : user.tenantId();
        if (tenantId != null && !tenantId.equals(space.getTenantId())) {
            throw new BadRequestException("X-Tenant-Id does not match requested knowledge space");
        }
        return new CurrentUser(userId, space.getTenantId(), user == null ? Set.of() : user.roleCodes());
    }

    private static int topK(Integer value) {
        if (value == null) {
            return DEFAULT_TOP_K;
        }
        return Math.min(value, MAX_TOP_K);
    }

    private static List<RetrievalCandidate> merge(
            List<ScoredChunk> keywordResults,
            List<ScoredChunk> vectorResults
    ) {
        Map<Long, RetrievalCandidate> merged = new LinkedHashMap<>();
        for (ScoredChunk result : keywordResults) {
            merged.put(result.chunk().getId(), new RetrievalCandidate(result.chunk(), result.score(), true, false));
        }
        for (ScoredChunk result : vectorResults) {
            merged.merge(
                    result.chunk().getId(),
                    new RetrievalCandidate(result.chunk(), result.score(), false, true),
                    RetrievalSearchService::mergeCandidate
            );
        }
        return new ArrayList<>(merged.values());
    }

    private static RetrievalCandidate mergeCandidate(RetrievalCandidate left, RetrievalCandidate right) {
        double score = Math.max(left.score(), right.score());
        boolean keywordHit = left.keywordHit() || right.keywordHit();
        boolean vectorHit = left.vectorHit() || right.vectorHit();
        if (keywordHit && vectorHit) {
            score = Math.min(0.99, score + 0.08);
        }
        return new RetrievalCandidate(left.chunk(), score, keywordHit, vectorHit);
    }

    private static RetrievalSearchResult toResult(
            RetrievalCandidate candidate,
            Map<Long, KbDocument> documentsById
    ) {
        KbDocument document = documentsById.get(candidate.chunk().getDocId());
        if (document == null) {
            return null;
        }
        KbDocumentChunk chunk = candidate.chunk();
        return new RetrievalSearchResult(
                chunk.getId(),
                document.getId(),
                document.getTitle(),
                chunk.getPageNo(),
                chunk.getSectionTitle(),
                chunk.getContent(),
                roundScore(candidate.score()),
                document.getSourceUri()
        );
    }

    private static double roundScore(double score) {
        return Math.round(score * 10_000.0) / 10_000.0;
    }

    private record RetrievalCandidate(
            KbDocumentChunk chunk,
            double score,
            boolean keywordHit,
            boolean vectorHit
    ) {
    }
}
