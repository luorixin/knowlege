package com.sunxin.knowledge.eval.application;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.eval.dto.EvalCaseSpec;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;

public interface EvalAnswerClient {

    AgentChatResponse answer(Long spaceId, String question, EvalCaseSpec spec, CurrentUser user);
}
