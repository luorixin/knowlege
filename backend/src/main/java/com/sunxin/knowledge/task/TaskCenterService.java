package com.sunxin.knowledge.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;
import com.sunxin.knowledge.task.dto.UnifiedTaskResponse;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.task.domain.TaskStatus;

@Service
public class TaskCenterService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final String TYPE_PARSE_CHUNK = "PARSE_CHUNK";
    private static final String TYPE_EMBEDDING_INDEX = "EMBEDDING_INDEX";

    private final KbDocumentParseTaskRepository parseTaskRepository;
    private final KbEmbeddingIndexTaskRepository embeddingTaskRepository;
    private final KbDocumentRepository documentRepository;
    private final DocumentParseTaskExecutionService parseTaskExecutionService;
    private final EmbeddingIndexTaskExecutionService embeddingTaskExecutionService;

    public TaskCenterService(
            KbDocumentParseTaskRepository parseTaskRepository,
            KbEmbeddingIndexTaskRepository embeddingTaskRepository,
            KbDocumentRepository documentRepository,
            DocumentParseTaskExecutionService parseTaskExecutionService,
            EmbeddingIndexTaskExecutionService embeddingTaskExecutionService
    ) {
        this.parseTaskRepository = parseTaskRepository;
        this.embeddingTaskRepository = embeddingTaskRepository;
        this.documentRepository = documentRepository;
        this.parseTaskExecutionService = parseTaskExecutionService;
        this.embeddingTaskExecutionService = embeddingTaskExecutionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UnifiedTaskResponse> list(Long spaceId, String status, String taskCategory, int page, int size) {
        int safeLimit = clampLimit(size);
        TaskStatus parsedStatus = status != null && !status.isBlank() ? TaskStatus.valueOf(status.trim()) : null;
        String normalizedCategory = normalizeCategory(taskCategory);
        List<Object> rawTasks = new ArrayList<>();
        PageRequest pageRequest = PageRequest.of(page, safeLimit);
        
        long totalElements = 0;
        if (normalizedCategory == null || TYPE_PARSE_CHUNK.equals(normalizedCategory)) {
            Page<KbDocumentParseTask> p = parseTaskRepository.findBySpaceIdAndOptionalStatus(spaceId, parsedStatus, pageRequest);
            rawTasks.addAll(p.getContent());
            totalElements += p.getTotalElements();
        }
        if (normalizedCategory == null || TYPE_EMBEDDING_INDEX.equals(normalizedCategory)) {
            Page<KbEmbeddingIndexTask> p = embeddingTaskRepository.findBySpaceIdAndOptionalStatus(spaceId, parsedStatus, pageRequest);
            rawTasks.addAll(p.getContent());
            totalElements += p.getTotalElements();
        }

        Map<Long, KbDocument> documents = loadDocuments(rawTasks);
        List<UnifiedTaskResponse> results = rawTasks.stream()
                .map(task -> toUnifiedTask(task, documents))
                .sorted(Comparator.comparing(TaskCenterService::sortTime).reversed())
                .limit(safeLimit)
                .toList();
                
        return new PageResponse<>(results, page, safeLimit, totalElements, (int) Math.ceil((double) totalElements / safeLimit));
    }

    public UnifiedTaskResponse run(String taskKey) {
        TaskRef ref = parseTaskKey(taskKey);
        if (TYPE_PARSE_CHUNK.equals(ref.category())) {
            parseTaskExecutionService.process(ref.id());
            return parseTaskRepository.findById(ref.id())
                    .map(task -> UnifiedTaskResponse.fromParseTask(task, document(task.getDocId())))
                    .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        }
        embeddingTaskExecutionService.process(ref.id());
        return embeddingTaskRepository.findById(ref.id())
                .map(task -> UnifiedTaskResponse.fromEmbeddingTask(task, document(task.getDocId())))
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
    }

    public UnifiedTaskResponse retry(String taskKey) {
        TaskRef ref = parseTaskKey(taskKey);
        if (TYPE_PARSE_CHUNK.equals(ref.category())) {
            parseTaskExecutionService.retry(ref.id());
            return parseTaskRepository.findById(ref.id())
                    .map(task -> UnifiedTaskResponse.fromParseTask(task, document(task.getDocId())))
                    .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        }
        embeddingTaskExecutionService.retry(ref.id());
        return embeddingTaskRepository.findById(ref.id())
                .map(task -> UnifiedTaskResponse.fromEmbeddingTask(task, document(task.getDocId())))
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
    }

    @Transactional(readOnly = true)
    public TaskScope requireScope(String taskKey) {
        TaskRef ref = parseTaskKey(taskKey);
        if (TYPE_PARSE_CHUNK.equals(ref.category())) {
            KbDocumentParseTask task = parseTaskRepository.findById(ref.id())
                    .orElseThrow(() -> new NotFoundException("Document parse task not found"));
            return new TaskScope(task.getTenantId(), task.getSpaceId());
        }
        KbEmbeddingIndexTask task = embeddingTaskRepository.findById(ref.id())
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
        return new TaskScope(task.getTenantId(), task.getSpaceId());
    }

    private UnifiedTaskResponse toUnifiedTask(Object task, Map<Long, KbDocument> documents) {
        if (task instanceof KbDocumentParseTask parseTask) {
            return UnifiedTaskResponse.fromParseTask(parseTask, documents.get(parseTask.getDocId()));
        }
        if (task instanceof KbEmbeddingIndexTask embeddingTask) {
            return UnifiedTaskResponse.fromEmbeddingTask(embeddingTask, documents.get(embeddingTask.getDocId()));
        }
        throw new IllegalArgumentException("Unsupported task type: " + task.getClass().getName());
    }

    private Map<Long, KbDocument> loadDocuments(List<Object> rawTasks) {
        Set<Long> docIds = rawTasks.stream()
                .map(TaskCenterService::docId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (docIds.isEmpty()) {
            return Map.of();
        }
        return documentRepository.findAllById(docIds).stream()
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));
    }

    private KbDocument document(Long docId) {
        if (docId == null) {
            return null;
        }
        return documentRepository.findById(docId).orElse(null);
    }

    private static Long docId(Object task) {
        if (task instanceof KbDocumentParseTask parseTask) {
            return parseTask.getDocId();
        }
        if (task instanceof KbEmbeddingIndexTask embeddingTask) {
            return embeddingTask.getDocId();
        }
        return null;
    }

    private static LocalDateTime sortTime(UnifiedTaskResponse task) {
        if (task.updatedAt() != null) {
            return task.updatedAt();
        }
        if (task.createdAt() != null) {
            return task.createdAt();
        }
        return LocalDateTime.MIN;
    }

    private static int clampLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private static String normalizeCategory(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        if ("PARSE".equals(normalized) || "PARSE_AND_CHUNK".equals(normalized)) {
            return TYPE_PARSE_CHUNK;
        }
        if ("EMBEDDING".equals(normalized) || "INDEX".equals(normalized) || "EMBEDDING_AND_INDEX".equals(normalized)) {
            return TYPE_EMBEDDING_INDEX;
        }
        if (TYPE_PARSE_CHUNK.equals(normalized) || TYPE_EMBEDDING_INDEX.equals(normalized)) {
            return normalized;
        }
        throw new BadRequestException("Unsupported task category: " + value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static TaskRef parseTaskKey(String taskKey) {
        String normalized = blankToNull(taskKey);
        if (normalized == null) {
            throw new BadRequestException("Task key is required");
        }
        if (normalized.startsWith("parse-")) {
            return new TaskRef(TYPE_PARSE_CHUNK, parseId(normalized.substring("parse-".length())));
        }
        if (normalized.startsWith("embedding-")) {
            return new TaskRef(TYPE_EMBEDDING_INDEX, parseId(normalized.substring("embedding-".length())));
        }
        throw new BadRequestException("Unsupported task key: " + taskKey);
    }

    private static Long parseId(String rawId) {
        try {
            return Long.valueOf(rawId);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid task key");
        }
    }

    private record TaskRef(String category, Long id) {
    }

    public record TaskScope(Long tenantId, Long spaceId) {
    }
}
