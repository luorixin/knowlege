package com.sunxin.knowledge.qa.application;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.qa.dto.AgentChatRequest;

@Service
public class QuestionUnderstandingService {

    private final com.sunxin.knowledge.qa.llm.LlmProvider llmProvider;

    public QuestionUnderstandingService(com.sunxin.knowledge.qa.llm.LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public QuestionIntent understand(AgentChatRequest request, java.util.List<com.sunxin.knowledge.qa.llm.ChatMessage> history) {
        String query = request.query().trim();
        java.util.List<String> expanded = llmProvider.expandQuery(query, history);
        return new QuestionIntent(query, request.filters(), expanded);
    }
}
