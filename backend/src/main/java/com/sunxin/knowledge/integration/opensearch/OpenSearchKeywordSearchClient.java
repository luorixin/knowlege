package com.sunxin.knowledge.integration.opensearch;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.sunxin.knowledge.integration.search.IndexedChunkDocument;
import com.sunxin.knowledge.integration.search.KeywordSearchClient;

@Component
@Primary
@ConditionalOnProperty(prefix = "knowledge.search", name = "engine", havingValue = "opensearch")
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchKeywordSearchClient implements KeywordSearchClient {

    private final RestClient restClient;
    private final OpenSearchProperties properties;

    public OpenSearchKeywordSearchClient(OpenSearchProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        Duration timeout = properties.getTimeout();
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getEndpoint()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String engineName() {
        return "opensearch";
    }

    @Override
    public void indexChunk(IndexedChunkDocument document) {
        String indexName = properties.chunkIndexName(document.indexName());
        ensureIndex(indexName);
        restClient.put()
                .uri("/{index}/_doc/{id}", indexName, document.chunkId())
                .body(toOpenSearchDocument(document))
                .retrieve()
                .toBodilessEntity();
    }

    private void ensureIndex(String indexName) {
        try {
            restClient.put()
                    .uri("/{index}", indexName)
                    .body(indexDefinition())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            String message = ex.getMessage();
            if (message == null || !message.contains("resource_already_exists_exception")) {
                throw ex;
            }
        }
    }

    private static Map<String, Object> toOpenSearchDocument(IndexedChunkDocument document) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("tenant_id", document.tenantId());
        source.put("space_id", document.spaceId());
        source.put("doc_id", document.docId());
        source.put("version_id", document.versionId());
        source.put("chunk_id", document.chunkId());
        source.put("doc_title", document.docTitle());
        source.put("doc_type", normalize(document.docType()));
        source.put("industry", normalize(document.industry()));
        source.put("service_line", normalize(document.serviceLine()));
        source.put("created_at", document.createdAt());
        source.put("chunk_index", document.chunkIndex());
        source.put("page_no", document.pageNo());
        source.put("section_title", document.sectionTitle());
        source.put("content", document.content());
        source.put("metadata_json", document.metadataJson());
        source.put("status", "ACTIVE");
        return source;
    }

    private static Map<String, Object> indexDefinition() {
        return Map.of(
                "mappings", Map.of(
                        "properties", Map.ofEntries(
                                Map.entry("tenant_id", Map.of("type", "long")),
                                Map.entry("space_id", Map.of("type", "long")),
                                Map.entry("doc_id", Map.of("type", "long")),
                                Map.entry("version_id", Map.of("type", "long")),
                                Map.entry("chunk_id", Map.of("type", "long")),
                                Map.entry("doc_title", Map.of("type", "text")),
                                Map.entry("doc_type", Map.of("type", "keyword")),
                                Map.entry("industry", Map.of("type", "keyword")),
                                Map.entry("service_line", Map.of("type", "keyword")),
                                Map.entry("created_at", Map.of("type", "date")),
                                Map.entry("chunk_index", Map.of("type", "integer")),
                                Map.entry("page_no", Map.of("type", "integer")),
                                Map.entry("section_title", Map.of("type", "text")),
                                Map.entry("content", Map.of("type", "text")),
                                Map.entry("status", Map.of("type", "keyword"))
                        )
                )
        );
    }

    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    public static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:9200";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
