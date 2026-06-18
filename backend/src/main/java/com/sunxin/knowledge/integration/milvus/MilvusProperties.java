package com.sunxin.knowledge.integration.milvus;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.vector-store")
public class MilvusProperties {

    private String engine = "mock";
    private String endpoint = "http://localhost:19530";
    private String token = "root:Milvus";
    private String collectionPrefix = "knowledge";
    private Integer dimension = 16;
    private Duration timeout = Duration.ofSeconds(10);
    private boolean failFast = false;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCollectionPrefix() {
        return collectionPrefix;
    }

    public void setCollectionPrefix(String collectionPrefix) {
        this.collectionPrefix = collectionPrefix;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public String collectionName(String requestedCollectionName) {
        if (requestedCollectionName != null && !requestedCollectionName.isBlank()) {
            return requestedCollectionName;
        }
        return collectionPrefix + "_chunk_vector";
    }
}
