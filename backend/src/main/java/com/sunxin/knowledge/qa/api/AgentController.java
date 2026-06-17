package com.sunxin.knowledge.qa.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.qa.application.AgentService;
import com.sunxin.knowledge.qa.dto.AgentChatRequest;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;

import jakarta.validation.Valid;

@Validated
@RestController
public class AgentController {

    private final AgentService agentService;
    private final CurrentUserResolver currentUserResolver;

    public AgentController(
            AgentService agentService,
            CurrentUserResolver currentUserResolver
    ) {
        this.agentService = agentService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/agent/chat")
    public ApiResponse<AgentChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody AgentChatRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(agentService.chat(request, currentUser));
    }
}
