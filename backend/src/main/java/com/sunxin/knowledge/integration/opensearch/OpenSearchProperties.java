package com.sunxin.knowledge.integration.opensearch;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.search")
public class OpenSearchProperties {

    private String engine = "database";
    private String endpoint = "http://localhost:9200";
    private String indexPrefix = "knowledge";
    private Duration timeout = Duration.ofSeconds(10);

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

    public String getIndexPrefix() {
        return indexPrefix;
    }

    public void setIndexPrefix(String indexPrefix) {
        this.indexPrefix = indexPrefix;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String chunkIndexName(String requestedIndexName) {
        if (requestedIndexName != null && !requestedIndexName.isBlank()) {
            return requestedIndexName;
        }
        return indexPrefix + "_chunk_keyword";
    }
}
