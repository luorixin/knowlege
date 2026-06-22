package com.sunxin.knowledge.audit.api;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.audit.TokenUsageQueryService;
import com.sunxin.knowledge.audit.dto.TokenUsageQueryRequest;
import com.sunxin.knowledge.audit.dto.TokenUsageResponse;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.common.dto.PageResponse;

@RestController
@RequestMapping("/api/v1/admin/token-usage")
public class TokenUsageController {

    private final TokenUsageQueryService tokenUsageQueryService;

    public TokenUsageController(TokenUsageQueryService tokenUsageQueryService) {
        this.tokenUsageQueryService = tokenUsageQueryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TokenUsageResponse>> list(
            CurrentUser currentUser,
            @RequestParam(name = "model_provider", required = false) String modelProvider,
            @RequestParam(name = "model_name", required = false) String modelName,
            @RequestParam(name = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdFrom,
            @RequestParam(name = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(tokenUsageQueryService.search(
                new TokenUsageQueryRequest(
                        modelProvider,
                        modelName,
                        createdFrom,
                        createdTo,
                        page,
                        size
                ),
                currentUser
        ));
    }
}
