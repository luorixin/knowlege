package com.sunxin.knowledge.qa.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbQueryMessage;
import com.sunxin.knowledge.persistence.entity.KbQuerySession;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.qa.dto.AgentChatRequest;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.qa.dto.AgentCitationResponse;
import com.sunxin.knowledge.qa.llm.LlmProvider;
import com.sunxin.knowledge.qa.llm.LlmRequest;
import com.sunxin.knowledge.qa.llm.LlmResponse;
import com.sunxin.knowledge.retrieval.application.RetrievalSearchService;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchRequest;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;
import com.sunxin.knowledge.retrieval.rerank.ContextBuildRequest;
import com.sunxin.knowledge.retrieval.rerank.ContextBuildResult;
import com.sunxin.knowledge.retrieval.rerank.ContextBuilderService;
import com.sunxin.knowledge.retrieval.rerank.ContextCitation;
import com.sunxin.knowledge.retrieval.rerank.RerankCandidate;
import com.sunxin.knowledge.retrieval.rerank.RerankRequest;
import com.sunxin.knowledge.retrieval.rerank.RerankService;
import com.sunxin.knowledge.retrieval.rerank.RerankedChunk;

@Service
public class AgentService {

    private static final String ACTIVE = "ACTIVE";
    private static final int RETRIEVAL_TOP_K = 20;
    private static final int RERANK_TOP_K = 8;
    private static final int MAX_CHUNKS_PER_DOCUMENT = 3;
    private static final int MAX_CONTEXT_CHARS = 8_000;

    private final KbSpaceRepository spaceRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentChunkRepository chunkRepository;
    private final QuestionUnderstandingService questionUnderstandingService;
    private final RetrievalSearchService retrievalSearchService;
    private final RerankService rerankService;
    private final ContextBuilderService contextBuilderService;
    private final LlmProvider llmProvider;
    private final AgentConversationRecorder conversationRecorder;
    private final AccessControlService accessControlService;

