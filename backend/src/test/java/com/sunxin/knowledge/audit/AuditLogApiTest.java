package com.sunxin.knowledge.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sunxin.knowledge.auth.SystemRoleConst;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.persistence.entity.KbAuditLog;
import com.sunxin.knowledge.persistence.entity.KbRole;
import com.sunxin.knowledge.persistence.entity.KbUserRole;
import com.sunxin.knowledge.persistence.repository.KbAuditLogRepository;
import com.sunxin.knowledge.persistence.repository.KbRoleRepository;
import com.sunxin.knowledge.persistence.repository.KbUserRoleRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AuditLogApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @Autowired
    private KbRoleRepository roleRepository;

    @Autowired
    private KbUserRoleRepository userRoleRepository;

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void adminCanQueryTenantAuditLogsWithFiltersAndRecordsQueryAudit() throws Exception {
        createRoleAssignment(1001L, 42L, SystemRoleConst.ADMIN);
        KbAuditLog matched = createAuditLog(1001L, 7L, "document_read", "DOCUMENT", "doc-1", "SUCCESS",
                "trace-a", LocalDateTime.of(2026, 1, 10, 10, 0), "{\"space_id\":1}");
        createAuditLog(1001L, 7L, "agent_chat", "SPACE", "space-1", "SUCCESS",
                "trace-b", LocalDateTime.of(2026, 1, 11, 10, 0), "{}");
        createAuditLog(2002L, 7L, "document_read", "DOCUMENT", "other-tenant-doc", "SUCCESS",
                "trace-c", LocalDateTime.of(2026, 1, 10, 10, 0), "{}");

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", "1001")
                        .param("action", "document_read")
                        .param("resource_type", "DOCUMENT")
                        .param("result_status", "SUCCESS")
                        .param("created_from", "2026-01-10T00:00:00")
                        .param("created_to", "2026-01-10T23:59:59")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(matched.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].tenant_id").value("1001"))
                .andExpect(jsonPath("$.data.content[0].actor_user_id").value("7"))
                .andExpect(jsonPath("$.data.content[0].action").value("document_read"))
                .andExpect(jsonPath("$.data.content[0].resource_type").value("DOCUMENT"))
                .andExpect(jsonPath("$.data.content[0].resource_id").value("doc-1"))
                .andExpect(jsonPath("$.data.content[0].result_status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].trace_id").value("trace-a"))
                .andExpect(jsonPath("$.data.content[0].detail_json").value("{\"space_id\":1}"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getTenantId()).isEqualTo(1001L);
                    assertThat(log.getActorUserId()).isEqualTo(42L);
                    assertThat(log.getAction()).isEqualTo("audit_log_query");
                    assertThat(log.getResourceType()).isEqualTo("AUDIT_LOG");
                    assertThat(log.getResultStatus()).isEqualTo(AuditLogRecorder.SUCCESS);
                });
    }

    @Test
    void nonAdminCannotQueryAuditLogs() throws Exception {
        createAuditLog(1001L, 7L, "document_read", "DOCUMENT", "doc-1", "SUCCESS",
                "trace-a", LocalDateTime.of(2026, 1, 10, 10, 0), "{}");

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("X-User-Id", "99")
                        .header("X-Tenant-Id", "1001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private KbAuditLog createAuditLog(
            Long tenantId,
            Long actorUserId,
            String action,
            String resourceType,
            String resourceId,
            String resultStatus,
            String traceId,
            LocalDateTime createdAt,
            String detailJson
    ) {
        KbAuditLog log = new KbAuditLog();
        log.setId(idGenerator.nextId());
        log.setTenantId(tenantId);
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setResultStatus(resultStatus);
        log.setTraceId(traceId);
        log.setRequestMethod("GET");
        log.setRequestUri("/api/test");
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("JUnit");
        log.setDetailJson(detailJson);
        log.setCreatedAt(createdAt);
        log.setUpdatedAt(createdAt);
        return auditLogRepository.save(log);
    }

    private void createRoleAssignment(Long tenantId, Long userId, String roleCode) {
        KbRole role = new KbRole();
        role.setId(idGenerator.nextId());
        role.setTenantId(tenantId);
        role.setCode(roleCode);
        role.setName(roleCode);
        role.setStatus("ACTIVE");
        roleRepository.save(role);

        KbUserRole userRole = new KbUserRole();
        userRole.setId(idGenerator.nextId());
        userRole.setTenantId(tenantId);
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setStatus("ACTIVE");
        userRoleRepository.save(userRole);
    }
}
