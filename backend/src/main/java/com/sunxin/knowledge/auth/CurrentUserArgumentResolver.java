package com.sunxin.knowledge.auth;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserResolver currentUserResolver;
    private final boolean headerFallbackEnabled;

    public CurrentUserArgumentResolver(CurrentUserResolver currentUserResolver) {
        this(currentUserResolver, false);
    }

    public CurrentUserArgumentResolver(CurrentUserResolver currentUserResolver, boolean headerFallbackEnabled) {
        this.currentUserResolver = currentUserResolver;
        this.headerFallbackEnabled = headerFallbackEnabled;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Long userId = null;
        Long tenantId = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = firstLong(jwt.getClaim("userId"), jwt.getClaim("user_id"), jwt.getSubject());
            tenantId = firstLong(jwt.getClaim("tenantId"), jwt.getClaim("tenant_id"));
        }

        if (headerFallbackEnabled && userId == null) {
            String userIdHeader = webRequest.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    userId = Long.valueOf(userIdHeader);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (headerFallbackEnabled && tenantId == null) {
            String tenantIdHeader = webRequest.getHeader("X-Tenant-Id");
            if (tenantIdHeader != null) {
                try {
                    tenantId = Long.valueOf(tenantIdHeader);
                } catch (NumberFormatException ignored) {}
            }
        }

        return currentUserResolver.resolve(userId, tenantId);
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
