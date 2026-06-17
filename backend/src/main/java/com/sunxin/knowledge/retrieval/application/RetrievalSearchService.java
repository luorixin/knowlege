package com.sunxin.knowledge.retrieval.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.audit.AuditLogRecorder;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchRequest;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;
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
    private final DocumentAccessFilter documentAccessFilter;
    private final KeywordChunkSearchClient keywordSearchClient;
    private final VectorChunkSearchClient vectorSearchClient;
    private final AuditLogRecorder auditLogRecorder;

    public RetrievalSearchService(
            KbSpaceRepository spaceRepository,
            DocumentAccessFilter documentAccessFilter,
            KeywordChunkSearchClient keywordSearchClient,
            VectorChunkSearchClient vectorSearchClient,
            AuditLogRecorder auditLogRecorder
    ) {
        this.spaceRepository = spaceRepository;
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

        List<KbDocument> allowedDocuments = documentAccessFilter.accessibleDocuments(
                space,
                resolvedUser,
                request.filters()
        );
        auditLogRecorder.record(
                resolvedUser,
                space.getTenantId(),
                "retrieval_search",
                "SPACE",
                space.getId(),
                AuditLogRecorder.SUCCESS,
                auditLogRecorder.detail("allowed_doc_count", allowedDocuments.size())
        );
        if (allowedDocuments.isEmpty()) {
            return new RetrievalSearchResponse(List.of());
        }

        Set<Long> allowedDocIds = allowedDocuments.stream()
                .map(KbDocument::getId)
                .collect(Collectors.toSet());
        Map<Long, KbDocument> documentsById = allowedDocuments.stream()
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));

        int topK = topK(request.topK());
        int candidateLimit = Math.min(MAX_TOP_K * 2, Math.max(topK * 3, topK));
        List<ScoredChunk> keywordResults = keywordSearchClient.search(request.query(), allowedDocIds, candidateLimit);
        List<ScoredChunk> vectorResults = vectorSearchClient.search(request.query(), allowedDocIds, candidateLimit);

        List<RetrievalCandidate> candidates = merge(keywordResults, vectorResults);
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
