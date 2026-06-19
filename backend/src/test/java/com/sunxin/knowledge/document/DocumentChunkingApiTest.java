package com.sunxin.knowledge.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDesensitizationMappingRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.task.domain.TaskStatus;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class DocumentChunkingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentChunkRepository chunkRepository;

    @Autowired
    private KbDesensitizationMappingRepository desensitizationMappingRepository;

    @Autowired
    private KbDocumentParseTaskRepository parseTaskRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbSpaceRepository spaceRepository;

    @Autowired
    private KbPermissionPolicyRepository permissionPolicyRepository;

    @Autowired
    private IdGenerator idGenerator;

    @BeforeEach
    void cleanDatabase() {
        permissionPolicyRepository.deleteAll();
        desensitizationMappingRepository.deleteAll();
        chunkRepository.deleteAll();
        parseTaskRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        spaceRepository.deleteAll();
    }

    @Test
    void rebuildChunksSplitsByHeadingsAndLengthThenUpdatesParseTask() throws Exception {
        UploadedDocument uploaded = uploadDocument("proposal.md", "# Placeholder");
        String longParagraph = "项目背景".repeat(45) + "实施目标".repeat(45);

        mockMvc.perform(post("/api/v1/documents/{documentId}/chunks/rebuild", uploaded.documentId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkSize": 80,
                                  "overlap": 10,
                                  "pages": [
                                    {
                                      "pageNo": 1,
                                      "sectionTitle": "Proposal",
                                      "contentType": "text",
                                      "content": "# 项目背景\\n%s\\n# 解决方案\\n短段落",
                                      "metadata": {"parser": "test"}
                                    }
                                  ]
                                }
                                """.formatted(longParagraph)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(uploaded.documentId()))
                .andExpect(jsonPath("$.data.versionId").value(uploaded.versionId()))
                .andExpect(jsonPath("$.data.chunkCount").value(7))
                .andExpect(jsonPath("$.data.parseStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.chunks", hasSize(7)))
                .andExpect(jsonPath("$.data.chunks[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data.chunks[0].pageNo").value(1))
                .andExpect(jsonPath("$.data.chunks[0].sectionTitle").value("项目背景"));

        List<KbDocumentChunk> chunks = chunksByIndex(uploaded.versionId());
        assertThat(chunks).hasSize(7);
        assertThat(chunks).extracting(KbDocumentChunk::getChunkIndex)
                .containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(chunks.get(0).getContent()).contains("# 项目背景");
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getContent().length()).isLessThanOrEqualTo(80));
        assertThat(chunks.get(1).getContent())
                .startsWith(chunks.get(0).getContent().substring(chunks.get(0).getContent().length() - 10));
        assertThat(chunks.get(6).getSectionTitle()).isEqualTo("解决方案");
        assertThat(chunks.get(0).getMetadataJson()).contains("\"content_type\":\"text\"");
        assertThat(chunks.get(0).getTokenCount()).isEqualTo(chunks.get(0).getContent().length());

        KbDocumentVersion version = versionRepository.findById(uploaded.versionId()).orElseThrow();
        assertThat(version.getChunkCount()).isEqualTo(7);
        assertThat(version.getParseStatus()).isEqualTo("COMPLETED");
        assertThat(version.getTotalTokens()).isEqualTo(
                chunks.stream().mapToInt(KbDocumentChunk::getTokenCount).sum()
        );

        KbDocumentParseTask task = parseTaskRepository.findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(
                uploaded.documentId(),
                uploaded.versionId()
        ).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getProgressPercent()).isEqualTo(100);
    }

    @Test
    void rechunkDeletesOldChunksAndRebuildsWithNewIndexes() throws Exception {
        UploadedDocument uploaded = uploadDocument("scope-sow.docx", "scope");

        rebuildChunks(uploaded.documentId(), 50, 5, "第一版内容".repeat(12));
        assertThat(chunksByIndex(uploaded.versionId())).hasSize(2);

        rebuildChunks(uploaded.documentId(), 120, 0, "第二版内容");

        List<KbDocumentChunk> chunks = chunksByIndex(uploaded.versionId());
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getChunkIndex()).isZero();
        assertThat(chunks.get(0).getContent()).isEqualTo("第二版内容");
    }

    @Test
    void pptDocumentChunksByPage() throws Exception {
        UploadedDocument uploaded = uploadDocument("deck.pptx", "deck");

        mockMvc.perform(post("/api/v1/documents/{documentId}/chunks/rebuild", uploaded.documentId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkSize": 800,
                                  "overlap": 100,
                                  "pages": [
                                    {"pageNo": 1, "sectionTitle": "封面", "contentType": "text", "content": "封面内容", "metadata": {}},
                                    {"pageNo": 2, "sectionTitle": "方案", "contentType": "text", "content": "方案内容", "metadata": {}}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkCount").value(2));

        List<KbDocumentChunk> chunks = chunksByIndex(uploaded.versionId());
        assertThat(chunks).extracting(KbDocumentChunk::getPageNo).containsExactly(1, 2);
        assertThat(chunks).extracting(KbDocumentChunk::getSectionTitle).containsExactly("封面", "方案");
    }

    @Test
    void rebuildChunksDesensitizesParsedContentBeforeSavingChunksAndRecordsStatus() throws Exception {
        UploadedDocument uploaded = uploadDocument("sensitive-proposal.md", "placeholder");

        mockMvc.perform(post("/api/v1/documents/{documentId}/chunks/rebuild", uploaded.documentId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkSize": 800,
                                  "overlap": 100,
                                  "pages": [
                                    {
                                      "pageNo": 1,
                                      "sectionTitle": "客户信息",
                                      "contentType": "text",
                                      "content": "客户名称：平安银行\\n客户联系人：张三\\n手机号：13812348888\\n邮箱：alice@company.com\\n身份证号：110105199001011234\\n报价金额：250万元",
                                      "metadata": {}
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunks[0].content").value(containsString("138****8888")))
                .andExpect(jsonPath("$.data.chunks[0].content").value(containsString("a***@company.com")))
                .andExpect(jsonPath("$.data.chunks[0].content").value(containsString("客户联系人A")))
                .andExpect(jsonPath("$.data.chunks[0].content").value(containsString("某金融客户")))
                .andExpect(jsonPath("$.data.chunks[0].content").value(containsString("金额区间：100万-300万")))
                .andExpect(jsonPath("$.data.chunks[0].content").value(not(containsString("13812348888"))))
                .andExpect(jsonPath("$.data.chunks[0].content").value(not(containsString("alice@company.com"))))
                .andExpect(jsonPath("$.data.chunks[0].content").value(not(containsString("110105199001011234"))))
                .andExpect(jsonPath("$.data.chunks[0].content").value(not(containsString("张三"))));

        KbDocumentChunk chunk = chunksByIndex(uploaded.versionId()).get(0);
        assertThat(chunk.getContent()).contains("138****8888", "a***@company.com", "客户联系人A", "某金融客户");
        assertThat(chunk.getContent()).doesNotContain("13812348888", "alice@company.com", "110105199001011234", "张三", "平安银行");

        KbDocumentVersion version = versionRepository.findById(uploaded.versionId()).orElseThrow();
        assertThat(version.getDesensitizeStatus()).isEqualTo("COMPLETED");
        assertThat(version.getDesensitizedAt()).isNotNull();
    }

    @Test
    void desensitizationMappingsRequireAdminManagePermission() throws Exception {
        UploadedDocument uploaded = uploadDocument("mapping-proposal.md", "placeholder");
        rebuildChunks(uploaded.documentId(), 800, 100, "手机号：13812348888，邮箱：alice@company.com");

        mockMvc.perform(get("/api/v1/documents/{documentId}/desensitization-mappings", uploaded.documentId())
                        .header("X-User-Id", "99")
                        .header("X-Tenant-Id", "1001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        allowUserOnDocument(uploaded.documentId(), 42L, "admin_manage");

        mockMvc.perform(get("/api/v1/documents/{documentId}/desensitization-mappings", uploaded.documentId())
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].maskedValue").exists())
                .andExpect(jsonPath("$.data[0].originalValue").exists());
    }

    private void rebuildChunks(Long documentId, int chunkSize, int overlap, String content) throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/chunks/rebuild", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkSize": %d,
                                  "overlap": %d,
                                  "pages": [
                                    {"pageNo": 1, "sectionTitle": "范围", "contentType": "text", "content": "%s", "metadata": {}}
                                  ]
                                }
                                """.formatted(chunkSize, overlap, content)))
                .andExpect(status().isOk());
    }

    private UploadedDocument uploadDocument(String filename, String content) throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                filename,
                                MediaType.TEXT_PLAIN_VALUE,
                                content.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();
        Long documentId = readLong(upload, "$.data.documentId");
        Long versionId = readLong(upload, "$.data.versionId");
        return new UploadedDocument(documentId, versionId);
    }

    private Long createSpace() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/kb-spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": 1001,
                                  "name": "Chunking Space"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readLong(result, "$.data.id");
    }

    private static Long readLong(MvcResult result, String path) throws Exception {
        String value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return Long.valueOf(value);
    }

    private List<KbDocumentChunk> chunksByIndex(Long versionId) {
        return chunkRepository.findAll().stream()
                .filter(chunk -> chunk.getVersionId().equals(versionId))
                .sorted(Comparator.comparing(KbDocumentChunk::getChunkIndex))
                .toList();
    }

    private void allowUserOnDocument(Long documentId, Long userId, String actions) {
        var document = documentRepository.findById(documentId).orElseThrow();
        KbPermissionPolicy policy = new KbPermissionPolicy();
        policy.setId(idGenerator.nextId());
        policy.setTenantId(document.getTenantId());
        policy.setSpaceId(document.getSpaceId());
        policy.setSubjectType("USER");
        policy.setSubjectId(userId.toString());
        policy.setResourceType("DOCUMENT");
        policy.setResourceId(documentId);
        policy.setEffect("ALLOW");
        policy.setActions(actions);
        policy.setStatus("ACTIVE");
        permissionPolicyRepository.save(policy);
    }

    private record UploadedDocument(Long documentId, Long versionId) {
    }
}
