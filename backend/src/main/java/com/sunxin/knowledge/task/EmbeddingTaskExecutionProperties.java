package com.sunxin.knowledge.task;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.tasks.embedding")
public class EmbeddingTaskExecutionProperties {

    private boolean autoRun = false;
    private Duration fixedDelay = Duration.ofSeconds(10);

    public boolean isAutoRun() {
        return autoRun;
    }

    public void setAutoRun(boolean autoRun) {
        this.autoRun = autoRun;
    }

    public Duration getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Duration fixedDelay) {
        this.fixedDelay = fixedDelay;
    }
}
