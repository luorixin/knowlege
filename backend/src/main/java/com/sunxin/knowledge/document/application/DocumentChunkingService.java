package com.sunxin.knowledge.document.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.document.chunking.ChunkDraft;
import com.sunxin.knowledge.document.chunking.DocumentChunker;
import com.sunxin.knowledge.document.dto.ChunkResponse;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.document.dto.RebuildChunksResponse;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.task.domain.TaskStatus;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;

@Service
public class DocumentChunkingService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DELETED = "DELETED";

    private final KbDocumentRepository documentRepository;
    private final KbDocumentVersionRepository versionRepository;
    private final KbDocumentChunkRepository chunkRepository;
    private final KbDocumentParseTaskRepository parseTaskRepository;
    private final KbEmbeddingIndexTaskRepository embeddingIndexTaskRepository;
    private final DocumentChunker documentChunker;
    private final DocumentDesensitizationService desensitizationService;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final AccessControlService accessControlService;
    private final com.sunxin.knowledge.task.TaskEventProducer taskEventProducer;
    private final com.sunxin.knowledge.integration.ai.AiServiceProperties aiProperties;

    public DocumentChunkingService(
            KbDocumentRepository documentRepository,
            KbDocumentVersionRepository versionRepository,
            KbDocumentChunkRepository chunkRepository,
            KbDocumentParseTaskRepository parseTaskRepository,
            KbEmbeddingIndexTaskRepository embeddingIndexTaskRepository,
            DocumentChunker documentChunker,
            DocumentDesensitizationService desensitizationService,
            ObjectMapper objectMapper,
            IdGenerator idGenerator,
            AccessControlService accessControlService,
            com.sunxin.knowledge.task.TaskEventProducer taskEventProducer,
            com.sunxin.knowledge.integration.ai.AiServiceProperties aiProperties
    ) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.chunkRepository = chunkRepository;
        this.parseTaskRepository = parseTaskRepository;
        this.embeddingIndexTaskRepository = embeddingIndexTaskRepository;
        this.documentChunker = documentChunker;
        this.desensitizationService = desensitizationService;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.accessControlService = accessControlService;
        this.taskEventProducer = taskEventProducer;
        this.aiProperties = aiProperties;
    }

    @Transactional
    public RebuildChunksResponse rebuildChunks(
            Long documentId,
            RebuildChunksRequest request,
            CurrentUser currentUser
    ) {
        KbDocument document = requireActiveDocument(documentId);
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.DOCUMENT_UPLOAD);
        KbDocumentVersion version = currentVersion(document);
        return rebuildChunksInternal(document, version, request, currentUser.userId());
    }

    @Transactional(readOnly = true)
    public List<ChunkResponse> listChunks(Long documentId, CurrentUser currentUser) {
        KbDocument document = requireActiveDocument(documentId);
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.DOCUMENT_READ);
        
        return chunkRepository.findByDocId(document.getId()).stream().map(chunk -> {
            String contentType = "text";
            try {
                if (chunk.getMetadataJson() != null && !chunk.getMetadataJson().isBlank()) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = objectMapper.readValue(chunk.getMetadataJson(), java.util.Map.class);
                    if (map.get("content_type") != null) {
                        contentType = String.valueOf(map.get("content_type"));
                    }
                }
            } catch (Exception e) {
                // Ignore parsing error, default to text
            }
            return ChunkResponse.fromEntity(chunk, contentType);
        }).toList();
    }

    @Transactional
    public RebuildChunksResponse rebuildChunksFromPipeline(
            Long documentId,
            RebuildChunksRequest request,
            Long actorUserId
    ) {
        KbDocument document = requireActiveDocument(documentId);
        KbDocumentVersion version = currentVersion(document);
        return rebuildChunksInternal(document, version, request, actorUserId);
    }

    private RebuildChunksResponse rebuildChunksInternal(
            KbDocument document,
            KbDocumentVersion version,
            RebuildChunksRequest request,
            Long actorUserId
    ) {
        RebuildChunksRequest desensitizedRequest = desensitizationService.desensitize(
                document,
                version,
                request,
                new CurrentUser(actorUserId, document.getTenantId(), java.util.Set.of())
        );
        List<ChunkDraft> drafts = documentChunker.chunk(document, desensitizedRequest);
        if (drafts.isEmpty()) {
            throw new BadRequestException("Parsed document content must produce at least one chunk");
        }

        chunkRepository.deleteByVersionId(version.getId());
        embeddingIndexTaskRepository.deleteByVersionId(version.getId());
        chunkRepository.flush();
        embeddingIndexTaskRepository.flush();

        List<KbDocumentChunk> existingChunks = chunkRepository.findByDocId(document.getId());
        java.util.Map<String, Long> hashToOldChunkId = existingChunks.stream()
                .filter(c -> c.getContentHash() != null)
                .collect(java.util.stream.Collectors.toMap(
                        KbDocumentChunk::getContentHash,
                        KbDocumentChunk::getId,
                        (id1, id2) -> id1
                ));

        List<KbDocumentChunk> allChunks = new ArrayList<>();
        List<KbDocumentChunk> newChunks = new ArrayList<>();

        for (ChunkDraft draft : drafts) {
            String hash = sha256(draft.content());
            Long reusedId = hashToOldChunkId.get(hash);

            KbDocumentChunk chunk = new KbDocumentChunk();
            chunk.setId(reusedId != null ? reusedId : idGenerator.nextId());
            chunk.setTenantId(document.getTenantId());
            chunk.setSpaceId(document.getSpaceId());
            chunk.setDocId(document.getId());
            chunk.setVersionId(version.getId());
            chunk.setChunkIndex(draft.chunkIndex());
            chunk.setPageNo(draft.pageNo());
            chunk.setSectionTitle(draft.sectionTitle());
            chunk.setContent(draft.content());
            chunk.setTokenCount(draft.content().length());
            chunk.setContentHash(hash);
            chunk.setMetadataJson(toJson(draft));
            chunk.setStatus(ACTIVE);
            chunk.setCreatedBy(actorUserId);
            chunk.setUpdatedBy(actorUserId);
            allChunks.add(chunk);

            if (reusedId == null) {
                newChunks.add(chunk);
            }
        }

        List<KbDocumentChunk> savedChunks = chunkRepository.saveAll(allChunks);
        int totalTokens = savedChunks.stream()
                .mapToInt(KbDocumentChunk::getTokenCount)
                .sum();

        version.setChunkCount(savedChunks.size());
        version.setTotalTokens(totalTokens);
        version.setParseStatus("COMPLETED");
        version.setUpdatedBy(actorUserId);
        versionRepository.save(version);

        markParseTaskCompleted(document, version, savedChunks.size(), totalTokens);
        createEmbeddingIndexTasks(document, version, newChunks, actorUserId);

        List<ChunkResponse> chunkResponses = new ArrayList<>();
        for (int index = 0; index < savedChunks.size(); index++) {
            chunkResponses.add(ChunkResponse.fromEntity(savedChunks.get(index), drafts.get(index).contentType()));
        }
        return new RebuildChunksResponse(
                document.getId(),
                version.getId(),
                savedChunks.size(),
                version.getParseStatus(),
                chunkResponses
        );
    }

    private void markParseTaskCompleted(
            KbDocument document,
            KbDocumentVersion version,
            int chunkCount,
            int totalTokens
    ) {
        KbDocumentParseTask task = parseTaskRepository.findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(
                document.getId(),
                version.getId()
        ).orElse(null);
        if (task == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgressPercent(100);
        task.setFinishedAt(now);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setMetadataJson("""
                {"chunk_count":%d,"total_tokens":%d}
                """.formatted(chunkCount, totalTokens).strip());
        parseTaskRepository.save(task);
    }

    private void createEmbeddingIndexTasks(
            KbDocument document,
            KbDocumentVersion version,
            List<KbDocumentChunk> chunks,
            Long actorUserId
    ) {
        List<KbEmbeddingIndexTask> tasks = new ArrayList<>();
        for (KbDocumentChunk chunk : chunks) {
            KbEmbeddingIndexTask task = new KbEmbeddingIndexTask();
            task.setId(idGenerator.nextId());
            task.setTenantId(document.getTenantId());
            task.setSpaceId(document.getSpaceId());
            task.setDocId(document.getId());
            task.setVersionId(version.getId());
            task.setChunkId(chunk.getId());
            task.setModelProvider(aiProperties.getEmbeddingProvider() != null ? aiProperties.getEmbeddingProvider() : "openai");
            task.setModelName(aiProperties.getEmbeddingModel() != null ? aiProperties.getEmbeddingModel() : "mock-embedding-v1");
            task.setEmbeddingDimension(aiProperties.getEmbeddingDimension() != null ? aiProperties.getEmbeddingDimension() : 128);
            task.setIndexName("knowledge_chunk_keyword");
            task.setVectorCollection("knowledge_chunk_vector");
            task.setStatus(TaskStatus.PENDING);
            task.setPriority(0);
            task.setRetryCount(0);
            task.setProgressPercent(0);
            task.setCreatedBy(actorUserId);
            task.setUpdatedBy(actorUserId);
            tasks.add(task);
        }
        embeddingIndexTaskRepository.saveAll(tasks);

        for (KbEmbeddingIndexTask task : tasks) {
            taskEventProducer.sendEmbeddingTask(task.getId());
        }
    }

    private KbDocument requireActiveDocument(Long documentId) {
        return documentRepository.findByIdAndStatusNot(documentId, com.sunxin.knowledge.document.domain.DocumentStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private KbDocumentVersion currentVersion(KbDocument document) {
        if (document.getCurrentVersionId() != null) {
            return versionRepository.findById(document.getCurrentVersionId())
                    .orElseThrow(() -> new NotFoundException("Document version not found"));
        }
        return versionRepository.findFirstByDocIdAndStatusOrderByVersionNoDesc(document.getId(), ACTIVE)
                .orElseThrow(() -> new NotFoundException("Document version not found"));
    }

    private String toJson(ChunkDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft.metadata());
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Chunk metadata cannot be serialized");
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
