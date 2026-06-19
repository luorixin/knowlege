package com.sunxin.knowledge.qa.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;

public record AgentMessageDto(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt,
        List<AgentCitationResponse> citations,
        boolean error
) {
}
