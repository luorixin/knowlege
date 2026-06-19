package com.sunxin.knowledge.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.sunxin.knowledge.integration.ai.AiPipelineClient;
import com.sunxin.knowledge.integration.ai.AiServiceException;
import com.sunxin.knowledge.integration.ai.DocumentParseResponse;
import com.sunxin.knowledge.integration.ai.PageParseError;
import com.sunxin.knowledge.integration.ai.ParsedBlock;
import com.sunxin.knowledge.integration.ai.ParsedPage;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.task.domain.TaskStatus;

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

    @Autowired
    private KbDocumentChunkRepository chunkRepository;

    @Autowired
    private KbEmbeddingIndexTaskRepository embeddingIndexTaskRepository;

    @MockBean
    private AiPipelineClient aiPipelineClient;

    @BeforeEach
    void cleanDatabase() {
        embeddingIndexTaskRepository.deleteAll();
        chunkRepository.deleteAll();
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
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.name").value("Proposal Knowledge"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/kb-spaces").param("tenantId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Proposal Knowledge"));
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
                .andExpect(jsonPath("$.data.documentId").isString())
                .andExpect(jsonPath("$.data.versionId").isString())
                .andExpect(jsonPath("$.data.parseTaskId").isString())
                .andExpect(jsonPath("$.data.docType").value("TXT"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.duplicated").value(false))
                .andExpect(jsonPath("$.data.sourceUri").value(org.hamcrest.Matchers.startsWith("local://")))
                .andReturn();

        Long documentId = readLong(result, "$.data.documentId");

        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(documentVersionRepository.count()).isEqualTo(1);
        assertThat(parseTaskRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/kb-spaces/{spaceId}/documents", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Case Study"));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Case Study"))
                .andExpect(jsonPath("$.data.currentVersion.parseStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/documents/{documentId}/parse-status", documentId))
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

        Long documentId = readLong(upload, "$.data.documentId");

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        mockMvc.perform(get("/api/v1/kb-spaces/{spaceId}/documents", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));
    }

    @Test
    void runsPendingParseTaskThroughAiServiceAndCreatesChunksAndEmbeddingTasks() throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "parse-me.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "raw document".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();
        Long taskId = readLong(upload, "$.data.parseTaskId");

        MvcResult initialTasks = mockMvc.perform(get("/api/v1/tasks/center")
                        .param("spaceId", String.valueOf(spaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].task_key").value("parse-" + taskId))
                .andExpect(jsonPath("$.data.content[0].task_category").value("PARSE_CHUNK"))
                .andExpect(jsonPath("$.data.content[0].task_type").value("PARSE_AND_CHUNK"))
                .andExpect(jsonPath("$.data.content[0].stage_label").value("文档解析 / 内容切片"))
                .andReturn();
        assertThat(readStringList(initialTasks, "$.data.content[*].document_title")).contains("parse-me.txt");

        when(aiPipelineClient.parseDocument(any())).thenReturn(new DocumentParseResponse(
                "doc",
                "version",
                java.util.List.of(new ParsedPage(
                        1,
                        "项目背景",
                        "text",
                        "解析后的项目背景内容，包含金融行业数据治理案例。",
                        java.util.Map.of("parser", "mock-test")
                ))
        ));

        mockMvc.perform(post("/api/v1/tasks/center/{taskKey}/run", "parse-" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.progress_percent").value(100))
                .andExpect(jsonPath("$.data.task_category").value("PARSE_CHUNK"));

        assertThat(chunkRepository.count()).isEqualTo(1);
        assertThat(embeddingIndexTaskRepository.count()).isEqualTo(1);

        Long embeddingTaskId = embeddingIndexTaskRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/api/v1/tasks/embedding")
                        .param("spaceId", String.valueOf(spaceId))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].chunk_id").isString());

        MvcResult unifiedTasks = mockMvc.perform(get("/api/v1/tasks/center")
                        .param("spaceId", String.valueOf(spaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andReturn();
        assertThat(readStringList(unifiedTasks, "$.data.content[*].task_category"))
                .contains("PARSE_CHUNK", "EMBEDDING_INDEX");
        assertThat(readStringList(unifiedTasks, "$.data.content[*].task_type"))
                .contains("PARSE_AND_CHUNK", "EMBEDDING_AND_INDEX");

        mockMvc.perform(post("/api/v1/tasks/embedding/{taskId}/run", embeddingTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.progress_percent").value(100))
                .andExpect(jsonPath("$.data.embedding_dimension").value(16))
                .andExpect(jsonPath("$.data.model_provider").value("mock"))
                .andExpect(jsonPath("$.data.model_name").value("mock-embedding-v1"));

        Long documentId = readLong(upload, "$.data.documentId");
        mockMvc.perform(get("/api/v1/documents/{documentId}/parse-status", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.parseStatus").value("COMPLETED"));
    }

    @Test
    void parseTaskUsesBlocksBeforePagesAndPreservesPartialSuccessMetadata() throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "partial-ppt.pptx",
                                MediaType.TEXT_PLAIN_VALUE,
                                "raw deck".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();
        Long taskId = readLong(upload, "$.data.parseTaskId");
        Long documentId = readLong(upload, "$.data.documentId");
        Long versionId = readLong(upload, "$.data.versionId");

        when(aiPipelineClient.parseDocument(any())).thenReturn(new DocumentParseResponse(
                "doc",
                "version",
                "PARTIAL_SUCCESS",
                java.util.List.of(new ParsedPage(
                        1,
                        "fallback-page",
                        "text",
                        "page fallback should not be chunked",
                        java.util.Map.of("parser", "page")
                )),
                java.util.List.of(new ParsedBlock(
                        "block-1",
                        "figure",
                        3,
                        "解决方案",
                        "slide summary from block",
                        "slide summary from block",
                        null,
                        java.util.List.of(1.0, 2.0, 3.0, 4.0),
                        0.91,
                        "local://deck.pptx#slide-3",
                        "local://deck.pptx",
                        java.util.Map.of("slide_summary", "方案摘要", "parser", "PptParser")
                )),
                java.util.List.of(new PageParseError(
                        4,
                        "PAGE_PARSE_FAILED",
                        "broken slide",
                        java.util.Map.of("slide_path", "ppt/slides/slide4.xml")
                )),
                "slide summary from block",
                java.util.Map.of("parser", "PptParser", "page_count", 1, "block_count", 1)
        ));

        mockMvc.perform(post("/api/v1/tasks/center/{taskKey}/run", "parse-" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.data.progress_percent").value(100));

        KbDocumentChunk chunk = chunkRepository.findAll().getFirst();
        assertThat(chunk.getDocId()).isEqualTo(documentId);
        assertThat(chunk.getVersionId()).isEqualTo(versionId);
        assertThat(chunk.getContent()).contains("slide summary from block");
        assertThat(chunk.getContent()).doesNotContain("page fallback should not be chunked");
        assertThat(chunk.getPageNo()).isEqualTo(3);
        assertThat(chunk.getSectionTitle()).isEqualTo("解决方案");
        assertThat(chunk.getMetadataJson()).contains("\"block_type\":\"figure\"");
        assertThat(chunk.getMetadataJson()).contains("\"image_uri\":\"local://deck.pptx#slide-3\"");
        assertThat(chunk.getMetadataJson()).contains("\"confidence\":0.91");

        KbDocumentParseTask task = parseTaskRepository.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PARTIAL_SUCCESS);
        assertThat(task.getMetadataJson()).contains("\"parser\":\"PptParser\"");
        assertThat(task.getMetadataJson()).contains("\"error_count\":1");
        assertThat(task.getMetadataJson()).contains("PAGE_PARSE_FAILED");

        KbDocumentVersion version = documentVersionRepository.findById(versionId).orElseThrow();
        assertThat(version.getParseStatus()).isEqualTo("PARTIAL_SUCCESS");
    }

    @Test
    void parseTaskKeepsMarkdownOnlyTableBlocks() throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "table-only.xlsx",
                                MediaType.TEXT_PLAIN_VALUE,
                                "raw workbook".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();
        Long taskId = readLong(upload, "$.data.parseTaskId");

        when(aiPipelineClient.parseDocument(any())).thenReturn(new DocumentParseResponse(
                "doc",
                "version",
                "SUCCESS",
                java.util.List.of(new ParsedPage(
                        1,
                        "fallback-page",
                        "text",
                        "fallback page should not be used when blocks exist",
                        java.util.Map.of("parser", "page")
                )),
                java.util.List.of(new ParsedBlock(
                        "table-1",
                        "table",
                        2,
                        "收入明细",
                        "",
                        "| 科目 | 金额 |\n| --- | --- |\n| 收入 | 1200 |",
                        null,
                        null,
                        1.0,
                        null,
                        "doc:doc/version:version/page:2",
                        java.util.Map.of("parser", "ExcelParser", "sheet_name", "收入明细")
                )),
                java.util.List.of(),
                "| 科目 | 金额 |",
                java.util.Map.of("parser", "ExcelParser", "page_count", 1, "block_count", 1)
        ));

        mockMvc.perform(post("/api/v1/tasks/center/{taskKey}/run", "parse-" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        KbDocumentChunk chunk = chunkRepository.findAll().getFirst();
        assertThat(chunk.getContent()).contains("| 科目 | 金额 |");
        assertThat(chunk.getContent()).doesNotContain("fallback page should not be used");
        assertThat(chunk.getMetadataJson()).contains("\"block_type\":\"table\"");
        assertThat(chunk.getMetadataJson()).contains("\"sheet_name\":\"收入明细\"");
    }

    @Test
    void failedAiParseResponsePersistsStructuredMetadata() throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "failed-scan.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "%PDF-1.7".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();
        Long taskId = readLong(upload, "$.data.parseTaskId");
        Long documentId = readLong(upload, "$.data.documentId");

        when(aiPipelineClient.parseDocument(any())).thenReturn(new DocumentParseResponse(
                "doc",
                "version",
                "FAILED",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(new PageParseError(
                        1,
                        "OCR_RECOGNIZE_FAILED",
                        "page 1 corrupt",
                        java.util.Map.of("page_parse_mode", "scanned_image")
                )),
                "",
                java.util.Map.of("parser", "OCRParser", "page_count", 0, "block_count", 0)
        ));

        mockMvc.perform(post("/api/v1/tasks/center/{taskKey}/run", "parse-" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.metadata.parser").value("OCRParser"))
                .andExpect(jsonPath("$.data.metadata.error_count").value(1));

        mockMvc.perform(get("/api/v1/documents/{documentId}/parse-status", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.metadata.parser").value("OCRParser"))
                .andExpect(jsonPath("$.data.metadata.errors", hasSize(1)));

        KbDocumentParseTask task = parseTaskRepository.findById(taskId).orElseThrow();
        assertThat(task.getMetadataJson()).contains("\"parser\":\"OCRParser\"");
        assertThat(task.getMetadataJson()).contains("OCR_RECOGNIZE_FAILED");
    }

    @Test
    void failedParseTaskCanBeRetried() throws Exception {
        Long spaceId = createSpace();
        MvcResult upload = mockMvc.perform(multipart("/api/v1/kb-spaces/{spaceId}/documents", spaceId)
                        .file(new MockMultipartFile(
                                "file",
                                "fail-me.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "raw document".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andReturn();
        Long taskId = readLong(upload, "$.data.parseTaskId");

        when(aiPipelineClient.parseDocument(any()))
                .thenThrow(new AiServiceException("AI service unavailable", new RuntimeException("down")));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/run", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.error_code").value("AiServiceException"));

        mockMvc.perform(post("/api/v1/tasks/center/{taskKey}/retry", "parse-" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.retry_count").value(1))
                .andExpect(jsonPath("$.data.error_code").doesNotExist());
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

        return readLong(result, "$.data.id");
    }

    private static Long readLong(MvcResult result, String path) throws Exception {
        String value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return Long.valueOf(value);
    }

    private static List<String> readStringList(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
