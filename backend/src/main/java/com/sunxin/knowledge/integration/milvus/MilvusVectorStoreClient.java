package com.sunxin.knowledge.integration.milvus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import com.sunxin.knowledge.integration.vector.VectorRecord;
import com.sunxin.knowledge.integration.vector.VectorStoreClient;

@Component
@Primary
@ConditionalOnProperty(prefix = "knowledge.vector-store", name = "engine", havingValue = "milvus")
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusVectorStoreClient implements VectorStoreClient {

    private final RestClient restClient;
    private final MilvusProperties properties;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public MilvusVectorStoreClient(MilvusProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        Duration timeout = properties.getTimeout();
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getEndpoint()))
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .defaultHeader("Request-Timeout", String.valueOf(Math.max(1, timeout.toSeconds())))
                .build();
    }

    @Override
    public String storeName() {
        return "milvus";
    }

    @Override
    public void upsert(VectorRecord record) {
        String collectionName = properties.collectionName(record.collectionName());
        ensureCollection(collectionName, record.embedding().size());
        try {
            restClient.post()
                    .uri("/v2/vectordb/entities/insert")
                    .body(Map.of(
                            "collectionName", collectionName,
                            "data", List.of(toMilvusRow(record))
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            if (properties.isFailFast()) {
                throw ex;
            }
        }
    }

    private void ensureCollection(String collectionName, int dimension) {
        try {
            restClient.post()
                    .uri("/v2/vectordb/collections/create")
                    .body(collectionDefinition(collectionName, dimension))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            String message = ex.getMessage();
            if (properties.isFailFast()
                    || message == null
                    || !(message.contains("already") || message.contains("exist"))) {
                if (properties.isFailFast()) {
                    throw ex;
                }
            }
        }
    }

    private static Map<String, Object> collectionDefinition(String collectionName, int dimension) {
        return Map.of(
                "collectionName", collectionName,
                "schema", Map.of(
                        "autoID", false,
                        "enableDynamicField", true,
                        "fields", List.of(
                                Map.of(
                                        "fieldName", "id",
                                        "dataType", "Int64",
                                        "isPrimary", true,
                                        "autoID", false
                                ),
                                Map.of(
                                        "fieldName", "vector",
                                        "dataType", "FloatVector",
                                        "elementTypeParams", Map.of("dim", dimension)
                                )
                        )
                ),
                "indexParams", List.of(Map.of(
                        "fieldName", "vector",
                        "indexName", "vector_idx",
                        "metricType", "COSINE",
                        "indexType", "AUTOINDEX"
                ))
        );
    }

    private static Map<String, Object> toMilvusRow(VectorRecord record) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", record.chunkId());
        row.put("vector", record.embedding());
        row.put("tenant_id", record.tenantId());
        row.put("space_id", record.spaceId());
        row.put("doc_id", record.docId());
        row.put("version_id", record.versionId());
        row.put("chunk_id", record.chunkId());
        row.put("doc_title", record.docTitle());
        row.put("doc_type", normalize(record.docType()));
        row.put("industry", normalize(record.industry()));
        row.put("service_line", normalize(record.serviceLine()));
        row.put("metadata_json", record.metadataJson());
        
        if (record.metadataJson() != null && !record.metadataJson().isBlank()) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(record.metadataJson());
                if (node.hasNonNull("block_type")) row.put("block_type", normalize(node.get("block_type").asText(null)));
                if (node.hasNonNull("source_uri")) row.put("source_uri", normalize(node.get("source_uri").asText(null)));
                if (node.hasNonNull("image_uri")) row.put("image_uri", normalize(node.get("image_uri").asText(null)));
                if (node.hasNonNull("confidence")) row.put("confidence", node.get("confidence").asDouble());
                if (node.hasNonNull("page_parse_mode")) row.put("page_parse_mode", normalize(node.get("page_parse_mode").asText(null)));
                if (node.hasNonNull("sheet_name")) row.put("sheet_name", normalize(node.get("sheet_name").asText(null)));
                if (node.hasNonNull("table_region")) row.put("table_region", normalize(node.get("table_region").asText(null)));
                if (node.hasNonNull("caption_provider")) row.put("caption_provider", normalize(node.get("caption_provider").asText(null)));
                if (node.hasNonNull("content_type")) row.put("content_type", normalize(node.get("content_type").asText(null)));
                if (node.hasNonNull("parser")) row.put("parser", normalize(node.get("parser").asText(null)));
            } catch (Exception ignore) {
            }
        }
        
        return row;
    }

    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    public static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:19530";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
