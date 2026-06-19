package com.sunxin.knowledge.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.integration.embedding.EmbeddingProvider;
import com.sunxin.knowledge.integration.embedding.EmbeddingResult;
import com.sunxin.knowledge.integration.search.IndexedChunkDocument;
import com.sunxin.knowledge.integration.search.KeywordSearchClient;
import com.sunxin.knowledge.integration.vector.VectorRecord;
import com.sunxin.knowledge.integration.vector.VectorStoreClient;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;
import com.sunxin.knowledge.task.dto.EmbeddingIndexTaskResponse;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.task.domain.TaskStatus;

@Service
@EnableConfigurationProperties(EmbeddingTaskExecutionProperties.class)
public class EmbeddingIndexTaskExecutionService {

    private static final int RUNNING_PROGRESS = 40;

    private final KbEmbeddingIndexTaskRepository taskRepository;
    private final KbDocumentChunkRepository chunkRepository;
    private final KbDocumentRepository documentRepository;
    private final EmbeddingProvider embeddingProvider;
    private final KeywordSearchClient keywordSearchClient;
    private final VectorStoreClient vectorStoreClient;
    private final EmbeddingTaskExecutionProperties properties;

    public EmbeddingIndexTaskExecutionService(
            KbEmbeddingIndexTaskRepository taskRepository,
            KbDocumentChunkRepository chunkRepository,
            KbDocumentRepository documentRepository,
            EmbeddingProvider embeddingProvider,
            KeywordSearchClient keywordSearchClient,
            VectorStoreClient vectorStoreClient,
            EmbeddingTaskExecutionProperties properties
    ) {
        this.taskRepository = taskRepository;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingProvider = embeddingProvider;
        this.keywordSearchClient = keywordSearchClient;
        this.vectorStoreClient = vectorStoreClient;
        this.properties = properties;
    }

    public Optional<EmbeddingIndexTaskResponse> processNextPending() {
        return taskRepository.findFirstByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus.PENDING)
                .map(task -> process(task.getId()));
    }

    public EmbeddingIndexTaskResponse process(Long taskId) {
        KbEmbeddingIndexTask task = markRunning(taskId);
        try {
            KbDocumentChunk chunk = chunkRepository.findById(task.getChunkId())
                    .orElseThrow(() -> new NotFoundException("Document chunk not found"));
            KbDocument document = documentRepository.findById(task.getDocId())
                    .orElseThrow(() -> new NotFoundException("Document not found"));
            EmbeddingResult embedding = embeddingProvider.embed(chunk.getContent());
            keywordSearchClient.indexChunk(toIndexedDocument(task, document, chunk));
            vectorStoreClient.upsert(toVectorRecord(task, document, chunk, embedding));
            return markCompleted(taskId, embedding);
        } catch (RuntimeException ex) {
            return markFailed(taskId, ex);
        }
    }

    @Transactional
    public EmbeddingIndexTaskResponse retry(Long taskId) {
        KbEmbeddingIndexTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
        if (task.getStatus() != TaskStatus.FAILED) {
            throw new BadRequestException("Only FAILED embedding index tasks can be retried");
        }
        task.setStatus(TaskStatus.PENDING);
        task.setProgressPercent(0);
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        return EmbeddingIndexTaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmbeddingIndexTaskResponse> list(Long spaceId, String status, int page, int size) {
        Page<KbEmbeddingIndexTask> taskPage = taskRepository.findBySpaceIdAndOptionalStatus(
                spaceId,
                status != null && !status.isBlank() ? TaskStatus.valueOf(status.trim()) : null,
                PageRequest.of(page, Math.min(Math.max(size, 1), 200))
        );
        return new PageResponse<>(
                taskPage.getContent().stream().map(EmbeddingIndexTaskResponse::fromEntity).toList(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages()
        );
    }

    @org.springframework.kafka.annotation.KafkaListener(topics = KafkaConfig.TOPIC_EMBEDDING_TASKS)
    public void processFromKafka(String taskIdStr) {
        if (properties.isAutoRun()) {
            try {
                Long taskId = Long.valueOf(taskIdStr);
                process(taskId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(EmbeddingIndexTaskExecutionService.class)
                        .error("Error processing embedding task from Kafka: {}", taskIdStr, e);
            }
        }
    }

    @Transactional
    protected KbEmbeddingIndexTask markRunning(Long taskId) {
        KbEmbeddingIndexTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.RUNNING) {
            throw new BadRequestException("Only PENDING embedding index tasks can be processed");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.RUNNING);
        task.setProgressPercent(RUNNING_PROGRESS);
        task.setStartedAt(now);
        task.setFinishedAt(null);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        return taskRepository.save(task);
    }

    @Transactional
    protected EmbeddingIndexTaskResponse markCompleted(Long taskId, EmbeddingResult embedding) {
        KbEmbeddingIndexTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgressPercent(100);
        task.setFinishedAt(LocalDateTime.now());
        task.setModelProvider(embedding.modelProvider());
        task.setModelName(embedding.modelName());
        task.setEmbeddingDimension(embedding.dimension());
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setMetadataJson("""
                {"embedding_dimension":%d,"embedding_provider":"%s","keyword_engine":"%s","vector_store":"%s"}
                """.formatted(
                embedding.dimension(),
                embeddingProvider.providerName(),
                keywordSearchClient.engineName(),
                vectorStoreClient.storeName()
        ).strip());
        return EmbeddingIndexTaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    protected EmbeddingIndexTaskResponse markFailed(Long taskId, RuntimeException ex) {
        KbEmbeddingIndexTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
        task.setStatus(TaskStatus.FAILED);
        task.setProgressPercent(100);
        task.setFinishedAt(LocalDateTime.now());
        task.setErrorCode(ex.getClass().getSimpleName());
        task.setErrorMessage(limitMessage(ex.getMessage()));
        return EmbeddingIndexTaskResponse.fromEntity(taskRepository.save(task));
    }

    private static IndexedChunkDocument toIndexedDocument(
            KbEmbeddingIndexTask task,
            KbDocument document,
            KbDocumentChunk chunk
    ) {
        return new IndexedChunkDocument(
                task.getTenantId(),
                task.getSpaceId(),
                task.getDocId(),
                task.getVersionId(),
                chunk.getId(),
                document.getTitle(),
                document.getDocType(),
                document.getIndustry(),
                document.getServiceLine(),
                document.getCreatedAt(),
                chunk.getChunkIndex(),
                chunk.getPageNo(),
                chunk.getSectionTitle(),
                chunk.getContent(),
                chunk.getMetadataJson(),
                task.getIndexName()
        );
    }

    private static VectorRecord toVectorRecord(
            KbEmbeddingIndexTask task,
            KbDocument document,
            KbDocumentChunk chunk,
            EmbeddingResult embedding
    ) {
        return new VectorRecord(
                task.getTenantId(),
                task.getSpaceId(),
                task.getDocId(),
                task.getVersionId(),
                chunk.getId(),
                document.getTitle(),
                document.getDocType(),
                document.getIndustry(),
                document.getServiceLine(),
                task.getVectorCollection(),
                embedding.vector(),
                chunk.getMetadataJson()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String limitMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Embedding index task failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
