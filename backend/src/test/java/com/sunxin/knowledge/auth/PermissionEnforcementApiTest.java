package com.sunxin.knowledge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.persistence.entity.KbAuditLog;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbAnswerCitationRepository;
import com.sunxin.knowledge.persistence.repository.KbAuditLogRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbQueryMessageRepository;
import com.sunxin.knowledge.persistence.repository.KbQuerySessionRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PermissionEnforcementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private KbSpaceRepository spaceRepository;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbDocumentChunkRepository chunkRepository;

    @Autowired
    private KbPermissionPolicyRepository permissionPolicyRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @Autowired
    private KbQuerySessionRepository querySessionRepository;

    @Autowired
    private KbQueryMessageRepository queryMessageRepository;

    @Autowired
    private KbAnswerCitationRepository answerCitationRepository;

    @BeforeEach
    void cleanDatabase() {
        answerCitationRepository.deleteAll();
        queryMessageRepository.deleteAll();
        querySessionRepository.deleteAll();
        auditLogRepository.deleteAll();
        permissionPolicyRepository.deleteAll();
        chunkRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        spaceRepository.deleteAll();
        deleteIfExists("kb_user_role");
        deleteIfExists("kb_role");
        deleteIfExists("kb_user");
    }

    @Test
    void documentDetailDeniesUserWithoutDocumentReadAndWritesAuditLog() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "Restricted Proposal");

        mockMvc.perform(get("/api/v1/documents/{documentId}", document.getId())
                        .header("X-User-Id", "99")
                        .header("X-Tenant-Id", space.getTenantId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getActorUserId()).isEqualTo(99L);
                    assertThat(log.getAction()).isEqualTo("document_read");
                    assertThat(log.getResourceType()).isEqualTo("DOCUMENT");
                    assertThat(log.getResourceId()).isEqualTo(document.getId().toString());
                    assertThat(log.getResultStatus()).isEqualTo("DENIED");
                });
    }

    @Test
    void retrievalReturnsNoChunksWhenUserHasNoReadableDocuments() throws Exception {
        KbSpace space = createSpace();
        KbDocument first = createDocument(space, "金融数据治理 Proposal");
        KbDocument second = createDocument(space, "银行数据治理 SOW");
        createChunk(first, "金融行业数据治理 proposal 项目背景");
        createChunk(second, "银行数据治理 SOW 交付范围");

        mockMvc.perform(post("/api/retrieval/search")
                        .header("X-User-Id", "99")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "金融 数据治理",
                                  "space_id": %d,
                                  "top_k": 10
                                }
                                """.formatted(space.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(0)));
    }

    @Test
    void agentChatRequiresAgentChatPermissionEvenWhenUserCanReadDocuments() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "金融数据治理 Proposal");
        createChunk(document, "金融行业数据治理 proposal 常见结构包括项目背景和解决方案。");
        allowUserOnSpace(space, 42L, "document_read");

        mockMvc.perform(post("/api/agent/chat")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "space_id": %d,
                                  "query": "总结金融行业数据治理 proposal 的结构"
                                }
                                """.formatted(space.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(querySessionRepository.findAll()).isEmpty();
        assertThat(queryMessageRepository.findAll()).isEmpty();
    }

    @Test
    void documentDeleteRequiresDocumentDeletePermission() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "Read Only Proposal");
        allowUserOnDocument(space, document, 42L, "document_read");

        mockMvc.perform(delete("/api/v1/documents/{documentId}", document.getId())
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(documentRepository.findById(document.getId()).orElseThrow().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void rolePolicyAllowsMemberToReadDocumentAndDeniesNonMember() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "Role Protected Proposal");
        Long roleId = createRole(space.getTenantId(), "delivery_reader");
        createUser(space.getTenantId(), 42L, "alice");
        createUser(space.getTenantId(), 99L, "bob");
        assignRole(space.getTenantId(), 42L, roleId);
        allowRoleOnDocument(space, document, "delivery_reader", "document_read");

        mockMvc.perform(get("/api/v1/documents/{documentId}", document.getId())
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Role Protected Proposal"));

        mockMvc.perform(get("/api/v1/documents/{documentId}", document.getId())
                        .header("X-User-Id", "99")
                        .header("X-Tenant-Id", space.getTenantId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private KbSpace createSpace() {
        KbSpace space = new KbSpace();
        space.setId(idGenerator.nextId());
        space.setTenantId(1001L);
        space.setName("Permission Space");
        space.setOwnerUserId(7L);
        space.setStatus("ACTIVE");
        return spaceRepository.save(space);
    }

    private KbDocument createDocument(KbSpace space, String title) {
        KbDocument document = new KbDocument();
        document.setId(idGenerator.nextId());
        document.setTenantId(space.getTenantId());
        document.setSpaceId(space.getId());
        document.setTitle(title);
        document.setDocType("PROPOSAL");
        document.setIndustry("金融");
        document.setServiceLine("数据治理");
        document.setConfidentialLevel("INTERNAL");
        document.setSourceUri("local://permission/" + title);
        document.setStorageUri(document.getSourceUri());
        document.setFileHash("hash-" + document.getId());
        document.setStatus("ACTIVE");
        documentRepository.save(document);

        KbDocumentVersion version = new KbDocumentVersion();
        version.setId(idGenerator.nextId());
        version.setTenantId(space.getTenantId());
        version.setSpaceId(space.getId());
        version.setDocId(document.getId());
        version.setVersionNo(1);
        version.setSourceUri(document.getSourceUri());
        version.setStorageUri(document.getStorageUri());
        version.setFileHash(document.getFileHash());
        version.setParseStatus("COMPLETED");
        version.setChunkCount(0);
        version.setTotalTokens(0);
        version.setStatus("ACTIVE");
        versionRepository.save(version);

        document.setCurrentVersionId(version.getId());
        return documentRepository.save(document);
    }

    private void createChunk(KbDocument document, String content) {
        KbDocumentChunk chunk = new KbDocumentChunk();
        chunk.setId(idGenerator.nextId());
        chunk.setTenantId(document.getTenantId());
        chunk.setSpaceId(document.getSpaceId());
        chunk.setDocId(document.getId());
        chunk.setVersionId(document.getCurrentVersionId());
        chunk.setChunkIndex(0);
        chunk.setPageNo(1);
        chunk.setSectionTitle("项目背景");
        chunk.setContent(content);
        chunk.setTokenCount(content.length());
        chunk.setContentHash("hash-" + chunk.getId());
        chunk.setMetadataJson("{\"content_type\":\"text\"}");
        chunk.setStatus("ACTIVE");
        chunkRepository.save(chunk);
    }

    private void allowUserOnSpace(KbSpace space, Long userId, String actions) {
        KbPermissionPolicy policy = policy(space, "USER", userId.toString(), actions);
        policy.setResourceType("SPACE");
        policy.setResourceId(space.getId());
        permissionPolicyRepository.save(policy);
    }

    private void allowUserOnDocument(KbSpace space, KbDocument document, Long userId, String actions) {
        KbPermissionPolicy policy = policy(space, "USER", userId.toString(), actions);
        policy.setResourceType("DOCUMENT");
        policy.setResourceId(document.getId());
        permissionPolicyRepository.save(policy);
    }

    private void allowRoleOnDocument(KbSpace space, KbDocument document, String roleCode, String actions) {
        KbPermissionPolicy policy = policy(space, "ROLE", roleCode, actions);
        policy.setResourceType("DOCUMENT");
        policy.setResourceId(document.getId());
        permissionPolicyRepository.save(policy);
    }

    private KbPermissionPolicy policy(KbSpace space, String subjectType, String subjectId, String actions) {
        KbPermissionPolicy policy = new KbPermissionPolicy();
        policy.setId(idGenerator.nextId());
        policy.setTenantId(space.getTenantId());
        policy.setSpaceId(space.getId());
        policy.setSubjectType(subjectType);
        policy.setSubjectId(subjectId);
        policy.setEffect("ALLOW");
        policy.setActions(actions);
        policy.setStatus("ACTIVE");
        return policy;
    }

    private void createUser(Long tenantId, Long userId, String username) {
        jdbcTemplate.update("""
                INSERT INTO kb_user (id, tenant_id, username, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, userId, tenantId, username, username);
    }

    private Long createRole(Long tenantId, String code) {
        Long roleId = idGenerator.nextId();
        jdbcTemplate.update("""
                INSERT INTO kb_role (id, tenant_id, code, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, roleId, tenantId, code, code);
        return roleId;
    }

    private void assignRole(Long tenantId, Long userId, Long roleId) {
        jdbcTemplate.update("""
                INSERT INTO kb_user_role (id, tenant_id, user_id, role_id, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, idGenerator.nextId(), tenantId, userId, roleId);
    }

    private void deleteIfExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND LOWER(TABLE_NAME) = ?
                """, Integer.class, tableName);
        if (count != null && count > 0) {
            jdbcTemplate.execute("DELETE FROM " + tableName);
        }
    }
}