    public AgentService(
            KbSpaceRepository spaceRepository,
            KbDocumentRepository documentRepository,
            KbDocumentChunkRepository chunkRepository,
            QuestionUnderstandingService questionUnderstandingService,
            RetrievalSearchService retrievalSearchService,
            RerankService rerankService,
            ContextBuilderService contextBuilderService,
            LlmProvider llmProvider,
            AgentConversationRecorder conversationRecorder,
            AccessControlService accessControlService
    ) {
        this.spaceRepository = spaceRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.questionUnderstandingService = questionUnderstandingService;
        this.retrievalSearchService = retrievalSearchService;
        this.rerankService = rerankService;
        this.contextBuilderService = contextBuilderService;
        this.llmProvider = llmProvider;
        this.conversationRecorder = conversationRecorder;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public AgentChatResponse chat(AgentChatRequest request, CurrentUser user) {
        KbSpace space = requireSpace(request.spaceId());
        CurrentUser resolvedUser = resolveUser(user, space);
        accessControlService.requireSpacePermission(space, resolvedUser, PermissionAction.AGENT_CHAT);
        QuestionIntent intent = questionUnderstandingService.understand(request);
        KbQuerySession session = conversationRecorder.resolveSession(request, space, resolvedUser);

        RetrievalSearchResponse retrieval = retrieve(request, intent, resolvedUser);
        CandidateBundle bundle = enrichCandidates(retrieval.results());
        List<RerankedChunk> reranked = rerank(intent, bundle.candidates());
        ContextBuildResult context = contextBuilderService.build(new ContextBuildRequest(reranked, MAX_CONTEXT_CHARS));
        
        LlmResponse llmResponse;
        if (context.context() == null || context.context().isBlank()) {
            llmResponse = new LlmResponse("未在当前知识库中找到可靠依据。");
        } else {
            llmResponse = llmProvider.generate(new LlmRequest(
                    intent.query(),
                    context.context(),
                    context.citations()
            ));
        }

        KbQueryMessage userMessage = conversationRecorder.saveUserMessage(session, intent.query());
        KbQueryMessage assistantMessage = conversationRecorder.saveAssistantMessage(
                session,
                userMessage.getId(),
                llmResponse
        );
        conversationRecorder.saveCitations(session, assistantMessage, context.citations(), bundle.chunksById());

        Map<String, Object> debugInfo = null;
        if (resolvedUser.roleCodes().contains("ADMIN") || resolvedUser.roleCodes().contains("admin") || resolvedUser.roleCodes().contains("KNOWLEDGE_ADMIN")) {
            debugInfo = Map.of(
                    "retrieval_results", retrieval.results(),
                    "reranked_chunks", reranked,
                    "final_context", context.context()
            );
        }

        return new AgentChatResponse(
                session.getId(),
                llmResponse.answer(),
                toCitationResponses(context.citations(), bundle.chunksById()),
                debugInfo
        );
    }

    private RetrievalSearchResponse retrieve(
            AgentChatRequest request,
            QuestionIntent intent,
            CurrentUser user
    ) {
        return retrievalSearchService.search(
                new RetrievalSearchRequest(
                        intent.query(),
                        request.spaceId(),
                        intent.filters(),
                        RETRIEVAL_TOP_K
                ),
                user
        );
    }

    private List<RerankedChunk> rerank(QuestionIntent intent, List<RerankCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        return rerankService.rerank(new RerankRequest(
                intent.query(),
                intent.filters(),
                candidates,
                RERANK_TOP_K,
                MAX_CHUNKS_PER_DOCUMENT
        ));
    }

    private CandidateBundle enrichCandidates(List<RetrievalSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return new CandidateBundle(List.of(), Map.of());
        }

        List<Long> docIds = results.stream().map(RetrievalSearchResult::docId).distinct().toList();
        List<Long> chunkIds = results.stream().map(RetrievalSearchResult::chunkId).distinct().toList();
        Map<Long, KbDocument> documentsById = documentRepository.findAllById(docIds).stream()
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));
        Map<Long, KbDocumentChunk> chunksById = chunkRepository.findAllById(chunkIds).stream()
                .collect(Collectors.toMap(KbDocumentChunk::getId, Function.identity()));

        List<RerankCandidate> candidates = results.stream()
                .map(result -> toRerankCandidate(result, documentsById, chunksById))
                .filter(candidate -> candidate != null)
                .toList();
        return new CandidateBundle(candidates, chunksById);
    }

    private static RerankCandidate toRerankCandidate(
            RetrievalSearchResult result,
            Map<Long, KbDocument> documentsById,
            Map<Long, KbDocumentChunk> chunksById
    ) {
        KbDocument document = documentsById.get(result.docId());
        KbDocumentChunk chunk = chunksById.get(result.chunkId());
        if (document == null || chunk == null) {
            return null;
        }
        return new RerankCandidate(
                result.chunkId(),
                result.docId(),
                result.docTitle(),
                document.getDocType(),
                document.getIndustry(),
                document.getServiceLine(),
                document.getCreatedAt(),
                chunk.getChunkIndex(),
                result.pageNo(),
                result.sectionTitle(),
                result.content(),
                result.score(),
                result.sourceUri()
        );
    }

    private static List<AgentCitationResponse> toCitationResponses(
            List<ContextCitation> citations,
            Map<Long, KbDocumentChunk> chunksById
    ) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }
        return citations.stream()
                .sorted(Comparator.comparing(ContextCitation::citationNo))
                .map(citation -> {
                    String chunkContent = citation.chunkIds().stream()
                            .map(chunksById::get)
                            .filter(c -> c != null)
                            .map(KbDocumentChunk::getContent)
                            .collect(Collectors.joining("\n\n"));
                    return new AgentCitationResponse(
                            citation.citationNo(),
                            citation.docId(),
                            citation.docTitle(),
                            citation.startPageNo(),
                            citation.sectionTitle(),
                            chunkContent,
                            citation.sourceUri()
                    );
                })
                .toList();
    }

    private KbSpace requireSpace(Long spaceId) {
        return spaceRepository.findByIdAndStatus(spaceId, ACTIVE)
                .orElseThrow(() -> new NotFoundException("Knowledge space not found"));
    }

    private static CurrentUser resolveUser(CurrentUser user, KbSpace space) {
        Long userId = user == null || user.userId() == null ? 0L : user.userId();
        Long tenantId = user == null ? null : user.tenantId();
        if (tenantId != null && !tenantId.equals(space.getTenantId())) {
            throw new BadRequestException("X-Tenant-Id does not match requested knowledge space");
        }
        return new CurrentUser(userId, space.getTenantId(), user == null ? Set.of() : user.roleCodes());
    }

    private record CandidateBundle(
            List<RerankCandidate> candidates,
            Map<Long, KbDocumentChunk> chunksById
    ) {
    }
}
