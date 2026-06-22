package com.sunxin.knowledge.audit.dto;

import java.time.LocalDateTime;

public record AuditLogQueryRequest(
        Long actorUserId,
        String action,
        String resourceType,
        String resourceId,
        String resultStatus,
        String traceId,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        Integer page,
        Integer size
) {
}
