package com.sunxin.knowledge.document.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.document.desensitization.DesensitizationResult;
import com.sunxin.knowledge.document.desensitization.Desensitizer;
import com.sunxin.knowledge.document.desensitization.SensitiveMapping;
import com.sunxin.knowledge.document.dto.DesensitizationMappingResponse;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.persistence.entity.KbDesensitizationMapping;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.repository.KbDesensitizationMappingRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;

@Service
public class DocumentDesensitizationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DELETED = "DELETED";
    private static final String COMPLETED = "COMPLETED";

    private final Desensitizer desensitizer;
    private final KbDesensitizationMappingRepository mappingRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentVersionRepository versionRepository;
    private final AccessControlService accessControlService;
    private final IdGenerator idGenerator;

    public DocumentDesensitizationService(
            Desensitizer desensitizer,
            KbDesensitizationMappingRepository mappingRepository,
            KbDocumentRepository documentRepository,
            KbDocumentVersionRepository versionRepository,
            AccessControlService accessControlService,
            IdGenerator idGenerator
    ) {
        this.desensitizer = desensitizer;
        this.mappingRepository = mappingRepository;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.accessControlService = accessControlService;
        this.idGenerator = idGenerator;
    }

    public RebuildChunksRequest desensitize(
            KbDocument document,
            KbDocumentVersion version,
            RebuildChunksRequest request,
            CurrentUser currentUser
    ) {
        DesensitizationResult result = desensitizer.desensitize(request.pages());
        mappingRepository.deleteByVersionId(version.getId());
        mappingRepository.flush();

        List<KbDesensitizationMapping> rows = new ArrayList<>();
        for (SensitiveMapping mapping : result.mappings()) {
            KbDesensitizationMapping row = new KbDesensitizationMapping();
            row.setId(idGenerator.nextId());
            row.setTenantId(document.getTenantId());
            row.setSpaceId(document.getSpaceId());
            row.setDocId(document.getId());
            row.setVersionId(version.getId());
            row.setPageNo(mapping.pageNo());
            row.setSectionTitle(mapping.sectionTitle());
            row.setSensitiveType(mapping.sensitiveType());
            row.setOriginalValue(mapping.originalValue());
            row.setMaskedValue(mapping.maskedValue());
            row.setRuleName(mapping.ruleName());
            row.setOccurrenceIndex(mapping.occurrenceIndex());
            row.setStatus(ACTIVE);
            row.setCreatedBy(currentUser.userId());
            row.setUpdatedBy(currentUser.userId());
            rows.add(row);
        }
        mappingRepository.saveAll(rows);

        version.setDesensitizeStatus(COMPLETED);
        version.setDesensitizedAt(LocalDateTime.now());
        version.setUpdatedBy(currentUser.userId());
        return new RebuildChunksRequest(request.chunkSize(), request.overlap(), result.pages());
    }

    @Transactional(readOnly = true)
    public List<DesensitizationMappingResponse> mappings(Long documentId, CurrentUser currentUser) {
        KbDocument document = documentRepository.findByIdAndStatusNot(documentId, com.sunxin.knowledge.document.domain.DocumentStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        accessControlService.requireDocumentPermission(document, currentUser, PermissionAction.ADMIN_MANAGE);
        KbDocumentVersion version = currentVersion(document);
        return mappingRepository.findByDocIdAndVersionIdAndStatusOrderByOccurrenceIndex(
                        document.getId(),
                        version.getId(),
                        ACTIVE
                )
                .stream()
                .sorted(Comparator.comparing(KbDesensitizationMapping::getOccurrenceIndex))
                .map(DesensitizationMappingResponse::fromEntity)
                .toList();
    }

    private KbDocumentVersion currentVersion(KbDocument document) {
        if (document.getCurrentVersionId() != null) {
            return versionRepository.findById(document.getCurrentVersionId())
                    .orElseThrow(() -> new NotFoundException("Document version not found"));
        }
        return versionRepository.findFirstByDocIdAndStatusOrderByVersionNoDesc(document.getId(), ACTIVE)
                .orElseThrow(() -> new NotFoundException("Document version not found"));
    }
}
