package com.sunxin.knowledge.audit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.persistence.entity.KbAuditLog;
import com.sunxin.knowledge.persistence.repository.KbAuditLogRepository;

@Service
public class AuditLogRecorder {

    public static final String SUCCESS = "SUCCESS";
    public static final String DENIED = "DENIED";

    private final KbAuditLogRepository auditLogRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public AuditLogRecorder(
            KbAuditLogRepository auditLogRepository,
            IdGenerator idGenerator,
            ObjectMapper objectMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            CurrentUser user,
            Long tenantId,
            String action,
            String resourceType,
            Object resourceId,
            String resultStatus,
            Map<String, ?> detail
    ) {
        KbAuditLog log = new KbAuditLog();
        log.setId(idGenerator.nextId());
        log.setTenantId(tenantId == null ? 0L : tenantId);
        log.setActorUserId(user == null ? null : user.userId());
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId == null ? null : String.valueOf(resourceId));
        log.setResultStatus(resultStatus);
        log.setDetailJson(toJson(detail == null ? Map.of() : detail));
        auditLogRepository.save(log);
    }

    public Map<String, Object> detail(Object... pairs) {
        Map<String, Object> detail = new LinkedHashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            detail.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return detail;
    }

    private String toJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
