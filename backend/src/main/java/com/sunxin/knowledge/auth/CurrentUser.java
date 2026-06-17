package com.sunxin.knowledge.auth;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record CurrentUser(
        Long userId,
        Long tenantId,
        Set<String> roleCodes
) {

    public CurrentUser {
        if (userId == null) {
            userId = 0L;
        }
        if (roleCodes == null) {
            roleCodes = Set.of();
        } else {
            roleCodes = roleCodes.stream()
                    .map(CurrentUser::normalize)
                    .filter(role -> !role.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    public CurrentUser withTenant(Long tenantId) {
        return new CurrentUser(userId, tenantId, roleCodes);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
