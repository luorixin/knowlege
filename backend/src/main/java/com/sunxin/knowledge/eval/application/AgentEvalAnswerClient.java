package com.sunxin.knowledge.eval.application;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.eval.dto.EvalCaseSpec;
import com.sunxin.knowledge.qa.application.AgentService;
import com.sunxin.knowledge.qa.dto.AgentChatRequest;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;

@Component
public class AgentEvalAnswerClient implements EvalAnswerClient {

    private final AgentService agentService;

    public AgentEvalAnswerClient(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public AgentChatResponse answer(Long spaceId, String question, EvalCaseSpec spec, CurrentUser user) {
        return agentService.chat(new AgentChatRequest(spaceId, null, question, spec.filters()), user);
    }
}
