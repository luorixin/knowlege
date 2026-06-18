package com.sunxin.knowledge.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbEvalResult;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbAnswerCitationRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbEvalCaseRepository;
import com.sunxin.knowledge.persistence.repository.KbEvalDatasetRepository;
import com.sunxin.knowledge.persistence.repository.KbEvalResultRepository;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbQueryMessageRepository;
import com.sunxin.knowledge.persistence.repository.KbQuerySessionRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class EvalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private KbSpaceRepository spaceRepository;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentChunkRepository chunkRepository;

    @Autowired
    private KbPermissionPolicyRepository permissionPolicyRepository;

    @Autowired
    private KbEvalDatasetRepository evalDatasetRepository;

    @Autowired
    private KbEvalCaseRepository evalCaseRepository;

    @Autowired
    private KbEvalResultRepository evalResultRepository;

    @Autowired
    private KbQuerySessionRepository querySessionRepository;

    @Autowired
    private KbQueryMessageRepository queryMessageRepository;

    @Autowired
    private KbAnswerCitationRepository answerCitationRepository;

    @BeforeEach
    void cleanDatabase() {
        evalResultRepository.deleteAll();
        evalCaseRepository.deleteAll();
        evalDatasetRepository.deleteAll();
        answerCitationRepository.deleteAll();
        queryMessageRepository.deleteAll();
        querySessionRepository.deleteAll();
        permissionPolicyRepository.deleteAll();
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        spaceRepository.deleteAll();
    }

    @Test
    void createsDatasetCasesRunsEvaluationAndReturnsReport() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "金融数据治理 Proposal", "PROPOSAL", "金融", "数据治理");
        KbDocumentChunk expectedChunk = createChunk(
                document,
                0,
                12,
                "解决方案",
                "金融行业数据治理 proposal 常见结构包括项目背景、现状诊断、解决方案、实施路径和交付计划。"
        );
        allowUserOnSpace(space, 42L, "document_read,agent_chat,admin_manage");

        Long datasetId = createDataset(space);
        createEvalCase(
                datasetId,
                "请总结金融行业数据治理 proposal 的常见结构",
                document.getId(),
                expectedChunk.getId(),
                false
        );
        createEvalCase(
                datasetId,
                "请总结当前知识库不存在的保险理赔风控白皮书",
                null,
                null,
                true
        );

        MvcResult run = mockMvc.perform(post("/api/eval/run")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset_id": %d,
                                  "top_k": 5
                                }
                                """.formatted(datasetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.run_id", startsWith("eval-")))
                .andExpect(jsonPath("$.data.dataset_id").value(datasetId))
                .andExpect(jsonPath("$.data.case_count").value(2))
                .andExpect(jsonPath("$.data.metrics.recall_at_k").value(1.0))
                .andExpect(jsonPath("$.data.metrics.precision_at_k").value(0.5))
                .andExpect(jsonPath("$.data.metrics.mrr").value(1.0))
                .andExpect(jsonPath("$.data.metrics.citation_accuracy").value(1.0))
                .andExpect(jsonPath("$.data.metrics.no_answer_accuracy").value(1.0))
                .andExpect(jsonPath("$.data.metrics.permission_violation_count").value(0))
                .andExpect(jsonPath("$.data.cases", hasSize(2)))
                .andReturn();

        String runId = JsonPath.read(run.getResponse().getContentAsString(), "$.data.run_id");

        mockMvc.perform(get("/api/eval/result/{runId}", runId)
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.run_id").value(runId))
                .andExpect(jsonPath("$.data.metrics.recall_at_k").value(1.0))
                .andExpect(jsonPath("$.data.metrics.no_answer_accuracy").value(1.0))
                .andExpect(jsonPath("$.data.cases", hasSize(2)));

        List<KbEvalResult> rows = evalResultRepository.findAll().stream()
                .sorted(Comparator.comparing(KbEvalResult::getCaseId))
                .toList();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getRunId()).isEqualTo(runId);
        assertThat(rows.get(0).getDetailJson()).contains("recall_hit", "retrieved_chunk_ids", "citation_accuracy");
    }

    @Test
    void runEvaluationDetectsPermissionViolationWhenExpectedChunkIsNotAccessible() throws Exception {
        KbSpace space = createSpace();
        KbDocument allowedDocument = createDocument(space, "公开 Proposal", "PROPOSAL", "金融", "数据治理");
        KbDocument restrictedDocument = createDocument(space, "受限 Proposal", "PROPOSAL", "金融", "数据治理");
        createChunk(allowedDocument, 0, 1, "项目背景", "金融行业数据治理 proposal 公开案例。");
        KbDocumentChunk restrictedChunk = createChunk(
                restrictedDocument,
                0,
                1,
                "保密章节",
                "金融行业数据治理 proposal 受限案例。"
        );
        allowUserOnDocument(space, allowedDocument, 42L, "document_read,agent_chat,admin_manage");
        allowUserOnSpace(space, 42L, "agent_chat");

        Long datasetId = createDataset(space);
        createEvalCase(datasetId, "金融行业数据治理 proposal 受限案例", restrictedDocument.getId(), restrictedChunk.getId(), false);

        mockMvc.perform(post("/api/eval/run")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset_id": %d,
                                  "top_k": 5
                                }
                                """.formatted(datasetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.recall_at_k").value(0.0))
                .andExpect(jsonPath("$.data.metrics.permission_violation_count").value(0))
                .andExpect(jsonPath("$.data.cases[0].inaccessible_expected_target_count").value(1))
                .andExpect(jsonPath("$.data.cases[0].permission_violation", is(false)));
    }

    private Long createDataset(KbSpace space) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/eval/dataset")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_id": %d,
                                  "space_id": "%d",
                                  "name": "RAG Eval",
                                  "description": "MVP evaluation set"
                                }
                """.formatted(space.getTenantId(), space.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.name").value("RAG Eval"))
                .andReturn();
        String datasetId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return Long.valueOf(datasetId);
    }

    private void createEvalCase(
            Long datasetId,
            String question,
            Long expectedDocId,
            Long expectedChunkId,
            boolean expectNoAnswer
    ) throws Exception {
        String expectedDocIds = expectedDocId == null ? "[]" : "[\"" + expectedDocId + "\"]";
        String expectedChunkIds = expectedChunkId == null ? "[]" : "[\"" + expectedChunkId + "\"]";
        mockMvc.perform(post("/api/eval/case")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset_id": "%d",
                                  "question": "%s",
                                  "expected_answer": "%s",
                                  "expected_doc_ids": %s,
                                  "expected_chunk_ids": %s,
                                  "expect_no_answer": %s,
                                  "filters": {
                                    "doc_type": "proposal",
                                    "industry": "金融"
                                  }
                                }
                                """.formatted(
                                datasetId,
                                question,
                                expectNoAnswer ? "未在当前知识库中找到可靠依据" : "项目背景、现状诊断、解决方案、实施路径、交付计划",
                                expectedDocIds,
                                expectedChunkIds,
                expectNoAnswer
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isString());
    }

    private KbSpace createSpace() {
        KbSpace space = new KbSpace();
        space.setId(idGenerator.nextId());
        space.setTenantId(1001L);
        space.setName("Eval Space");
        space.setOwnerUserId(7L);
        space.setStatus("ACTIVE");
        return spaceRepository.save(space);
    }

    private KbDocument createDocument(
            KbSpace space,
            String title,
            String docType,
            String industry,
            String serviceLine
    ) {
        KbDocument document = new KbDocument();
        document.setId(idGenerator.nextId());
        document.setTenantId(space.getTenantId());
        document.setSpaceId(space.getId());
        document.setTitle(title);
        document.setDocType(docType);
        document.setIndustry(industry);
        document.setServiceLine(serviceLine);
        document.setConfidentialLevel("INTERNAL");
        document.setSourceUri("local://eval/" + title);
        document.setStorageUri(document.getSourceUri());
        document.setFileHash("hash-" + document.getId());
        document.setCurrentVersionId(idGenerator.nextId());
        document.setStatus("ACTIVE");
        return documentRepository.save(document);
    }

    private KbDocumentChunk createChunk(
            KbDocument document,
            int chunkIndex,
            int pageNo,
            String sectionTitle,
            String content
    ) {
        KbDocumentChunk chunk = new KbDocumentChunk();
        chunk.setId(idGenerator.nextId());
        chunk.setTenantId(document.getTenantId());
        chunk.setSpaceId(document.getSpaceId());
        chunk.setDocId(document.getId());
        chunk.setVersionId(document.getCurrentVersionId());
        chunk.setChunkIndex(chunkIndex);
        chunk.setPageNo(pageNo);
        chunk.setSectionTitle(sectionTitle);
        chunk.setContent(content);
        chunk.setTokenCount(content.length());
        chunk.setContentHash("hash-" + chunk.getId());
        chunk.setMetadataJson("{\"content_type\":\"text\"}");
        chunk.setStatus("ACTIVE");
        return chunkRepository.save(chunk);
    }

    private void allowUserOnSpace(KbSpace space, Long userId, String actions) {
        KbPermissionPolicy policy = policy(space, userId, actions);
        policy.setResourceType("SPACE");
        policy.setResourceId(space.getId());
        permissionPolicyRepository.save(policy);
    }

    private void allowUserOnDocument(KbSpace space, KbDocument document, Long userId, String actions) {
        KbPermissionPolicy policy = policy(space, userId, actions);
        policy.setResourceType("DOCUMENT");
        policy.setResourceId(document.getId());
        permissionPolicyRepository.save(policy);
    }

    private KbPermissionPolicy policy(KbSpace space, Long userId, String actions) {
        KbPermissionPolicy policy = new KbPermissionPolicy();
        policy.setId(idGenerator.nextId());
        policy.setTenantId(space.getTenantId());
        policy.setSpaceId(space.getId());
        policy.setSubjectType("USER");
        policy.setSubjectId(userId.toString());
        policy.setEffect("ALLOW");
        policy.setActions(actions);
        policy.setStatus("ACTIVE");
        return policy;
    }
}
