package com.sunxin.knowledge.retrieval.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.retrieval.application.RetrievalSearchService;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchRequest;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestMapping;

@Validated
@RestController
@RequestMapping({"/api/v1/retrieval", "/api/retrieval"})
public class RetrievalController {

    private final RetrievalSearchService retrievalSearchService;
    private final CurrentUserResolver currentUserResolver;

    public RetrievalController(
            RetrievalSearchService retrievalSearchService,
            CurrentUserResolver currentUserResolver
    ) {
        this.retrievalSearchService = retrievalSearchService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/search")
    public ApiResponse<RetrievalSearchResponse> search(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody RetrievalSearchRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(retrievalSearchService.search(request, currentUser));
    }
}
