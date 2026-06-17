package com.sunxin.knowledge.qa.application;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.qa.dto.AgentChatRequest;

@Service
public class QuestionUnderstandingService {

    public QuestionIntent understand(AgentChatRequest request) {
        return new QuestionIntent(request.query().trim(), request.filters());
    }
}
