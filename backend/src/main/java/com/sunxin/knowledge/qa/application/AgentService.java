package com.sunxin.knowledge.qa.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

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
import com.sunxin.knowledge.qa.dto.AgentSessionDto;
import com.sunxin.knowledge.qa.dto.AgentMessageDto;
import com.sunxin.knowledge.common.dto.PageResponse;
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
    private final AnswerSafetyGuard answerSafetyGuard;
    private final AgentConversationRecorder conversationRecorder;
    private final AccessControlService accessControlService;
    private final org.springframework.core.task.TaskExecutor taskExecutor;

    public AgentService(
            KbSpaceRepository spaceRepository,
            KbDocumentRepository documentRepository,
            KbDocumentChunkRepository chunkRepository,
            QuestionUnderstandingService questionUnderstandingService,
            RetrievalSearchService retrievalSearchService,
            RerankService rerankService,
            ContextBuilderService contextBuilderService,
            LlmProvider llmProvider,
            AnswerSafetyGuard answerSafetyGuard,
            AgentConversationRecorder conversationRecorder,
            AccessControlService accessControlService,
            @Qualifier("applicationTaskExecutor")
            org.springframework.core.task.TaskExecutor taskExecutor
    ) {
        this.spaceRepository = spaceRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.questionUnderstandingService = questionUnderstandingService;
        this.retrievalSearchService = retrievalSearchService;
        this.rerankService = rerankService;
        this.contextBuilderService = contextBuilderService;
        this.llmProvider = llmProvider;
        this.answerSafetyGuard = answerSafetyGuard;
        this.conversationRecorder = conversationRecorder;
        this.accessControlService = accessControlService;
        this.taskExecutor = taskExecutor;
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentSessionDto> listSessions(Long spaceId, CurrentUser user, int page, int size) {
        KbSpace space = requireSpace(spaceId);
        CurrentUser resolvedUser = resolveUser(user, space);
        accessControlService.requireSpacePermission(space, resolvedUser, PermissionAction.AGENT_CHAT);

        Page<KbQuerySession> sessions = conversationRecorder.listSessions(space, resolvedUser, page, size);
        return new PageResponse<>(
                sessions.getContent().stream()
                        .map(s -> new AgentSessionDto(
                                s.getId(),
                                s.getSpaceId(),
                                s.getTitle(),
                                s.getStatus(),
                                s.getCreatedAt()
                        ))
                        .toList(),
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentMessageDto> getSessionMessages(Long spaceId, Long sessionId, CurrentUser user, int page, int size) {
        KbSpace space = requireSpace(spaceId);
        CurrentUser resolvedUser = resolveUser(user, space);
        accessControlService.requireSpacePermission(space, resolvedUser, PermissionAction.AGENT_CHAT);

        Page<KbQueryMessage> messages = conversationRecorder.getSessionMessages(sessionId, space, resolvedUser, page, size);
        List<Long> messageIds = messages.getContent().stream().map(com.sunxin.knowledge.persistence.entity.KbQueryMessage::getId).toList();
        Map<Long, List<AgentCitationResponse>> citationsByMessageId = conversationRecorder.getMessageCitations(messageIds);

        return new PageResponse<>(
                messages.getContent().stream()
                        .map(m -> new AgentMessageDto(
                                m.getId(),
                                m.getRole(),
                                m.getContent(),
                                m.getCreatedAt(),
                                citationsByMessageId.getOrDefault(m.getId(), List.of()),
                                "ERROR".equalsIgnoreCase(m.getStatus())
                        ))
                        .toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages()
        );
    }

    @Transactional
    public AgentChatResponse chat(AgentChatRequest request, CurrentUser user) {
        KbSpace space = requireSpace(request.spaceId());
        CurrentUser resolvedUser = resolveUser(user, space);
        accessControlService.requireSpacePermission(space, resolvedUser, PermissionAction.AGENT_CHAT);
        KbQuerySession session = conversationRecorder.resolveSession(request, space, resolvedUser);
        List<com.sunxin.knowledge.qa.llm.ChatMessage> history = conversationRecorder.getHistoryMessages(session.getId(), 10);
        QuestionIntent intent = questionUnderstandingService.understand(request, history);

        RetrievalSearchResponse retrieval = retrieve(request, intent, resolvedUser);
        CandidateBundle bundle = enrichCandidates(retrieval.results());
        List<RerankedChunk> reranked = rerank(intent, bundle.candidates());
        ContextBuildResult context = contextBuilderService.build(new ContextBuildRequest(reranked, MAX_CONTEXT_CHARS));
        
        LlmResponse llmResponse;
        if (context.context() == null || context.context().isBlank()) {
            llmResponse = new LlmResponse("未在当前知识库中找到可靠依据。", llmProvider.provider(), llmProvider.modelName(), 0, 0, 0L);
        } else {
            llmResponse = llmProvider.generate(new LlmRequest(
                    intent.query(),
                    context.context(),
                    context.citations(),
                    history
            ));
            llmResponse = answerSafetyGuard.guard(llmResponse, context.citations());
        }

        KbQueryMessage userMessage = conversationRecorder.saveUserMessage(session, intent.query());
        KbQueryMessage assistantMessage = conversationRecorder.saveAssistantMessage(
                session,
                userMessage.getId(),
                llmResponse
        );
        conversationRecorder.saveCitations(session, assistantMessage, context.citations(), bundle.chunksById());

        Map<String, Object> debugInfo = buildDebugInfo(resolvedUser, retrieval, reranked, context);

        return new AgentChatResponse(
                session.getId(),
                llmResponse.answer(),
                toCitationResponses(context.citations(), bundle.chunksById()),
                debugInfo
        );
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(AgentChatRequest request, CurrentUser user) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(600000L); // 10 minutes timeout
        
        taskExecutor.execute(() -> {
            try {
                KbSpace space = requireSpace(request.spaceId());
                CurrentUser resolvedUser = resolveUser(user, space);
                accessControlService.requireSpacePermission(space, resolvedUser, PermissionAction.AGENT_CHAT);
                KbQuerySession session = conversationRecorder.resolveSession(request, space, resolvedUser);
                List<com.sunxin.knowledge.qa.llm.ChatMessage> history = conversationRecorder.getHistoryMessages(session.getId(), 10);
                QuestionIntent intent = questionUnderstandingService.understand(request, history);

                RetrievalSearchResponse retrieval = retrieve(request, intent, resolvedUser);
                CandidateBundle bundle = enrichCandidates(retrieval.results());
                List<RerankedChunk> reranked = rerank(intent, bundle.candidates());
                ContextBuildResult context = contextBuilderService.build(new ContextBuildRequest(reranked, MAX_CONTEXT_CHARS));
                
                KbQueryMessage userMessage = conversationRecorder.saveUserMessage(session, intent.query());
                Map<String, Object> debugInfo = buildDebugInfo(resolvedUser, retrieval, reranked, context);
                List<AgentCitationResponse> citationResponses = toCitationResponses(context.citations(), bundle.chunksById());

                if (context.context() == null || context.context().isBlank()) {
                    String answer = "未在当前知识库中找到可靠依据。";
                    LlmResponse llmResponse = new LlmResponse(answer, llmProvider.provider(), llmProvider.modelName(), 0, 0, 0L);
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("message").data(answer));
                    
                    AgentChatResponse finalResponse = new AgentChatResponse(session.getId(), answer, citationResponses, debugInfo);
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("done").data(finalResponse));
                    
                    KbQueryMessage assistantMessage = conversationRecorder.saveAssistantMessage(session, userMessage.getId(), llmResponse);
                    conversationRecorder.saveCitations(session, assistantMessage, context.citations(), bundle.chunksById());
                    emitter.complete();
                    return;
                }

                LlmRequest llmRequest = new LlmRequest(intent.query(), context.context(), context.citations(), history);
                llmProvider.stream(llmRequest,
                    chunk -> {
                        try {
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("message").data(chunk));
                        } catch (Exception e) {
                            // Ignore broken pipe
                        }
                    },
                    response -> {
                        try {
                            LlmResponse guardedResponse = answerSafetyGuard.guard(response, context.citations());
                            AgentChatResponse finalResponse = new AgentChatResponse(session.getId(), guardedResponse.answer(), citationResponses, debugInfo);
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("done").data(finalResponse));
                            
                            KbQueryMessage assistantMessage = conversationRecorder.saveAssistantMessage(session, userMessage.getId(), guardedResponse);
                            conversationRecorder.saveCitations(session, assistantMessage, context.citations(), bundle.chunksById());
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        emitter.completeWithError(error);
                    }
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private Map<String, Object> buildDebugInfo(CurrentUser resolvedUser, RetrievalSearchResponse retrieval, List<RerankedChunk> reranked, ContextBuildResult context) {
        if (resolvedUser.roleCodes().contains(com.sunxin.knowledge.auth.SystemRoleConst.ADMIN) || resolvedUser.roleCodes().contains("admin") || resolvedUser.roleCodes().contains(com.sunxin.knowledge.auth.SystemRoleConst.KNOWLEDGE_ADMIN)) {
            return Map.of(
                    "retrieval_results", retrieval.results(),
                    "reranked_chunks", reranked,
                    "final_context", context.context()
            );
        }
        return null;
    }

    private RetrievalSearchResponse retrieve(
            AgentChatRequest request,
            QuestionIntent intent,
            CurrentUser resolvedUser
    ) {
        return retrievalSearchService.search(new RetrievalSearchRequest(
                intent.query(),
                request.spaceId(),
                intent.filters(),
                RETRIEVAL_TOP_K,
                intent.expandedQueries()
        ), resolvedUser);
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
