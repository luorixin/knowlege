package com.sunxin.knowledge.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import com.sunxin.knowledge.persistence.entity.KbAnswerCitation;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.entity.KbQueryMessage;
import com.sunxin.knowledge.persistence.entity.KbQuerySession;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbAnswerCitationRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbQueryMessageRepository;
import com.sunxin.knowledge.persistence.repository.KbQuerySessionRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AgentChatApiTest {

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
        permissionPolicyRepository.deleteAll();
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        spaceRepository.deleteAll();
    }

    @Test
    void chatAnswersFromRetrievedContextAndPersistsConversationWithCitations() throws Exception {
        KbSpace space = createSpace();
        KbDocument document = createDocument(space, "金融数据治理 proposal", "PROPOSAL", "金融", "数据治理");
        KbDocumentChunk chunk = createChunk(
                document,
                0,
                12,
                "解决方案",
                "金融行业数据治理 proposal 常见结构包括项目背景、现状诊断、解决方案、实施路径和交付计划。"
        );
        allowUserOnSpace(space, 42L);

        MvcResult result = mockMvc.perform(post("/api/agent/chat")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "space_id": %d,
                                  "query": "请根据历史资料总结金融行业数据治理 proposal 的常见结构",
                                  "filters": {
                                    "doc_type": "proposal",
                                    "industry": "金融"
                                  }
                                }
                                """.formatted(space.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_id").exists())
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("[引用1]")))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("项目背景")))
                .andExpect(jsonPath("$.data.citations", hasSize(1)))
                .andExpect(jsonPath("$.data.citations[0].citation_id").value(1))
                .andExpect(jsonPath("$.data.citations[0].doc_id").value(document.getId().toString()))
                .andExpect(jsonPath("$.data.citations[0].doc_title").value(document.getTitle()))
                .andExpect(jsonPath("$.data.citations[0].page_no").value(12))
                .andExpect(jsonPath("$.data.citations[0].section_title").value("解决方案"))
                .andReturn();

        Long sessionId = readLong(result, "$.data.session_id");
        List<KbQuerySession> sessions = querySessionRepository.findAll();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getId()).isEqualTo(sessionId);
        assertThat(sessions.get(0).getUserId()).isEqualTo(42L);
        assertThat(sessions.get(0).getSpaceId()).isEqualTo(space.getId());

        List<KbQueryMessage> messages = queryMessageRepository.findAll().stream()
                .sorted(Comparator.comparing(KbQueryMessage::getCreatedAt))
                .toList();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo("USER");
        assertThat(messages.get(0).getContent()).contains("金融行业数据治理 proposal");
        assertThat(messages.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(messages.get(1).getContent()).contains("[引用1]");
        assertThat(messages.get(1).getModelProvider()).isEqualTo("mock");

        List<KbAnswerCitation> citations = answerCitationRepository.findAll();
        assertThat(citations).hasSize(1);
        assertThat(citations.get(0).getMessageId()).isEqualTo(messages.get(1).getId());
        assertThat(citations.get(0).getSessionId()).isEqualTo(sessionId);
        assertThat(citations.get(0).getDocId()).isEqualTo(document.getId());
        assertThat(citations.get(0).getVersionId()).isEqualTo(document.getCurrentVersionId());
        assertThat(citations.get(0).getChunkId()).isEqualTo(chunk.getId());
        assertThat(citations.get(0).getPageNo()).isEqualTo(12);
        assertThat(citations.get(0).getSectionTitle()).isEqualTo("解决方案");
        assertThat(citations.get(0).getRankNo()).isEqualTo(1);
    }

    private static Long readLong(MvcResult result, String path) throws Exception {
        String value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return Long.valueOf(value);
    }

    @Test
    void chatSaysNoReliableEvidenceAndDoesNotCreateCitationsWhenRetrievalIsEmpty() throws Exception {
        KbSpace space = createSpace();
        allowUserOnSpace(space, 42L);

        mockMvc.perform(post("/api/agent/chat")
                        .header("X-User-Id", "42")
                        .header("X-Tenant-Id", space.getTenantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "space_id": %d,
                                  "query": "请总结不存在的资料",
                                  "filters": {
                                    "doc_type": "proposal",
                                    "industry": "金融"
                                  }
                                }
                                """.formatted(space.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("未在当前知识库中找到可靠依据")))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("[引用1]"))))
                .andExpect(jsonPath("$.data.citations", hasSize(0)));

        assertThat(querySessionRepository.findAll()).hasSize(1);
        assertThat(queryMessageRepository.findAll()).hasSize(2);
        assertThat(answerCitationRepository.findAll()).isEmpty();
    }

    private KbSpace createSpace() {
        KbSpace space = new KbSpace();
        space.setId(idGenerator.nextId());
        space.setTenantId(1001L);
        space.setName("Agent Space");
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
        document.setSourceUri("local://agent/" + title);
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

    private void allowUserOnSpace(KbSpace space, Long userId) {
        KbPermissionPolicy policy = new KbPermissionPolicy();
        policy.setId(idGenerator.nextId());
        policy.setTenantId(space.getTenantId());
        policy.setSpaceId(space.getId());
        policy.setSubjectType("USER");
        policy.setSubjectId(userId.toString());
        policy.setResourceType("SPACE");
        policy.setResourceId(space.getId());
        policy.setEffect("ALLOW");
        policy.setActions("document_read,agent_chat");
        policy.setStatus("ACTIVE");
        permissionPolicyRepository.save(policy);
    }
}
