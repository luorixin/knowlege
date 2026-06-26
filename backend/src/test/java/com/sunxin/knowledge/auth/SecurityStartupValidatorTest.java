package com.sunxin.knowledge.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecurityStartupValidatorTest {

    @Test
    void rejectsDefaultJwtSecretWhenSecurityIsEnabledOutsideLocalProfiles() {
        SecurityStartupValidator validator = new SecurityStartupValidator(
                true,
                "KnowledgePlatformMVPSecretKey1234567890",
                new String[]{"prod"}
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret");
    }

    @Test
    void allowsDefaultJwtSecretForDevProfile() {
        SecurityStartupValidator validator = new SecurityStartupValidator(
                true,
                "KnowledgePlatformMVPSecretKey1234567890",
                new String[]{"dev"}
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
