package com.sunxin.knowledge.retrieval.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.retrieval")
public class RetrievalProperties {

    private Duration branchTimeout = Duration.ofSeconds(3);

    public Duration getBranchTimeout() {
        return branchTimeout;
    }

    public void setBranchTimeout(Duration branchTimeout) {
        if (branchTimeout == null || branchTimeout.isZero() || branchTimeout.isNegative()) {
            throw new IllegalArgumentException("Retrieval branch timeout must be positive");
        }
        this.branchTimeout = branchTimeout;
    }
}
