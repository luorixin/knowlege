package com.sunxin.knowledge.qa.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.qa.application.AgentService;
import com.sunxin.knowledge.qa.dto.AgentChatRequest;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.qa.dto.AgentSessionDto;
import com.sunxin.knowledge.qa.dto.AgentMessageDto;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestMapping;

@Validated
@RestController
@RequestMapping({"/api/v1/agent", "/api/agent"})
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

    @GetMapping("/kb-spaces/{spaceId}/sessions")
    public ApiResponse<PageResponse<AgentSessionDto>> listSessions(
            @PathVariable Long spaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(agentService.listSessions(spaceId, currentUser, page, size));
    }

    @GetMapping("/kb-spaces/{spaceId}/sessions/{sessionId}/messages")
    public ApiResponse<PageResponse<AgentMessageDto>> getSessionMessages(
            @PathVariable Long spaceId,
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(agentService.getSessionMessages(spaceId, sessionId, currentUser, page, size));
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody AgentChatRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(agentService.chat(request, currentUser));
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody AgentChatRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return agentService.streamChat(request, currentUser);
    }
}
