package com.sunxin.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
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
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class RetrievalSearchApiTest {

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

    @BeforeEach
    void cleanDatabase() {
        permissionPolicyRepository.deleteAll();
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        spaceRepository.deleteAll();
    }

    @Test
    void searchReturnsAccessibleFilteredChunksWithCitationFieldsAndDeduplication() throws Exception {
        KbSpace space = createSpace();
        KbDocument matchingProposal = createDocument(space, "金融数据治理 Proposal", "PROPOSAL", "金融", "数据治理");
        KbDocument vectorOnlyCase = createDocument(space, "银行数据资产案例", "PROPOSAL", "金融", "数据治理");
        KbDocument deniedProposal = createDocument(space, "禁止访问 Proposal", "PROPOSAL", "金融", "数据治理");
        KbDocument wrongIndustry = createDocument(space, "制造业数据治理 Proposal", "PROPOSAL", "制造", "数据治理");
        KbDocument wrongType = createDocument(space, "金融制度文档", "POLICY", "金融", "数据治理");

        KbDocumentChunk keywordChunk = createChunk(
                matchingProposal,
                3,
                "项目背景",
                "金融行业数据治理 proposal 类似案例，包含主数据、数据标准和治理组织设计。"
        );
        KbDocumentChunk vectorChunk = createChunk(
                vectorOnlyCase,
                5,
                "案例摘要",
                "银行客户主数据质量提升，建设数据资产目录、责任矩阵和治理流程。"
        );
        createChunk(deniedProposal, 1, "保密案例", "金融行业数据治理 proposal 禁止访问案例。");
        createChunk(wrongIndustry, 2, "项目背景", "制造业数据治理 proposal 案例。");
        createChunk(wrongType, 1, "制度流程", "金融行业数据治理制度流程。");
        allowUserOnSpace(space, 42L);
        denyUserOnDocument(space, 42L, deniedProposal);

        MvcResult result = mockMvc.perform(post("/api/retrieval/search")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "金融行业数据治理 proposal 有哪些类似案例？",
                                  "space_id": %d,
                                  "filters": {
                                    "doc_type": "proposal",
                                    "industry": "金融",
                                    "service_line": "数据治理",
                                    "year_from": 2022
                                  },
                                  "top_k": 20
                                }
                                """.formatted(space.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(2)))
                .andExpect(jsonPath("$.data.results[*].doc_title", hasItem("金融数据治理 Proposal")))
                .andExpect(jsonPath("$.data.results[*].doc_title", hasItem("银行数据资产案例")))
                .andExpect(jsonPath("$.data.results[*].doc_title", not(hasItem("禁止访问 Proposal"))))
                .andExpect(jsonPath("$.data.results[*].doc_title", not(hasItem("制造业数据治理 Proposal"))))
                .andExpect(jsonPath("$.data.results[*].doc_title", not(hasItem("金融制度文档"))))
                .andExpect(jsonPath("$.data.results[0].chunk_id").exists())
                .andExpect(jsonPath("$.data.results[0].doc_id").exists())
                .andExpect(jsonPath("$.data.results[0].doc_title").exists())
                .andExpect(jsonPath("$.data.results[0].page_no").exists())
                .andExpect(jsonPath("$.data.results[0].section_title").exists())
                .andExpect(jsonPath("$.data.results[0].content").exists())
                .andExpect(jsonPath("$.data.results[0].score").exists())
                .andExpect(jsonPath("$.data.results[0].source_uri").exists())
                .andReturn();

        List<Number> chunkIds = JsonPath.read(result.getResponse().getContentAsString(), "$.data.results[*].chunk_id");
        assertThat(new HashSet<>(chunkIds)).hasSize(chunkIds.size());
        assertThat(chunkIds).contains(keywordChunk.getId(), vectorChunk.getId());
    }

    @Test
    void searchReturnsNoChunksWhenSpaceHasPoliciesButUserHasNoAllow() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "金融数据治理 Proposal", "PROPOSAL", "金融", "数据治理");
        createChunk(document, 1, "项目背景", "金融行业数据治理 proposal 类似案例。");
        allowUserOnSpace(space, 42L);

        mockMvc.perform(post("/api/retrieval/search")
                        .header("X-User-Id", "99")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "金融行业数据治理 proposal",
                                  "space_id": %d,
                                  "top_k": 10
                                }
                                """.formatted(space.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(0)));
    }

    private KbSpace createSpace() {
        KbSpace space = new KbSpace();
        space.setId(idGenerator.nextId());
        space.setTenantId(1001L);
        space.setName("Retrieval Space");
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
        document.setSourceUri("local://retrieval/" + title);
        document.setStorageUri(document.getSourceUri());
        document.setFileHash("hash-" + document.getId());
        document.setCurrentVersionId(idGenerator.nextId());
        document.setStatus("ACTIVE");
        return documentRepository.save(document);
    }

    private KbDocumentChunk createChunk(KbDocument document, int pageNo, String sectionTitle, String content) {
        KbDocumentChunk chunk = new KbDocumentChunk();
        chunk.setId(idGenerator.nextId());
        chunk.setTenantId(document.getTenantId());
        chunk.setSpaceId(document.getSpaceId());
        chunk.setDocId(document.getId());
        chunk.setVersionId(document.getCurrentVersionId());
        chunk.setChunkIndex(0);
        chunk.setPageNo(pageNo);
        chunk.setSectionTitle(sectionTitle);
        chunk.setContent(content);
        chunk.setTokenCount(content.length());
        chunk.setContentHash("hash-" + chunk.getId());
        chunk.setMetadataJson("{\"content_type\":\"text\"}");
        chunk.setStatus("ACTIVE");
        return chunkRepository.save(chunk);
    }

    private void allowUserOnSpace(KbSpace space, Long userId) {
        KbPermissionPolicy policy = policy(space, userId);
        policy.setResourceType("SPACE");
        policy.setResourceId(space.getId());
        policy.setEffect("ALLOW");
        permissionPolicyRepository.save(policy);
    }

    private void denyUserOnDocument(KbSpace space, Long userId, KbDocument document) {
        KbPermissionPolicy policy = policy(space, userId);
        policy.setResourceType("DOCUMENT");
        policy.setResourceId(document.getId());
        policy.setEffect("DENY");
        policy.setPriority(100);
        permissionPolicyRepository.save(policy);
    }

    private KbPermissionPolicy policy(KbSpace space, Long userId) {
        KbPermissionPolicy policy = new KbPermissionPolicy();
        policy.setId(idGenerator.nextId());
        policy.setTenantId(space.getTenantId());
        policy.setSpaceId(space.getId());
        policy.setSubjectType("USER");
        policy.setSubjectId(userId.toString());
        policy.setActions("RETRIEVE,READ");
        policy.setStatus("ACTIVE");
        return policy;
    }
}
