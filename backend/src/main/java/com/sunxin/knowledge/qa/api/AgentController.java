package com.sunxin.knowledge.qa.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.qa.application.AgentService;
import com.sunxin.knowledge.qa.dto.AgentChatRequest;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.qa.dto.AgentSessionDto;
import com.sunxin.knowledge.qa.dto.AgentMessageDto;
import java.util.List;

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

    @GetMapping("/api/agent/kb-spaces/{spaceId}/sessions")
    public ApiResponse<List<AgentSessionDto>> listSessions(
            @PathVariable Long spaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(agentService.listSessions(spaceId, currentUser));
    }

    @GetMapping("/api/agent/kb-spaces/{spaceId}/sessions/{sessionId}/messages")
    public ApiResponse<List<AgentMessageDto>> getSessionMessages(
            @PathVariable Long spaceId,
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(agentService.getSessionMessages(spaceId, sessionId, currentUser));
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

    @PostMapping(value = "/api/agent/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody AgentChatRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return agentService.streamChat(request, currentUser);
    }
}
