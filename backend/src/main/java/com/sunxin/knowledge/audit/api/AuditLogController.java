package com.sunxin.knowledge.audit.api;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.audit.AuditLogQueryService;
import com.sunxin.knowledge.audit.dto.AuditLogQueryRequest;
import com.sunxin.knowledge.audit.dto.AuditLogResponse;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.common.dto.PageResponse;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    public AuditLogController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            CurrentUser currentUser,
            @RequestParam(name = "actor_user_id", required = false) Long actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(name = "resource_type", required = false) String resourceType,
            @RequestParam(name = "resource_id", required = false) String resourceId,
            @RequestParam(name = "result_status", required = false) String resultStatus,
            @RequestParam(name = "trace_id", required = false) String traceId,
            @RequestParam(name = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdFrom,
            @RequestParam(name = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(auditLogQueryService.search(
                new AuditLogQueryRequest(
                        actorUserId,
                        action,
                        resourceType,
                        resourceId,
                        resultStatus,
                        traceId,
                        createdFrom,
                        createdTo,
                        page,
                        size
                ),
                currentUser
        ));
    }
}
