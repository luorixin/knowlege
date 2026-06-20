package com.sunxin.knowledge.task;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.document.cleaning.DocumentCleaningContext;
import com.sunxin.knowledge.document.cleaning.DocumentCleaningService;
import com.sunxin.knowledge.document.cleaning.DocumentCleaningService.CleanedDocumentResult;
import com.sunxin.knowledge.document.application.DocumentChunkingService;
import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.document.storage.LocalStoredFileResolver;
import com.sunxin.knowledge.document.support.DocumentType;
import com.sunxin.knowledge.integration.ai.AiPipelineClient;
import com.sunxin.knowledge.integration.ai.DocumentParseRequest;
import com.sunxin.knowledge.integration.ai.DocumentParseResponse;
import com.sunxin.knowledge.integration.ai.ParsedBlock;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.task.dto.ParseTaskResponse;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.task.domain.TaskStatus;

@Service
@EnableConfigurationProperties(TaskExecutionProperties.class)
public class DocumentParseTaskExecutionService {

    private static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final int RUNNING_PROGRESS = 10;

    private static final Logger log = LoggerFactory.getLogger(DocumentParseTaskExecutionService.class);

    private final KbDocumentParseTaskRepository taskRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentVersionRepository versionRepository;
    private final LocalStoredFileResolver localStoredFileResolver;
    private final AiPipelineClient aiPipelineClient;
    private final DocumentChunkingService chunkingService;
    private final TaskExecutionProperties properties;
    private final ObjectMapper objectMapper;

    private final DocumentCleaningService cleaningService;

    public DocumentParseTaskExecutionService(
            KbDocumentParseTaskRepository taskRepository,
            KbDocumentRepository documentRepository,
            KbDocumentVersionRepository versionRepository,
            LocalStoredFileResolver localStoredFileResolver,
            AiPipelineClient aiPipelineClient,
            DocumentChunkingService chunkingService,
                                             DocumentCleaningService cleaningService,
            TaskExecutionProperties properties,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.localStoredFileResolver = localStoredFileResolver;
        this.aiPipelineClient = aiPipelineClient;
        this.chunkingService = chunkingService;
        this.cleaningService = cleaningService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<ParseTaskResponse> processNextPending() {
        return taskRepository.findFirstByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus.PENDING)
                .map(task -> process(task.getId()));
    }

