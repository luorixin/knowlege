package com.sunxin.knowledge.eval.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.eval.application.EvalService;
import com.sunxin.knowledge.eval.dto.EvalCaseCreateRequest;
import com.sunxin.knowledge.eval.dto.EvalCaseResponse;
import com.sunxin.knowledge.eval.dto.EvalDatasetCreateRequest;
import com.sunxin.knowledge.eval.dto.EvalDatasetResponse;
import com.sunxin.knowledge.eval.dto.EvalRunRequest;
import com.sunxin.knowledge.eval.dto.EvalRunResponse;

import jakarta.validation.Valid;

@Validated
@RestController
public class EvalController {

    private final EvalService evalService;
    private final CurrentUserResolver currentUserResolver;

    public EvalController(EvalService evalService, CurrentUserResolver currentUserResolver) {
        this.evalService = evalService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/eval/dataset")
    public ApiResponse<EvalDatasetResponse> createDataset(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody EvalDatasetCreateRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, request.tenantId());
        return ApiResponse.ok(evalService.createDataset(request, currentUser));
    }

    @GetMapping("/api/eval/dataset")
    public ApiResponse<java.util.List<EvalDatasetResponse>> listDatasets(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(evalService.listDatasets(currentUser));
    }

    @GetMapping("/api/eval/dataset/{datasetId}/cases")
    public ApiResponse<java.util.List<EvalCaseResponse>> listCases(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long datasetId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(evalService.listCases(datasetId, currentUser));
    }

    @PostMapping("/api/eval/case")
    public ApiResponse<EvalCaseResponse> createCase(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody EvalCaseCreateRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(evalService.createCase(request, currentUser));
    }

    @PostMapping("/api/eval/run")
    public ApiResponse<EvalRunResponse> run(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Valid @RequestBody EvalRunRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(evalService.run(request, currentUser));
    }

    @GetMapping("/api/eval/result/{runId}")
    public ApiResponse<EvalRunResponse> result(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String runId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(evalService.result(runId, currentUser));
    }
}
