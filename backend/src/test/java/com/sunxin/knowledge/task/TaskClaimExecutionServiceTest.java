package com.sunxin.knowledge.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import com.sunxin.knowledge.document.application.DocumentChunkingService;
import com.sunxin.knowledge.document.cleaning.DocumentCleaningService;
import com.sunxin.knowledge.document.storage.StoredFileResolver;
import com.sunxin.knowledge.document.storage.ResolvedStoredFile;
import com.sunxin.knowledge.integration.ai.AiPipelineClient;
import com.sunxin.knowledge.integration.embedding.EmbeddingProvider;
import com.sunxin.knowledge.integration.search.KeywordSearchClient;
import com.sunxin.knowledge.integration.vector.VectorStoreClient;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;
import com.sunxin.knowledge.task.domain.TaskStatus;

class TaskClaimExecutionServiceTest {

    @Test
    void parseExecutionDeletesTemporaryResolvedFileWhenAiFails() throws Exception {
        KbDocumentParseTaskRepository taskRepository = mock(KbDocumentParseTaskRepository.class);
        KbDocumentRepository documentRepository = mock(KbDocumentRepository.class);
        KbDocumentVersionRepository versionRepository = mock(KbDocumentVersionRepository.class);
        StoredFileResolver fileResolver = mock(StoredFileResolver.class);
        AiPipelineClient aiPipelineClient = mock(AiPipelineClient.class);
        KbDocumentParseTask task = parseTask(33L, TaskStatus.PENDING);
        KbDocument document = new KbDocument();
        document.setId(task.getDocId());
        KbDocumentVersion version = new KbDocumentVersion();
        version.setId(task.getVersionId());
        version.setSourceUri("minio://knowledge-documents/1/2/sample.pdf");
        Path temporaryFile = Files.createTempFile("parse-task-test-", ".pdf");
        when(taskRepository.claimPending(
                org.mockito.ArgumentMatchers.eq(33L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1);
        when(taskRepository.findById(33L)).thenReturn(Optional.of(task));
        when(documentRepository.findById(task.getDocId())).thenReturn(Optional.of(document));
        when(versionRepository.findById(task.getVersionId())).thenReturn(Optional.of(version));
        when(fileResolver.resolve(version.getSourceUri())).thenReturn(ResolvedStoredFile.temporary(temporaryFile));
        when(aiPipelineClient.parseDocument(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("AI unavailable"));

        DocumentParseTaskExecutionService service = new DocumentParseTaskExecutionService(
                taskRepository,
                documentRepository,
                versionRepository,
                fileResolver,
                aiPipelineClient,
                mock(DocumentChunkingService.class),
                mock(DocumentCleaningService.class),
                new TaskExecutionProperties(),
                new ObjectMapper()
        );

        assertThat(service.process(33L).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(temporaryFile).doesNotExist();
    }

    @Test
    void duplicateParseExecutionReturnsCurrentTaskWithoutCallingAi() {
        KbDocumentParseTaskRepository taskRepository = mock(KbDocumentParseTaskRepository.class);
        KbDocumentRepository documentRepository = mock(KbDocumentRepository.class);
        KbDocumentVersionRepository versionRepository = mock(KbDocumentVersionRepository.class);
        StoredFileResolver fileResolver = mock(StoredFileResolver.class);
        AiPipelineClient aiPipelineClient = mock(AiPipelineClient.class);
        KbDocumentParseTask task = parseTask(11L, TaskStatus.COMPLETED);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        DocumentParseTaskExecutionService service = new DocumentParseTaskExecutionService(
                taskRepository,
                documentRepository,
                versionRepository,
                fileResolver,
                aiPipelineClient,
                mock(DocumentChunkingService.class),
                mock(DocumentCleaningService.class),
                new TaskExecutionProperties(),
                new ObjectMapper()
        );

        assertThat(service.process(11L).status()).isEqualTo(TaskStatus.COMPLETED);
        verify(aiPipelineClient, never()).parseDocument(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void duplicateEmbeddingExecutionReturnsCurrentTaskWithoutCallingIndexProviders() {
        KbEmbeddingIndexTaskRepository taskRepository = mock(KbEmbeddingIndexTaskRepository.class);
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        KeywordSearchClient keywordSearchClient = mock(KeywordSearchClient.class);
        VectorStoreClient vectorStoreClient = mock(VectorStoreClient.class);
        KbEmbeddingIndexTask task = embeddingTask(22L, TaskStatus.COMPLETED);
        when(taskRepository.findById(22L)).thenReturn(Optional.of(task));

        EmbeddingIndexTaskExecutionService service = new EmbeddingIndexTaskExecutionService(
                taskRepository,
                mock(KbDocumentChunkRepository.class),
                mock(KbDocumentRepository.class),
                embeddingProvider,
                keywordSearchClient,
                vectorStoreClient,
                new EmbeddingTaskExecutionProperties()
        );

        assertThat(service.process(22L).status()).isEqualTo(TaskStatus.COMPLETED);
        verify(embeddingProvider, never()).embed(org.mockito.ArgumentMatchers.anyString());
        verify(keywordSearchClient, never()).indexChunk(org.mockito.ArgumentMatchers.any());
        verify(vectorStoreClient, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    private static KbDocumentParseTask parseTask(Long id, TaskStatus status) {
        KbDocumentParseTask task = new KbDocumentParseTask();
        task.setId(id);
        task.setTenantId(1L);
        task.setSpaceId(2L);
        task.setDocId(3L);
        task.setVersionId(4L);
        task.setStatus(status);
        return task;
    }

    private static KbEmbeddingIndexTask embeddingTask(Long id, TaskStatus status) {
        KbEmbeddingIndexTask task = new KbEmbeddingIndexTask();
        task.setId(id);
        task.setTenantId(1L);
        task.setSpaceId(2L);
        task.setDocId(3L);
        task.setVersionId(4L);
        task.setChunkId(5L);
        task.setModelName("mock");
        task.setStatus(status);
        return task;
    }
}
