package com.sunxin.knowledge.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.ForbiddenException;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;

@Service
public class AdminAccessService {

    private static final String ACTIVE = "ACTIVE";
    private static final String ALLOW = "ALLOW";
    private static final String DENY = "DENY";

    private final KbPermissionPolicyRepository permissionPolicyRepository;

    public AdminAccessService(KbPermissionPolicyRepository permissionPolicyRepository) {
        this.permissionPolicyRepository = permissionPolicyRepository;
    }

    public void requireAdmin(CurrentUser user) {
        if (user == null || user.userId() == null || user.userId() == 0L) {
            throw new ForbiddenException("Admin permission required");
        }
        if (user.tenantId() == null) {
            throw new BadRequestException("Tenant is required for admin operation");
        }
        if (hasAdminRole(user) || hasAdminManagePolicy(user)) {
            return;
        }
        throw new ForbiddenException("Admin permission required");
    }

    private static boolean hasAdminRole(CurrentUser user) {
        Set<String> roleCodes = user.roleCodes() == null ? Set.of() : user.roleCodes();
        return roleCodes.stream()
                .map(AdminAccessService::normalize)
                .anyMatch(role -> normalize(SystemRoleConst.ADMIN).equals(role)
                        || normalize(SystemRoleConst.KNOWLEDGE_ADMIN).equals(role));
    }

    private boolean hasAdminManagePolicy(CurrentUser user) {
        List<KbPermissionPolicy> matchedPolicies = permissionPolicyRepository.findByTenantIdAndStatus(user.tenantId(), ACTIVE)
                .stream()
                .filter(AdminAccessService::validNow)
                .filter(policy -> subjectMatches(policy, user))
                .filter(policy -> actionMatches(policy, PermissionAction.ADMIN_MANAGE))
                .filter(AdminAccessService::adminResourceMatches)
                .toList();
        boolean denied = matchedPolicies.stream().anyMatch(policy -> DENY.equalsIgnoreCase(policy.getEffect()));
        if (denied) {
            return false;
        }
        return matchedPolicies.stream().anyMatch(policy -> ALLOW.equalsIgnoreCase(policy.getEffect()));
    }

    private static boolean subjectMatches(KbPermissionPolicy policy, CurrentUser user) {
        String subjectType = normalize(policy.getSubjectType());
        String subjectId = normalize(policy.getSubjectId());
        if ("*".equals(subjectType) || "all".equals(subjectType) || "*".equals(subjectId)) {
            return true;
        }
        if ("user".equals(subjectType) && String.valueOf(user.userId()).equals(policy.getSubjectId())) {
            return true;
        }
        Set<String> roleCodes = user.roleCodes() == null ? Set.of() : user.roleCodes().stream()
                .map(AdminAccessService::normalize)
                .collect(Collectors.toSet());
        return "role".equals(subjectType) && roleCodes.contains(subjectId);
    }

    private static boolean actionMatches(KbPermissionPolicy policy, String action) {
        Set<String> actions = splitActions(policy.getActions());
        return actions.contains("*") || actions.contains(normalizeAction(action));
    }

    private static boolean adminResourceMatches(KbPermissionPolicy policy) {
        String resourceType = normalize(policy.getResourceType());
        return resourceType.isBlank()
                || "*".equals(resourceType)
                || "all".equals(resourceType)
                || "tenant".equals(resourceType)
                || "admin".equals(resourceType);
    }

    private static boolean validNow(KbPermissionPolicy policy) {
        LocalDateTime now = LocalDateTime.now();
        return (policy.getValidFrom() == null || !policy.getValidFrom().isAfter(now))
                && (policy.getValidTo() == null || policy.getValidTo().isAfter(now));
    }

    private static Set<String> splitActions(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return List.of(value.split("[,，\\s]+")).stream()
                .map(AdminAccessService::normalizeAction)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private static String normalizeAction(String value) {
        return normalize(value).replace('-', '_').replace('.', '_');
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
