package com.sunxin.knowledge.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.document.application.DocumentChunkingService;
import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.document.storage.LocalStoredFileResolver;
import com.sunxin.knowledge.document.support.DocumentType;
import com.sunxin.knowledge.integration.ai.AiPipelineClient;
import com.sunxin.knowledge.integration.ai.DocumentParseRequest;
import com.sunxin.knowledge.integration.ai.DocumentParseResponse;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.task.dto.ParseTaskResponse;

@Service
@EnableConfigurationProperties(TaskExecutionProperties.class)
public class DocumentParseTaskExecutionService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String FAILED = "FAILED";
    private static final int RUNNING_PROGRESS = 10;

    private static final Logger log = LoggerFactory.getLogger(DocumentParseTaskExecutionService.class);

    private final KbDocumentParseTaskRepository taskRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentVersionRepository versionRepository;
    private final LocalStoredFileResolver localStoredFileResolver;
    private final AiPipelineClient aiPipelineClient;
    private final DocumentChunkingService chunkingService;
    private final TaskExecutionProperties properties;

    public DocumentParseTaskExecutionService(
            KbDocumentParseTaskRepository taskRepository,
            KbDocumentRepository documentRepository,
            KbDocumentVersionRepository versionRepository,
            LocalStoredFileResolver localStoredFileResolver,
            AiPipelineClient aiPipelineClient,
            DocumentChunkingService chunkingService,
            TaskExecutionProperties properties
    ) {
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.localStoredFileResolver = localStoredFileResolver;
        this.aiPipelineClient = aiPipelineClient;
        this.chunkingService = chunkingService;
        this.properties = properties;
    }

    public Optional<ParseTaskResponse> processNextPending() {
        return taskRepository.findFirstByStatusOrderByPriorityDescCreatedAtAsc(PENDING)
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
            RebuildChunksRequest rebuildRequest = toRebuildChunksRequest(parseResponse);
            chunkingService.rebuildChunksFromPipeline(document.getId(), rebuildRequest, actorUserId(task));
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("parse_task_complete task_id={} doc_id={} version_id={} status=COMPLETED elapsed_ms={}",
                    taskId, docId, versionId, elapsedMs);
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
        if (!FAILED.equals(task.getStatus())) {
            throw new BadRequestException("Only FAILED parse tasks can be retried");
        }
        task.setStatus(PENDING);
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
    public List<ParseTaskResponse> list(Long spaceId, String status, int limit) {
        return taskRepository.findBySpaceIdAndOptionalStatus(
                        spaceId,
                        blankToNull(status),
                        PageRequest.of(0, Math.min(Math.max(limit, 1), 200))
                )
                .stream()
                .map(ParseTaskResponse::fromEntity)
                .toList();
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
        if (!PENDING.equals(task.getStatus()) && !RUNNING.equals(task.getStatus())) {
            throw new BadRequestException("Only PENDING parse tasks can be processed");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(RUNNING);
        task.setProgressPercent(RUNNING_PROGRESS);
        task.setStartedAt(now);
        task.setFinishedAt(null);
        task.setWorkerId(properties.getWorkerId());
        task.setErrorCode(null);
        task.setErrorMessage(null);

        KbDocumentVersion version = versionRepository.findById(task.getVersionId())
                .orElseThrow(() -> new NotFoundException("Document version not found"));
        version.setParseStatus(RUNNING);
        versionRepository.save(version);
        return taskRepository.save(task);
    }

    @Transactional
    protected ParseTaskResponse markFailed(Long taskId, RuntimeException ex) {
        KbDocumentParseTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        task.setStatus(FAILED);
        task.setProgressPercent(100);
        task.setFinishedAt(LocalDateTime.now());
        task.setErrorCode(ex.getClass().getSimpleName());
        task.setErrorMessage(limitMessage(ex.getMessage()));
        taskRepository.save(task);

        versionRepository.findById(task.getVersionId()).ifPresent(version -> {
            version.setParseStatus(FAILED);
            versionRepository.save(version);
        });
        return ParseTaskResponse.fromEntity(task);
    }

    private static RebuildChunksRequest toRebuildChunksRequest(DocumentParseResponse response) {
        if (response == null || response.pages() == null || response.pages().isEmpty()) {
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
