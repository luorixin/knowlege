package com.sunxin.knowledge.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.audit.AuditLogRecorder;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.ForbiddenException;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.persistence.repository.KbUserRoleRepository;

@Service
public class AccessControlService {

    private static final String ACTIVE = "ACTIVE";
    private static final String ALLOW = "ALLOW";
    private static final String DENY = "DENY";

    private final KbPermissionPolicyRepository permissionPolicyRepository;
    private final KbUserRoleRepository userRoleRepository;
    private final KbSpaceRepository spaceRepository;
    private final AuditLogRecorder auditLogRecorder;

    public AccessControlService(
            KbPermissionPolicyRepository permissionPolicyRepository,
            KbUserRoleRepository userRoleRepository,
            KbSpaceRepository spaceRepository,
            AuditLogRecorder auditLogRecorder
    ) {
        this.permissionPolicyRepository = permissionPolicyRepository;
        this.userRoleRepository = userRoleRepository;
        this.spaceRepository = spaceRepository;
        this.auditLogRecorder = auditLogRecorder;
    }

    public void requireSpacePermission(KbSpace space, CurrentUser user, String action) {
        CurrentUser resolvedUser = requireTenant(space.getTenantId(), user);
        if (canAccessSpace(space, resolvedUser, action)) {
            auditLogRecorder.record(
                    resolvedUser,
                    space.getTenantId(),
                    action,
                    "SPACE",
                    space.getId(),
                    AuditLogRecorder.SUCCESS,
                    auditLogRecorder.detail("space_id", space.getId())
            );
            return;
        }
        auditLogRecorder.record(
                resolvedUser,
                space.getTenantId(),
                action,
                "SPACE",
                space.getId(),
                AuditLogRecorder.DENIED,
                auditLogRecorder.detail("space_id", space.getId())
        );
        throw new ForbiddenException("No permission to access knowledge space");
    }

    public CurrentUser resolveForTenant(CurrentUser user, Long tenantId) {
        CurrentUser resolvedUser = requireTenant(tenantId, user);
        if (resolvedUser.roleCodes() != null && !resolvedUser.roleCodes().isEmpty()) {
            return resolvedUser;
        }
        return new CurrentUser(
                resolvedUser.userId(),
                tenantId,
                userRoleRepository.findActiveRoleCodes(tenantId, resolvedUser.userId())
        );
    }

    public void requireDocumentPermission(KbDocument document, CurrentUser user, String action) {
        CurrentUser resolvedUser = requireTenant(document.getTenantId(), user);
        if (canAccessDocument(document, resolvedUser, action)) {
            auditLogRecorder.record(
                    resolvedUser,
                    document.getTenantId(),
                    action,
                    "DOCUMENT",
                    document.getId(),
                    AuditLogRecorder.SUCCESS,
                    auditLogRecorder.detail("space_id", document.getSpaceId(), "doc_id", document.getId())
            );
            return;
        }
        auditLogRecorder.record(
                resolvedUser,
                document.getTenantId(),
                action,
                "DOCUMENT",
                document.getId(),
                AuditLogRecorder.DENIED,
                auditLogRecorder.detail("space_id", document.getSpaceId(), "doc_id", document.getId())
        );
        throw new ForbiddenException("No permission to access document");
    }

    public boolean canAccessSpace(KbSpace space, CurrentUser user, String action) {
        CurrentUser resolvedUser = requireTenant(space.getTenantId(), user);
        if (isOwner(space, resolvedUser)) {
            return true;
        }
        PermissionDecision decision = decide(space.getTenantId(), resolvedUser, action, space, null);
        return decision.allowed();
    }

    public boolean canAccessDocument(KbDocument document, CurrentUser user, String action) {
        KbSpace space = spaceRepository.findById(document.getSpaceId()).orElse(null);
        return canAccessDocument(space, document, user, action);
    }

    public boolean canAccessDocument(KbSpace space, KbDocument document, CurrentUser user, String action) {
        CurrentUser resolvedUser = requireTenant(document.getTenantId(), user);
        if (space != null && isOwner(space, resolvedUser)) {
            return true;
        }
        PermissionDecision decision = decide(document.getTenantId(), resolvedUser, action, null, document);
        return decision.allowed();
    }

    private PermissionDecision decide(
            Long tenantId,
            CurrentUser user,
            String action,
            KbSpace space,
            KbDocument document
    ) {
        List<KbPermissionPolicy> matchedPolicies = permissionPolicyRepository.findByTenantIdAndStatus(tenantId, ACTIVE)
                .stream()
                .filter(AccessControlService::validNow)
                .filter(policy -> actionMatches(policy, action))
                .filter(policy -> subjectMatches(policy, user, tenantId))
                .filter(policy -> resourceMatches(policy, space, document))
                .toList();

        boolean denied = matchedPolicies.stream()
                .anyMatch(policy -> DENY.equalsIgnoreCase(policy.getEffect()));
        if (denied) {
            return PermissionDecision.DENIED;
        }

        boolean allowed = matchedPolicies.stream()
                .anyMatch(policy -> ALLOW.equalsIgnoreCase(policy.getEffect()));
        return allowed ? PermissionDecision.ALLOWED : PermissionDecision.NOT_MATCHED;
    }