    public ParseTaskResponse process(Long taskId) {
        long startedAt = System.currentTimeMillis();
        KbDocumentParseTask task = markRunning(taskId);
        long docId = task.getDocId();
        long versionId = task.getVersionId();
        log.info("parse_task_start task_id={} doc_id={} version_id={} worker_id={}",
                taskId, docId, versionId, properties.getWorkerId());
        try {
            KbDocument document = documentRepository.findById(docId)
                    .orElseThrow(() -> new NotFoundException("Document not found"));
            KbDocumentVersion version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new NotFoundException("Document version not found"));
            DocumentParseResponse parseResponse = aiPipelineClient.parseDocument(new DocumentParseRequest(
                    String.valueOf(document.getId()),
                    String.valueOf(version.getId()),
                    localStoredFileResolver.resolve(version.getSourceUri()).toString(),
                    fileType(version)
            ));
            if ("FAILED".equals(parseResponse.status())) {
                markParseTaskResult(taskId, parseResponse);
                long elapsedMs = System.currentTimeMillis() - startedAt;
                log.warn("parse_task_complete task_id={} doc_id={} version_id={} status={} elapsed_ms={}",
                        taskId, docId, versionId, normalizedAiStatus(parseResponse), elapsedMs);
                return ParseTaskResponse.fromEntity(taskRepository.findById(taskId).orElseThrow());
            }
            RebuildChunksRequest rawRequest = toRebuildChunksRequest(parseResponse);
            DocumentCleaningContext context = new DocumentCleaningContext(docId, versionId, fileType(version));
            com.sunxin.knowledge.document.cleaning.DocumentCleaningService.CleanedDocumentResult cleanedResult = cleaningService.clean(rawRequest.pages(), context);
            java.util.List<com.sunxin.knowledge.document.dto.ParsedPageRequest> cleanedPages = cleanedResult.pages();
            if (!cleanedPages.isEmpty()) {
                com.sunxin.knowledge.document.dto.ParsedPageRequest firstPage = cleanedPages.get(0);
                java.util.Map<String, Object> newMeta = new java.util.LinkedHashMap<>();
                if (firstPage.metadata() != null) {
                    newMeta.putAll(firstPage.metadata());
                }
                try {
                    String reportJson = objectMapper.writeValueAsString(cleanedResult.report());
                    newMeta.put("cleaning_report", reportJson);
                    cleanedPages.set(0, new com.sunxin.knowledge.document.dto.ParsedPageRequest(firstPage.pageNo(), firstPage.sectionTitle(), firstPage.contentType(), firstPage.content(), newMeta));
                } catch (Exception e) {
                    log.error("Failed to serialize cleaning report", e);
                }
            }
            RebuildChunksRequest rebuildRequest = new RebuildChunksRequest(null, null, cleanedPages);
            chunkingService.rebuildChunksFromPipeline(document.getId(), rebuildRequest, actorUserId(task));
            markParseTaskResult(taskId, parseResponse);
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("parse_task_complete task_id={} doc_id={} version_id={} status={} elapsed_ms={}",
                    taskId, docId, versionId, normalizedAiStatus(parseResponse), elapsedMs);
            return ParseTaskResponse.fromEntity(taskRepository.findById(taskId).orElseThrow());
        } catch (RuntimeException ex) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.warn("parse_task_failed task_id={} doc_id={} version_id={} error_code={} elapsed_ms={}",
                    taskId, docId, versionId, ex.getClass().getSimpleName(), elapsedMs);
            return markFailed(taskId, ex);
        }
    }

    @Transactional
    public ParseTaskResponse retry(Long taskId) {
        KbDocumentParseTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        if (task.getStatus() != TaskStatus.FAILED) {
            throw new BadRequestException("Only FAILED parse tasks can be retried");
        }
        task.setStatus(TaskStatus.PENDING);
        task.setProgressPercent(0);
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        task.setWorkerId(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        return ParseTaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public PageResponse<ParseTaskResponse> list(Long spaceId, String status, int page, int size) {
        Page<KbDocumentParseTask> taskPage = taskRepository.findBySpaceIdAndOptionalStatus(
                spaceId,
                status != null && !status.isBlank() ? TaskStatus.valueOf(status.trim()) : null,
                PageRequest.of(page, Math.min(Math.max(size, 1), 200))
        );
        return new PageResponse<>(
                taskPage.getContent().stream().map(ParseTaskResponse::fromEntity).toList(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages()
        );
    }

    @org.springframework.kafka.annotation.KafkaListener(topics = KafkaConfig.TOPIC_PARSE_TASKS)
    public void processFromKafka(String taskIdStr) {
        if (properties.isAutoRun()) {
            try {
                Long taskId = Long.valueOf(taskIdStr);
                process(taskId);
            } catch (Exception e) {
                log.error("Error processing parse task from Kafka: {}", taskIdStr, e);
            }
        }
    }

    @Transactional
    protected KbDocumentParseTask markRunning(Long taskId) {
        KbDocumentParseTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.RUNNING) {
            throw new BadRequestException("Only PENDING parse tasks can be processed");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.RUNNING);
        task.setProgressPercent(RUNNING_PROGRESS);
        task.setStartedAt(now);
        task.setFinishedAt(null);
        task.setWorkerId(properties.getWorkerId());
        task.setErrorCode(null);
        task.setErrorMessage(null);

        KbDocumentVersion version = versionRepository.findById(task.getVersionId())
                .orElseThrow(() -> new NotFoundException("Document version not found"));
        version.setParseStatus("RUNNING");
        versionRepository.save(version);
        return taskRepository.save(task);
    }

    @Transactional
    protected ParseTaskResponse markFailed(Long taskId, RuntimeException ex) {
        KbDocumentParseTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        task.setStatus(TaskStatus.FAILED);
        task.setProgressPercent(100);
        task.setFinishedAt(LocalDateTime.now());
        task.setErrorCode(ex.getClass().getSimpleName());
        task.setErrorMessage(limitMessage(ex.getMessage()));
        taskRepository.save(task);

        versionRepository.findById(task.getVersionId()).ifPresent(version -> {
            version.setParseStatus("FAILED");
            versionRepository.save(version);
        });
        return ParseTaskResponse.fromEntity(task);
    }

    private static RebuildChunksRequest toRebuildChunksRequest(DocumentParseResponse response) {
        if (response == null) {
            throw new BadRequestException("AI parser returned no response");
        }
        if (response.blocks() != null && !response.blocks().isEmpty()) {
            List<ParsedPageRequest> pages = response.blocks().stream()
                    .filter(block -> hasText(blockContent(block)))
                    .map(block -> new ParsedPageRequest(
                            block.pageNo(),
                            block.sectionTitle(),
                            blockContentType(block),
                            blockContent(block),
                            blockMetadata(block)
                    ))
                    .toList();
            if (!pages.isEmpty()) {
                return new RebuildChunksRequest(null, null, pages);
            }
        }
        if (response.pages() == null || response.pages().isEmpty()) {
            throw new BadRequestException("AI parser returned no pages");
        }
        List<ParsedPageRequest> pages = response.pages().stream()
                .filter(page -> page.content() != null && !page.content().isBlank())
                .map(page -> new ParsedPageRequest(
                        page.pageNo(),
                        page.sectionTitle(),
                        page.contentType(),
                        page.content(),
                        page.metadata() == null ? Map.of() : page.metadata()
                ))
                .toList();
        if (pages.isEmpty()) {
            throw new BadRequestException("AI parser returned only blank pages");
        }
        return new RebuildChunksRequest(null, null, pages);
    }

    @Transactional
    protected void markParseTaskResult(Long taskId, DocumentParseResponse response) {
        String status = normalizedAiStatus(response);
        KbDocumentParseTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        task.setStatus(TaskStatus.valueOf(status));
        task.setProgressPercent(100);
        if (task.getFinishedAt() == null) {
            task.setFinishedAt(LocalDateTime.now());
        }
        task.setErrorCode(errorCode(status));
        task.setErrorMessage(errorMessage(status, response));
        task.setMetadataJson(toJson(parseMetadata(response)));
        taskRepository.save(task);

        versionRepository.findById(task.getVersionId()).ifPresent(version -> {
            version.setParseStatus(status);
            versionRepository.save(version);
        });
    }

    private static String normalizedAiStatus(DocumentParseResponse response) {
        if (response == null || response.status() == null || response.status().isBlank()) {
            return "COMPLETED";
        }
        return switch (response.status()) {
            case "SUCCESS" -> "COMPLETED";
            case "PARTIAL_SUCCESS" -> "PARTIAL_SUCCESS";
            case "FAILED" -> "FAILED";
            default -> "COMPLETED";
        };
    }

    private static String blockContent(ParsedBlock block) {
        if ("table".equalsIgnoreCase(block.blockType()) && block.markdown() != null && !block.markdown().isBlank()) {
            return block.markdown();
        }
        if ((block.content() == null || block.content().isBlank())
                && block.markdown() != null
                && !block.markdown().isBlank()) {
            return block.markdown();
        }
        return block.content();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String errorCode(String status) {
        if (PARTIAL_SUCCESS.equals(status)) {
            return "PARTIAL_SUCCESS";
        }
        if ("FAILED".equals(status)) {
            return "PARSE_FAILED";
        }
        return null;
    }

    private static String errorMessage(String status, DocumentParseResponse response) {
        if (PARTIAL_SUCCESS.equals(status)) {
            return "Some pages failed to parse";
        }
        if ("FAILED".equals(status)) {
            if (response.errors() != null && !response.errors().isEmpty()) {
                return limitMessage(response.errors().getFirst().message());
            }
            return "AI parser returned FAILED status";
        }
        return null;
    }

    private static String blockContentType(ParsedBlock block) {
        if (block.blockType() == null || block.blockType().isBlank()) {
            return "text";
        }
        return block.blockType().trim().toLowerCase();
    }

    private static Map<String, Object> blockMetadata(ParsedBlock block) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (block.metadata() != null) {
            metadata.putAll(block.metadata());
        }
        metadata.put("block_id", block.blockId());
        metadata.put("block_type", block.blockType());
        metadata.put("source_uri", block.sourceUri());
        if (block.bbox() != null) {
            metadata.put("bbox", block.bbox());
        }
        if (block.confidence() != null) {
            metadata.put("confidence", block.confidence());
        }
        if (block.imageUri() != null) {
            metadata.put("image_uri", block.imageUri());
        }
        if (block.markdown() != null && !block.markdown().isBlank()) {
            metadata.put("markdown", block.markdown());
        }
        return metadata;
    }

    private static Map<String, Object> parseMetadata(DocumentParseResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ai_status", response.status());
        metadata.put("page_count", response.pages() == null ? 0 : response.pages().size());
        metadata.put("block_count", response.blocks() == null ? 0 : response.blocks().size());
        metadata.put("error_count", response.errors() == null ? 0 : response.errors().size());
        if (response.metadata() != null && !response.metadata().isEmpty()) {
            metadata.putAll(response.metadata());
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            metadata.put("errors", response.errors());
        }
        return metadata;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Parse task metadata cannot be serialized");
        }
    }

    private static Long actorUserId(KbDocumentParseTask task) {
        return task.getCreatedBy() == null ? 0L : task.getCreatedBy();
    }

    private static String fileType(KbDocumentVersion version) {
        String source = version.getSourceUri() == null ? version.getStorageUri() : version.getSourceUri();
        return DocumentType.extensionOf(source)
                .orElseThrow(() -> new BadRequestException("Document file type cannot be inferred"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String limitMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Document parse task failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
