package com.sunxin.knowledge.integration.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.ai-service")
public class AiServiceProperties {

    private String endpoint = "http://localhost:8001";
    private String parsePath = "/api/parse/document";
    private String embeddingPath = "/api/v1/embeddings";
    private String embeddingModel = "mock-embedding-v1";
    private Duration timeout = Duration.ofSeconds(60);

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getParsePath() {
        return parsePath;
    }

    public void setParsePath(String parsePath) {
        this.parsePath = parsePath;
    }

    public String getEmbeddingPath() {
        return embeddingPath;
    }

    public void setEmbeddingPath(String embeddingPath) {
        this.embeddingPath = embeddingPath;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
