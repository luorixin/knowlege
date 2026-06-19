package com.sunxin.knowledge.qa.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.auth.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.persistence.entity.KbAnswerCitation;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbQueryMessage;
import com.sunxin.knowledge.persistence.entity.KbQuerySession;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbAnswerCitationRepository;
import com.sunxin.knowledge.persistence.repository.KbQueryMessageRepository;
import com.sunxin.knowledge.persistence.repository.KbQuerySessionRepository;
import com.sunxin.knowledge.qa.dto.AgentChatRequest;
import com.sunxin.knowledge.qa.llm.LlmResponse;
import com.sunxin.knowledge.retrieval.rerank.ContextCitation;
import com.sunxin.knowledge.qa.dto.AgentCitationResponse;

@Service
public class AgentConversationRecorder {

    private static final String ACTIVE = "ACTIVE";
    private static final String WEB = "WEB";
    private static final String USER = "USER";
    private static final String ASSISTANT = "ASSISTANT";
    private static final String SUCCESS = "SUCCESS";

    private final KbQuerySessionRepository sessionRepository;
    private final KbQueryMessageRepository messageRepository;
    private final KbAnswerCitationRepository citationRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public AgentConversationRecorder(
            KbQuerySessionRepository sessionRepository,
            KbQueryMessageRepository messageRepository,
            KbAnswerCitationRepository citationRepository,
            IdGenerator idGenerator,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public KbQuerySession resolveSession(AgentChatRequest request, KbSpace space, CurrentUser user) {
        if (request.sessionId() != null) {
            KbQuerySession session = sessionRepository.findByIdAndTenantIdAndUserIdAndStatus(
                            request.sessionId(),
                            space.getTenantId(),
                            user.userId(),
                            ACTIVE
                    )
                    .orElseThrow(() -> new NotFoundException("Query session not found"));
            if (session.getSpaceId() != null && !session.getSpaceId().equals(space.getId())) {
                throw new BadRequestException("Query session does not belong to requested knowledge space");
            }
            return session;
        }

        KbQuerySession session = new KbQuerySession();
        session.setId(idGenerator.nextId());
        session.setTenantId(space.getTenantId());
        session.setSpaceId(space.getId());
        session.setUserId(user.userId());
        session.setTitle(title(request.query()));
        session.setChannel(WEB);
        session.setStatus(ACTIVE);
        session.setMetadataJson(toJson(Map.of("agent", "knowledge-agent-mvp")));
        return sessionRepository.save(session);
    }

    public List<com.sunxin.knowledge.qa.llm.ChatMessage> getHistoryMessages(Long sessionId, int limit) {
        if (sessionId == null) return List.of();
        // Fetch the last `limit` messages by ordering by createdAt descending
        List<KbQueryMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtDesc(
                sessionId, PageRequest.of(0, limit)
        ).getContent();
        // We need to return them in chronological order, so reverse the list
        List<KbQueryMessage> reversed = new ArrayList<>(messages);
        java.util.Collections.reverse(reversed);
        
        return reversed.stream()
                .map(m -> new com.sunxin.knowledge.qa.llm.ChatMessage(m.getRole().toLowerCase(), m.getContent()))
                .toList();
    }

    public Page<KbQuerySession> listSessions(KbSpace space, CurrentUser user, int page, int size) {
        return sessionRepository.findByTenantIdAndSpaceIdAndUserIdAndStatusOrderByCreatedAtDesc(
                space.getTenantId(),
                space.getId(),
                user.userId(),
                ACTIVE,
                PageRequest.of(page, size)
        );
    }

    public Page<KbQueryMessage> getSessionMessages(Long sessionId, KbSpace space, CurrentUser user, int page, int size) {
        KbQuerySession session = sessionRepository.findByIdAndTenantIdAndUserIdAndStatus(
                        sessionId,
                        space.getTenantId(),
                        user.userId(),
                        ACTIVE
                )
                .orElseThrow(() -> new NotFoundException("Query session not found"));
        if (session.getSpaceId() != null && !session.getSpaceId().equals(space.getId())) {
            throw new BadRequestException("Query session does not belong to requested knowledge space");
        }
        return messageRepository.findBySessionIdOrderByCreatedAt(sessionId, PageRequest.of(page, size));
    }

    public List<AgentCitationResponse> getMessageCitations(Long messageId) {
        return getMessageCitations(List.of(messageId)).getOrDefault(messageId, List.of());
    }

    public Map<Long, List<AgentCitationResponse>> getMessageCitations(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        
        List<KbAnswerCitation> citations = citationRepository.findByMessageIdIn(messageIds);
        Map<Long, List<AgentCitationResponse>> result = new LinkedHashMap<>();
        
        for (KbAnswerCitation c : citations) {
            String docTitle = "";
            String sourceUri = null;
            if (c.getMetadataJson() != null) {
                try {
                    Map<String, Object> map = objectMapper.readValue(c.getMetadataJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    if (map.containsKey("doc_title")) docTitle = (String) map.get("doc_title");
                    if (map.containsKey("source_uri")) sourceUri = (String) map.get("source_uri");
                } catch (Exception ignore) {}
            }
            AgentCitationResponse response = new AgentCitationResponse(
                    c.getRankNo() != null ? c.getRankNo() : 0,
                    c.getDocId(),
                    docTitle,
                    c.getPageNo(),
                    c.getSectionTitle(),
                    c.getQuoteText(),
                    sourceUri
            );
            result.computeIfAbsent(c.getMessageId(), k -> new ArrayList<>()).add(response);
        }
        
        // sort each list by rankNo
        result.values().forEach(list -> list.sort(java.util.Comparator.comparing(AgentCitationResponse::citationId)));
        
        return result;
    }

    public KbQueryMessage saveUserMessage(KbQuerySession session, String query) {
        KbQueryMessage message = new KbQueryMessage();
        message.setId(idGenerator.nextId());
        message.setTenantId(session.getTenantId());
        message.setSessionId(session.getId());
        message.setRole(USER);
        message.setContent(query);
        message.setStatus(SUCCESS);
        return messageRepository.save(message);
    }

    public KbQueryMessage saveAssistantMessage(
            KbQuerySession session,
            Long parentMessageId,
            LlmResponse llmResponse
    ) {
        KbQueryMessage message = new KbQueryMessage();
        message.setId(idGenerator.nextId());
        message.setTenantId(session.getTenantId());
        message.setSessionId(session.getId());
        message.setParentMessageId(parentMessageId);
        message.setRole(ASSISTANT);
        message.setContent(llmResponse.answer());
        message.setModelProvider(llmResponse.provider());
        message.setModelName(llmResponse.modelName());
        message.setPromptTokens(llmResponse.promptTokens());
        message.setCompletionTokens(llmResponse.completionTokens());
        message.setLatencyMs(llmResponse.latencyMs());
        message.setStatus(SUCCESS);
        return messageRepository.save(message);
    }

    public List<KbAnswerCitation> saveCitations(
            KbQuerySession session,
            KbQueryMessage assistantMessage,
            List<ContextCitation> citations,
            Map<Long, KbDocumentChunk> chunksById
    ) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }

        List<KbAnswerCitation> rows = new ArrayList<>();
        for (ContextCitation citation : citations) {
            for (Long chunkId : citation.chunkIds()) {
                KbDocumentChunk chunk = chunksById.get(chunkId);
                if (chunk == null) {
                    continue;
                }
                KbAnswerCitation row = new KbAnswerCitation();
                row.setId(idGenerator.nextId());
                row.setTenantId(session.getTenantId());
                row.setSessionId(session.getId());
                row.setMessageId(assistantMessage.getId());
                row.setDocId(citation.docId());
                row.setVersionId(chunk.getVersionId());
                row.setChunkId(chunk.getId());
                row.setPageNo(chunk.getPageNo());
                row.setSectionTitle(chunk.getSectionTitle());
                row.setQuoteText(chunk.getContent());
                row.setScore(citation.score() == null ? null : BigDecimal.valueOf(citation.score()));
                row.setRankNo(citation.citationNo());
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("doc_title", citation.docTitle());
                metadata.put("source_uri", citation.sourceUri());
                row.setMetadataJson(toJson(metadata));
                rows.add(row);
            }
        }
        return citationRepository.saveAll(rows);
    }

    private String toJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Conversation metadata cannot be serialized");
        }
    }

    private static String title(String query) {
        if (query == null || query.isBlank()) {
            return "新会话";
        }
        String trimmed = query.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80);
    }
}
