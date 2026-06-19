package com.sunxin.knowledge.document.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.document.dto.DocumentDeleteResponse;
import com.sunxin.knowledge.document.dto.DocumentDetailResponse;
import com.sunxin.knowledge.document.dto.DocumentListItemResponse;
import com.sunxin.knowledge.document.dto.DocumentParseStatusResponse;
import com.sunxin.knowledge.document.dto.DocumentUploadRequest;
import com.sunxin.knowledge.document.dto.DocumentUploadResponse;
import com.sunxin.knowledge.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.sunxin.knowledge.document.storage.FileStorageService;
import com.sunxin.knowledge.document.storage.StoredFile;
import com.sunxin.knowledge.document.support.DocumentType;
import com.sunxin.knowledge.document.support.FileHash;
import com.sunxin.knowledge.knowledgebase.application.KnowledgeSpaceApplicationService;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.document.domain.DocumentStatus;
import com.sunxin.knowledge.task.domain.TaskStatus;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;
import com.sunxin.knowledge.document.storage.LocalStoredFileResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;
import java.nio.file.Path;

@Service
public class DocumentIngestionService {

    private static final String PARSE_DOCUMENT = "PARSE_DOCUMENT";
    private static final String DEFAULT_CONFIDENTIAL_LEVEL = "INTERNAL";

    private final KnowledgeSpaceApplicationService spaceService;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentVersionRepository versionRepository;
    private final KbDocumentParseTaskRepository parseTaskRepository;
    private final FileStorageService fileStorageService;
    private final LocalStoredFileResolver fileResolver;
    private final IdGenerator idGenerator;
    private final AccessControlService accessControlService;
    private final com.sunxin.knowledge.task.TaskEventProducer taskEventProducer;

    public DocumentIngestionService(
            KnowledgeSpaceApplicationService spaceService,
            KbDocumentRepository documentRepository,
            KbDocumentVersionRepository versionRepository,
            KbDocumentParseTaskRepository parseTaskRepository,
            FileStorageService fileStorageService,
            LocalStoredFileResolver fileResolver,
            IdGenerator idGenerator,
            AccessControlService accessControlService,
            com.sunxin.knowledge.task.TaskEventProducer taskEventProducer
    ) {
        this.spaceService = spaceService;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.parseTaskRepository = parseTaskRepository;
        this.fileStorageService = fileStorageService;
        this.fileResolver = fileResolver;
        this.idGenerator = idGenerator;
        this.accessControlService = accessControlService;
        this.taskEventProducer = taskEventProducer;
    }

    @Transactional
    public DocumentUploadResponse upload(
            Long spaceId,
            MultipartFile file,
            DocumentUploadRequest request,
            CurrentUser currentUser
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }

        KbSpace space = spaceService.requireActiveSpace(spaceId);
        CurrentUser resolvedUser = currentUser.withTenant(space.getTenantId());
        accessControlService.requireSpacePermission(space, resolvedUser, PermissionAction.DOCUMENT_UPLOAD);
        String originalFilename = DocumentType.cleanFilename(file.getOriginalFilename());
        DocumentType documentType = DocumentType.fromFilename(originalFilename)
                .orElseThrow(() -> new BadRequestException("Unsupported document type"));
        String fileHash = FileHash.sha256(file);

        return documentRepository.findFirstByTenantIdAndSpaceIdAndFileHashAndStatusNotOrderByCreatedAtDesc(
                        space.getTenantId(),
                        space.getId(),
                        fileHash,
                        DocumentStatus.DELETED
                )
                .map(document -> existingUploadResponse(document, true))
                .orElseGet(() -> createNewDocument(
                        space,
                        file,
                        request,
                        originalFilename,
                        documentType,
                        fileHash,
                        resolvedUser
                ));
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentListItemResponse> listBySpace(Long spaceId, int page, int size, CurrentUser currentUser) {
        KbSpace space = spaceService.requireActiveSpace(spaceId);
        CurrentUser resolvedUser = currentUser.withTenant(space.getTenantId());
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<KbDocument> documentPage = documentRepository.findBySpaceIdAndStatusNotOrderByCreatedAtDesc(spaceId, DocumentStatus.DELETED, pageRequest);
        
        List<DocumentListItemResponse> filteredContent = documentPage.getContent().stream()
                .filter(document -> accessControlService.canAccessDocument(
                        space,
                        document,
                        resolvedUser,
                        PermissionAction.DOCUMENT_READ
                ))
                .map(DocumentListItemResponse::fromEntity)
                .toList();
                
        return new PageResponse<>(
                filteredContent,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse detail(Long documentId, CurrentUser currentUser) {
        KbDocument document = requireActiveDocument(documentId);
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.DOCUMENT_READ);
        KbDocumentVersion version = currentVersion(document);
        return DocumentDetailResponse.fromEntity(document, version);
    }

    @Transactional
    public DocumentDeleteResponse delete(Long documentId, CurrentUser currentUser) {
        KbDocument document = requireActiveDocument(documentId);
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.DOCUMENT_DELETE);
        document.setStatus(DocumentStatus.DELETED);
        document.setUpdatedBy(currentUser.userId());
        documentRepository.save(document);
        return new DocumentDeleteResponse(document.getId(), document.getStatus());
    }

    @Transactional(readOnly = true)
    public Resource download(Long documentId, CurrentUser currentUser) {
        KbDocument document = requireActiveDocument(documentId);
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.DOCUMENT_READ);
        
