package com.sunxin.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RetrievalStartupValidatorTest {

    @Test
    void rejectsDatabaseAndMockRetrievalEnginesOutsideLocalProfiles() {
        RetrievalStartupValidator validator = new RetrievalStartupValidator(
                "database",
                "mock",
                new String[]{"prod"}
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retrieval engines");
    }

    @Test
    void allowsDatabaseAndMockRetrievalEnginesForDevProfile() {
        RetrievalStartupValidator validator = new RetrievalStartupValidator(
                "database",
                "mock",
                new String[]{"dev"}
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void allowsProductionRetrievalEnginesOutsideLocalProfiles() {
        RetrievalStartupValidator validator = new RetrievalStartupValidator(
                "opensearch",
                "milvus",
                new String[]{"prod"}
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
