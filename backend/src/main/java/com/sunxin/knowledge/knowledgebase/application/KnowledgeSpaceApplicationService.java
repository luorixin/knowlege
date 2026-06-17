package com.sunxin.knowledge.knowledgebase.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.audit.AuditLogRecorder;
import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.knowledgebase.dto.CreateKnowledgeSpaceRequest;
import com.sunxin.knowledge.knowledgebase.dto.KnowledgeSpaceResponse;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@Service
public class KnowledgeSpaceApplicationService {

    private static final String ACTIVE = "ACTIVE";

    private final KbSpaceRepository spaceRepository;
    private final IdGenerator idGenerator;
    private final AccessControlService accessControlService;
    private final AuditLogRecorder auditLogRecorder;

    public KnowledgeSpaceApplicationService(
            KbSpaceRepository spaceRepository,
            IdGenerator idGenerator,
            AccessControlService accessControlService,
            AuditLogRecorder auditLogRecorder
    ) {
        this.spaceRepository = spaceRepository;
        this.idGenerator = idGenerator;
        this.accessControlService = accessControlService;
        this.auditLogRecorder = auditLogRecorder;
    }

    @Transactional
    public KnowledgeSpaceResponse create(CreateKnowledgeSpaceRequest request, CurrentUser currentUser) {
        KbSpace space = new KbSpace();
        space.setId(idGenerator.nextId());
        space.setTenantId(request.tenantId());
        space.setName(request.name().trim());
        space.setDescription(blankToNull(request.description()));
        space.setOwnerUserId(request.ownerUserId() == null ? currentUser.userId() : request.ownerUserId());
        space.setVisibility(defaultString(request.visibility(), "PRIVATE"));
        space.setStatus(ACTIVE);

        KbSpace saved = spaceRepository.save(space);
        auditLogRecorder.record(
                currentUser.withTenant(saved.getTenantId()),
                saved.getTenantId(),
                "space_create",
                "SPACE",
                saved.getId(),
                AuditLogRecorder.SUCCESS,
                auditLogRecorder.detail("space_id", saved.getId())
        );
        return KnowledgeSpaceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeSpaceResponse> list(Long tenantId, CurrentUser currentUser) {
        return spaceRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ACTIVE)
                .stream()
                .filter(space -> accessControlService.canAccessSpace(space, currentUser, PermissionAction.SPACE_READ))
                .map(KnowledgeSpaceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public KbSpace requireActiveSpace(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .filter(space -> ACTIVE.equals(space.getStatus()))
                .orElseThrow(() -> new NotFoundException("Knowledge space not found"));
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