        String sourceUri = document.getSourceUri();
        if (sourceUri == null) {
            throw new NotFoundException("Document file not found");
        }
        
        try {
            if (sourceUri.startsWith("local://")) {
                Path file = fileResolver.resolve(sourceUri);
                Resource resource = new UrlResource(file.toUri());
                if (resource.exists() || resource.isReadable()) {
                    return resource;
                } else {
                    throw new NotFoundException("File does not exist or is not readable");
                }
            } else {
                throw new BadRequestException("Unsupported storage protocol for direct download");
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not read file", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentParseStatusResponse parseStatus(Long documentId, CurrentUser currentUser) {
        KbDocument document = requireActiveDocument(documentId);
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.DOCUMENT_READ);
        KbDocumentVersion version = currentVersion(document);
        KbDocumentParseTask task = parseTaskRepository.findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(
                document.getId(),
                version.getId()
        ).orElse(null);
        return DocumentParseStatusResponse.fromEntities(document.getId(), version, task);
    }

    private DocumentUploadResponse createNewDocument(
            KbSpace space,
            MultipartFile file,
            DocumentUploadRequest request,
            String originalFilename,
            DocumentType documentType,
            String fileHash,
            CurrentUser currentUser
    ) {
        StoredFile storedFile = fileStorageService.store(new FileStorageService.StorageRequest(
                space.getTenantId(),
                space.getId(),
                fileHash,
                originalFilename,
                file
        ));

        KbDocument document = new KbDocument();
        document.setId(idGenerator.nextId());
        document.setTenantId(space.getTenantId());
        document.setSpaceId(space.getId());
        document.setTitle(defaultString(request.title(), originalFilename));
        document.setDocType(documentType.code());
        document.setIndustry(blankToNull(request.industry()));
        document.setServiceLine(blankToNull(request.serviceLine()));
        document.setConfidentialLevel(defaultString(request.confidentialLevel(), DEFAULT_CONFIDENTIAL_LEVEL));
        document.setSourceUri(storedFile.sourceUri());
        document.setStorageUri(storedFile.sourceUri());
        document.setFileHash(fileHash);
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.UPLOADED);
        document.setCreatedBy(currentUser.userId());
        document.setUpdatedBy(currentUser.userId());
        documentRepository.save(document);

        KbDocumentVersion version = new KbDocumentVersion();
        version.setId(idGenerator.nextId());
        version.setTenantId(space.getTenantId());
        version.setSpaceId(space.getId());
        version.setDocId(document.getId());
        version.setVersionNo(1);
        version.setSourceUri(storedFile.sourceUri());
        version.setStorageUri(storedFile.sourceUri());
        version.setFileHash(fileHash);
        version.setFileSize(file.getSize());
        version.setParseStatus("PENDING");
        version.setChunkCount(0);
        version.setTotalTokens(0);
        version.setStatus("ACTIVE");
        version.setCreatedBy(currentUser.userId());
        version.setUpdatedBy(currentUser.userId());
        versionRepository.save(version);

        document.setCurrentVersionId(version.getId());
        documentRepository.save(document);

        KbDocumentParseTask parseTask = new KbDocumentParseTask();
        parseTask.setId(idGenerator.nextId());
        parseTask.setTenantId(space.getTenantId());
        parseTask.setSpaceId(space.getId());
        parseTask.setDocId(document.getId());
        parseTask.setVersionId(version.getId());
        parseTask.setTaskType(PARSE_DOCUMENT);
        parseTask.setStatus(TaskStatus.PENDING);
        parseTask.setPriority(0);
        parseTask.setRetryCount(0);
        parseTask.setProgressPercent(0);
        parseTask.setCreatedBy(currentUser.userId());
        parseTask.setUpdatedBy(currentUser.userId());
        parseTaskRepository.save(parseTask);

        taskEventProducer.sendParseTask(parseTask.getId());

        return toUploadResponse(document, version, parseTask, false);
    }

    private DocumentUploadResponse existingUploadResponse(KbDocument document, boolean duplicated) {
        KbDocumentVersion version = currentVersion(document);
        KbDocumentParseTask task = parseTaskRepository.findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(
                document.getId(),
                version.getId()
        ).orElse(null);
        return toUploadResponse(document, version, task, duplicated);
    }

    private DocumentUploadResponse toUploadResponse(
            KbDocument document,
            KbDocumentVersion version,
            KbDocumentParseTask task,
            boolean duplicated
    ) {
        return new DocumentUploadResponse(
                document.getId(),
                version.getId(),
                task == null ? null : task.getId(),
                document.getTitle(),
                document.getDocType(),
                document.getIndustry(),
                document.getServiceLine(),
                document.getConfidentialLevel(),
                document.getSourceUri(),
                document.getFileHash(),
                document.getStatus(),
                version.getParseStatus(),
                duplicated
        );
    }

    private KbDocument requireActiveDocument(Long documentId) {
        return documentRepository.findByIdAndStatusNot(documentId, DocumentStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private KbDocumentVersion currentVersion(KbDocument document) {
        if (document.getCurrentVersionId() != null) {
            return versionRepository.findById(document.getCurrentVersionId())
                    .orElseThrow(() -> new NotFoundException("Document version not found"));
        }
        return versionRepository.findFirstByDocIdAndStatusOrderByVersionNoDesc(document.getId(), "ACTIVE")
                .orElseThrow(() -> new NotFoundException("Document version not found"));
    }

    private static String defaultString(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
