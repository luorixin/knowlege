package com.sunxin.knowledge.retrieval.search;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.sunxin.knowledge.integration.opensearch.OpenSearchKeywordSearchClient;
import com.sunxin.knowledge.integration.opensearch.OpenSearchProperties;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;

@Primary
@Service
@ConditionalOnProperty(prefix = "knowledge.search", name = "engine", havingValue = "opensearch")
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchChunkSearchClient implements KeywordChunkSearchClient {

    private final RestClient restClient;
    private final OpenSearchProperties properties;
    private final KbDocumentChunkRepository chunkRepository;
    private final DatabaseKeywordChunkSearchClient fallbackClient;

    public OpenSearchChunkSearchClient(
            OpenSearchProperties properties,
            KbDocumentChunkRepository chunkRepository,
            DatabaseKeywordChunkSearchClient fallbackClient
    ) {
        this.properties = properties;
        this.chunkRepository = chunkRepository;
        this.fallbackClient = fallbackClient;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        Duration timeout = properties.getTimeout();
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(OpenSearchKeywordSearchClient.trimTrailingSlash(properties.getEndpoint()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String engineName() {
        return "opensearch";
    }

    @Override
    public List<ScoredChunk> search(String query, ChunkSearchScope scope, int limit) {
        if (scope == null || limit <= 0) {
            return List.of();
        }
        if (!scope.spaceOwner()) {
            return fallbackClient.search(query, scope, limit);
        }
        try {
            return searchOpenSearch(query, scope, limit);
        } catch (RestClientException ex) {
            return fallbackClient.search(query, scope, limit);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ScoredChunk> searchOpenSearch(String query, ChunkSearchScope scope, int limit) {
        Map<String, Object> body = searchBody(query, scope, limit);
        Map<String, Object> response = restClient.post()
                .uri("/{index}/_search", properties.chunkIndexName(null))
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null) {
            return List.of();
        }
        Map<String, Object> hits = (Map<String, Object>) response.get("hits");
        if (hits == null) {
            return List.of();
        }
        List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.getOrDefault("hits", List.of());
        Map<Long, Double> scoresByChunkId = new LinkedHashMap<>();
        for (Map<String, Object> hit : hitList) {
            Long chunkId = chunkId(hit);
            if (chunkId != null) {
                scoresByChunkId.put(chunkId, score(hit.get("_score")));
            }
        }
        if (scoresByChunkId.isEmpty()) {
            return List.of();
        }
        Map<Long, KbDocumentChunk> chunksById = chunkRepository.findAllById(scoresByChunkId.keySet()).stream()
                .collect(Collectors.toMap(KbDocumentChunk::getId, Function.identity()));
        return scoresByChunkId.entrySet().stream()
                .map(entry -> {
                    KbDocumentChunk chunk = chunksById.get(entry.getKey());
                    return chunk == null ? null : new ScoredChunk(chunk, entry.getValue());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(limit)
                .toList();
    }

    private static Map<String, Object> searchBody(String query, ChunkSearchScope scope, int limit) {
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(term("tenant_id", scope.tenantId()));
        filters.add(term("space_id", scope.spaceId()));
        filters.add(term("status", "ACTIVE"));
        addOptionalFilter(filters, "doc_type", scope.docType());
        addOptionalFilter(filters, "industry", scope.industry());
        addOptionalFilter(filters, "service_line", scope.serviceLine());
        addOptionalFilter(filters, "block_type", scope.blockType());
        addOptionalFilter(filters, "content_type", scope.contentType());
        addOptionalFilter(filters, "parser", scope.parser());
        addOptionalFilter(filters, "page_parse_mode", scope.pageParseMode());
        if (scope.createdFrom() != null) {
            filters.add(Map.of("range", Map.of("created_at", Map.of("gte", scope.createdFrom().toString()))));
        }
        if (scope.minConfidence() != null) {
            filters.add(Map.of("range", Map.of("confidence", Map.of("gte", scope.minConfidence()))));
        }
        return Map.of(
                "size", limit,
                "query", Map.of(
                        "bool", Map.of(
                                "must", List.of(Map.of(
                                        "multi_match", Map.of(
                                                "query", query,
                                                "fields", List.of("content^2", "section_title^1.4", "doc_title")
                                        )
                                )),
                                "filter", filters
                        )
                )
        );
    }

    private static Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private static void addOptionalFilter(List<Map<String, Object>> filters, String field, String value) {
        String normalized = OpenSearchKeywordSearchClient.normalize(value);
        if (normalized != null) {
            filters.add(term(field, normalized));
        }
    }

    @SuppressWarnings("unchecked")
    private static Long chunkId(Map<String, Object> hit) {
        Map<String, Object> source = (Map<String, Object>) hit.get("_source");
        Object raw = source == null ? hit.get("_id") : source.getOrDefault("chunk_id", hit.get("_id"));
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String value && !value.isBlank()) {
            return Long.valueOf(value);
        }
        return null;
    }

    private static double score(Object raw) {
        if (raw instanceof Number number) {
            return Math.min(0.99, Math.max(0.0, number.doubleValue() / 10.0));
        }
        return 0.0;
    }
}
