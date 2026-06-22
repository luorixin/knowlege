package com.sunxin.knowledge.audit.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbAuditLog;

public record AuditLogResponse(
        String id,
        @JsonProperty("tenant_id")
        String tenantId,
        @JsonProperty("actor_user_id")
        String actorUserId,
        String action,
        @JsonProperty("resource_type")
        String resourceType,
        @JsonProperty("resource_id")
        String resourceId,
        @JsonProperty("result_status")
        String resultStatus,
        @JsonProperty("trace_id")
        String traceId,
        @JsonProperty("request_method")
        String requestMethod,
        @JsonProperty("request_uri")
        String requestUri,
        @JsonProperty("ip_address")
        String ipAddress,
        @JsonProperty("user_agent")
        String userAgent,
        @JsonProperty("detail_json")
        String detailJson,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static AuditLogResponse fromEntity(KbAuditLog log) {
        return new AuditLogResponse(
                String.valueOf(log.getId()),
                String.valueOf(log.getTenantId()),
                log.getActorUserId() == null ? null : String.valueOf(log.getActorUserId()),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getResultStatus(),
                log.getTraceId(),
                log.getRequestMethod(),
                log.getRequestUri(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getDetailJson(),
                log.getCreatedAt()
        );
    }
}
