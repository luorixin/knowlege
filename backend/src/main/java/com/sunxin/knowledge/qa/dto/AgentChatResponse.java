package com.sunxin.knowledge.qa.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentChatResponse(
        @JsonProperty("session_id")
        Long sessionId,

        String answer,

        List<AgentCitationResponse> citations
) {
}
