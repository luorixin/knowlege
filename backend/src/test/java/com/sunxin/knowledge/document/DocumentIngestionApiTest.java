package com.sunxin.knowledge.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

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
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class DocumentIngestionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbSpaceRepository spaceRepository;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository documentVersionRepository;

    @Autowired
    private KbDocumentParseTaskRepository parseTaskRepository;

    @BeforeEach
    void cleanDatabase() {
        parseTaskRepository.deleteAll();
        documentVersionRepository.deleteAll();
        documentRepository.deleteAll();
        spaceRepository.deleteAll();
    }

    @Test
    void createsAndListsKnowledgeSpaces() throws Exception {
        mockMvc.perform(post("/api/v1/kb-spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": 1001,
                                  "name": "Proposal Knowledge",
                                  "description": "Enterprise proposals and SOW files",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("Proposal Knowledge"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/kb-spaces").param("tenantId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Proposal Knowledge"));
    }

    @Test
    void uploadsSupportedDocumentAndCreatesVersionAndPendingParseTask() throws Exception {
        Long spaceId = createSpace();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "case-study.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "A useful proposal case study.".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(file)
                        .param("title", "Case Study")
                        .param("industry", "Education")
                        .param("serviceLine", "Consulting")
                        .param("confidentialLevel", "INTERNAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").isNumber())
                .andExpect(jsonPath("$.data.versionId").isNumber())
                .andExpect(jsonPath("$.data.parseTaskId").isNumber())
                .andExpect(jsonPath("$.data.docType").value("TXT"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.duplicated").value(false))
                .andExpect(jsonPath("$.data.sourceUri").value(org.hamcrest.Matchers.startsWith("local://")))
                .andReturn();

        Number documentId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.documentId");

        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(documentVersionRepository.count()).isEqualTo(1);
        assertThat(parseTaskRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/kb-spaces/{spaceId}/documents", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Case Study"));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Case Study"))
                .andExpect(jsonPath("$.data.currentVersion.parseStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/documents/{documentId}/parse-status", documentId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.taskType").value("PARSE_DOCUMENT"));
    }

    @Test
    void uploadDeduplicatesByFileHashWithinSpace() throws Exception {
        Long spaceId = createSpace();
        byte[] content = "Same file content".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile("file", "first.md", "text/markdown", content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicated").value(false));

        mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile("file", "second.md", "text/markdown", content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicated").value(true));

        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(documentVersionRepository.count()).isEqualTo(1);
        assertThat(parseTaskRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsUnsupportedDocumentType() throws Exception {
        Long spaceId = createSpace();

        mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "script.exe",
                                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                "not a supported document".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void deletesDocumentByMarkingItDeletedAndExcludesItFromList() throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "delete-me.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "%PDF-1.7".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();

        Number documentId = JsonPath.read(upload.getResponse().getContentAsString(), "$.data.documentId");

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        mockMvc.perform(get("/api/v1/kb-spaces/{spaceId}/documents", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    private Long createSpace() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/kb-spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": 1001,
                                  "name": "Delivery Docs"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }
}