    private CurrentUser requireTenant(Long resourceTenantId, CurrentUser user) {
        CurrentUser resolvedUser = user == null ? new CurrentUser(0L, resourceTenantId, Set.of()) : user;
        if (resolvedUser.tenantId() != null && resourceTenantId != null && !resolvedUser.tenantId().equals(resourceTenantId)) {
            throw new BadRequestException("X-Tenant-Id does not match requested resource");
        }
        if (resolvedUser.tenantId() == null) {
            return resolvedUser.withTenant(resourceTenantId);
        }
        return resolvedUser;
    }

    private static boolean isOwner(KbSpace space, CurrentUser user) {
        return space.getOwnerUserId() != null && space.getOwnerUserId().equals(user.userId());
    }

    private boolean subjectMatches(KbPermissionPolicy policy, CurrentUser user, Long tenantId) {
        String subjectType = normalize(policy.getSubjectType());
        String subjectId = normalize(policy.getSubjectId());
        if ("*".equals(subjectType) || "all".equals(subjectType) || "*".equals(subjectId)) {
            return true;
        }
        if ("user".equals(subjectType)) {
            return subjectId.equals(String.valueOf(user.userId()));
        }
        if ("role".equals(subjectType)) {
            Set<String> roleSubjects = roleSubjects(user, tenantId);
            return roleSubjects.contains(subjectId);
        }
        return false;
    }

    private Set<String> roleSubjects(CurrentUser user, Long tenantId) {
        Set<String> roleCodes = user.roleCodes();
        if ((roleCodes == null || roleCodes.isEmpty()) && tenantId != null) {
            roleCodes = userRoleRepository.findActiveRoleCodes(tenantId, user.userId());
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        return roleCodes.stream()
                .map(AccessControlService::normalize)
                .collect(Collectors.toSet());
    }

    private static boolean resourceMatches(KbPermissionPolicy policy, KbSpace space, KbDocument document) {
        String resourceType = normalize(policy.getResourceType());
        Long resourceId = policy.getResourceId();
        if ("*".equals(resourceType) || "all".equals(resourceType)) {
            return true;
        }
        if (document != null) {
            return switch (resourceType) {
                case "space", "kb_space" -> resourceId == null || resourceId.equals(document.getSpaceId());
                case "document", "doc", "kb_document" -> resourceId == null || resourceId.equals(document.getId());
                default -> false;
            };
        }
        if (space != null) {
            return switch (resourceType) {
                case "space", "kb_space" -> resourceId == null || resourceId.equals(space.getId());
                default -> false;
            };
        }
        return false;
    }

    private static boolean actionMatches(KbPermissionPolicy policy, String requestedAction) {
        Set<String> actions = splitActions(policy.getActions());
        if (actions.contains("*") || actions.contains(PermissionAction.ADMIN_MANAGE)) {
            return true;
        }
        String normalizedAction = normalizeAction(requestedAction);
        if (actions.contains(normalizedAction)) {
            return true;
        }
        Set<String> aliases = aliases(normalizedAction);
        return actions.stream().map(AccessControlService::normalizeAction).anyMatch(aliases::contains);
    }

    private static Set<String> aliases(String action) {
        return switch (action) {
            case PermissionAction.SPACE_READ -> Set.of(PermissionAction.SPACE_READ, "read", "space.read");
            case PermissionAction.DOCUMENT_READ -> Set.of(PermissionAction.DOCUMENT_READ, "read", "retrieve", "search", "document.read");
            case PermissionAction.DOCUMENT_UPLOAD -> Set.of(PermissionAction.DOCUMENT_UPLOAD, "upload", "document.upload");
            case PermissionAction.DOCUMENT_DELETE -> Set.of(PermissionAction.DOCUMENT_DELETE, "delete", "document.delete");
            case PermissionAction.AGENT_CHAT -> Set.of(PermissionAction.AGENT_CHAT, "chat", "agent.chat");
            default -> Set.of(action);
        };
    }

    private static Set<String> splitActions(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return List.of(value.split("[,，\\s]+")).stream()
                .map(AccessControlService::normalizeAction)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private static String normalizeAction(String value) {
        return normalize(value).replace('-', '_').replace('.', '_');
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validNow(KbPermissionPolicy policy) {
        LocalDateTime now = LocalDateTime.now();
        boolean startsBeforeNow = policy.getValidFrom() == null || !policy.getValidFrom().isAfter(now);
        boolean endsAfterNow = policy.getValidTo() == null || policy.getValidTo().isAfter(now);
        return startsBeforeNow && endsAfterNow;
    }

    private record PermissionDecision(boolean allowed) {
        static final PermissionDecision ALLOWED = new PermissionDecision(true);
        static final PermissionDecision DENIED = new PermissionDecision(false);
        static final PermissionDecision NOT_MATCHED = new PermissionDecision(false);
    }
}
