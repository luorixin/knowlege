package com.sunxin.knowledge.auth;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.persistence.repository.KbUserRoleRepository;

@Component
public class CurrentUserResolver {

    private final KbUserRoleRepository userRoleRepository;

    public CurrentUserResolver(KbUserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    public CurrentUser resolve(Long userId, Long tenantId) {
        return resolve(userId, tenantId, tenantId);
    }

    public CurrentUser resolve(Long userId, Long tenantId, Long fallbackTenantId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            Long jwtUserId = firstLong(jwt.getClaim("userId"), jwt.getClaim("user_id"), jwt.getSubject());
            Long jwtTenantId = firstLong(jwt.getClaim("tenantId"), jwt.getClaim("tenant_id"));
            if (jwtUserId != null && jwtTenantId != null) {
                userId = jwtUserId;
                tenantId = jwtTenantId;
            }
        }

        Long effectiveUserId = userId == null ? 0L : userId;
        Long effectiveTenantId = tenantId == null ? fallbackTenantId : tenantId;
        if (tenantId != null && fallbackTenantId != null && !tenantId.equals(fallbackTenantId)) {
            throw new BadRequestException("X-Tenant-Id does not match requested tenant");
        }
        Set<String> roleCodes = effectiveTenantId == null
                ? Set.of()
                : userRoleRepository.findActiveRoleCodes(effectiveTenantId, effectiveUserId);
        return new CurrentUser(effectiveUserId, effectiveTenantId, roleCodes);
    }

    private static Long firstLong(Object... values) {
        for (Object value : values) {
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Long parseLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
