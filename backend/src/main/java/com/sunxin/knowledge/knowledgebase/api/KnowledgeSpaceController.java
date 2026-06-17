package com.sunxin.knowledge.knowledgebase.api;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.knowledgebase.application.KnowledgeSpaceApplicationService;
import com.sunxin.knowledge.knowledgebase.dto.CreateKnowledgeSpaceRequest;
import com.sunxin.knowledge.knowledgebase.dto.KnowledgeSpaceResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/api/v1/kb-spaces")
public class KnowledgeSpaceController {

    private final KnowledgeSpaceApplicationService spaceService;
    private final CurrentUserResolver currentUserResolver;

    public KnowledgeSpaceController(
            KnowledgeSpaceApplicationService spaceService,
            CurrentUserResolver currentUserResolver
    ) {
        this.spaceService = spaceService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    public ApiResponse<KnowledgeSpaceResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody CreateKnowledgeSpaceRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, request.tenantId());
        return ApiResponse.ok(spaceService.create(request, currentUser));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeSpaceResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long currentTenantId,
            @RequestParam @NotNull Long tenantId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, currentTenantId, tenantId);
        return ApiResponse.ok(spaceService.list(tenantId, currentUser));
    }
}
