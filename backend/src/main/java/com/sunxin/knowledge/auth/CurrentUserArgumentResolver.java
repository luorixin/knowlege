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

    public CurrentUserArgumentResolver(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
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
            try {
                if (jwt.getSubject() != null) {
                    userId = Long.valueOf(jwt.getSubject());
                }
            } catch (NumberFormatException ignored) {}
            
            Object tenantIdClaim = jwt.getClaim("tenant_id");
            if (tenantIdClaim instanceof Number n) {
                tenantId = n.longValue();
            } else if (tenantIdClaim instanceof String s) {
                try {
                    tenantId = Long.valueOf(s);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (userId == null) {
            String userIdHeader = webRequest.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    userId = Long.valueOf(userIdHeader);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (tenantId == null) {
            String tenantIdHeader = webRequest.getHeader("X-Tenant-Id");
            if (tenantIdHeader != null) {
                try {
                    tenantId = Long.valueOf(tenantIdHeader);
                } catch (NumberFormatException ignored) {}
            }
        }

        return currentUserResolver.resolve(userId, tenantId);
    }
}
