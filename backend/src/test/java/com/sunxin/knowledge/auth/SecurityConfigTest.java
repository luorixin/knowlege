package com.sunxin.knowledge.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigTest {

    @Test
    void corsOriginPatternsAreExplicitInsteadOfWildcardByDefault() {
        CorsConfiguration configuration = SecurityConfig.buildCorsConfiguration(
                "http://localhost:5173,http://127.0.0.1:5173"
        );

        assertThat(configuration.getAllowedOriginPatterns())
                .containsExactly("http://localhost:5173", "http://127.0.0.1:5173")
                .doesNotContain("*");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
