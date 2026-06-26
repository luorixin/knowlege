package com.sunxin.knowledge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.ServletWebRequest;

import com.sunxin.knowledge.persistence.repository.KbUserRoleRepository;

class CurrentUserArgumentResolverTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ignoresClientIdentityHeadersWhenHeaderFallbackIsDisabled() {
        CurrentUserResolver resolver = new CurrentUserResolver(null);
        CurrentUserArgumentResolver argumentResolver = new CurrentUserArgumentResolver(resolver, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-Tenant-Id", "1001");

        CurrentUser user = (CurrentUser) argumentResolver.resolveArgument(
                null,
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(user.userId()).isEqualTo(0L);
        assertThat(user.tenantId()).isNull();
        assertThat(user.roleCodes()).isEqualTo(Set.of());
    }

    @Test
    void prefersJwtIdentityOverClientIdentityHeaders() {
        KbUserRoleRepository userRoleRepository = org.mockito.Mockito.mock(KbUserRoleRepository.class);
        when(userRoleRepository.findActiveRoleCodes(1001L, 42L)).thenReturn(Set.of("VIEWER"));
        CurrentUserResolver resolver = new CurrentUserResolver(userRoleRepository);
        CurrentUserArgumentResolver argumentResolver = new CurrentUserArgumentResolver(resolver, true);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("42")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("userId", 42L)
                .claim("tenantId", 1001L)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of())
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "99");
        request.addHeader("X-Tenant-Id", "2002");

        CurrentUser user = (CurrentUser) argumentResolver.resolveArgument(
                null,
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.tenantId()).isEqualTo(1001L);
        assertThat(user.roleCodes()).containsExactly("viewer");
    }
}
