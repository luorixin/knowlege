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

import com.sunxin.knowledge.integration.embedding.EmbeddingProvider;
import com.sunxin.knowledge.integration.embedding.EmbeddingResult;
import com.sunxin.knowledge.integration.milvus.MilvusProperties;
import com.sunxin.knowledge.integration.milvus.MilvusVectorStoreClient;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;

@Primary
@Service
@ConditionalOnProperty(prefix = "knowledge.vector-store", name = "engine", havingValue = "milvus")
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusVectorChunkSearchClient implements VectorChunkSearchClient {

    private final RestClient restClient;
    private final MilvusProperties properties;
    private final EmbeddingProvider embeddingProvider;
    private final KbDocumentChunkRepository chunkRepository;
    private final MockVectorChunkSearchClient fallbackClient;

    public MilvusVectorChunkSearchClient(
            MilvusProperties properties,
            EmbeddingProvider embeddingProvider,
            KbDocumentChunkRepository chunkRepository,
            MockVectorChunkSearchClient fallbackClient
    ) {
        this.properties = properties;
        this.embeddingProvider = embeddingProvider;
        this.chunkRepository = chunkRepository;
        this.fallbackClient = fallbackClient;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        Duration timeout = properties.getTimeout();
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(MilvusVectorStoreClient.trimTrailingSlash(properties.getEndpoint()))
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
    public List<ScoredChunk> search(String query, ChunkSearchScope scope, int limit) {
        if (scope == null || limit <= 0) {
            return List.of();
        }
        try {
            EmbeddingResult embedding = embeddingProvider.embed(query);
            return searchMilvus(embedding.vector(), scope, limit);
        } catch (RuntimeException ex) {
            if (properties.isFailFast()) {
                throw ex;
            }
            return fallbackClient.search(query, scope, limit);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ScoredChunk> searchMilvus(List<Double> vector, ChunkSearchScope scope, int limit) {
        Map<String, Object> response = restClient.post()
                .uri("/v2/vectordb/entities/search")
                .body(searchBody(vector, scope, limit))
                .retrieve()
                .body(Map.class);
        if (response == null) {
            return List.of();
        }
        List<SearchHit> hits = extractHits(response.get("data"));
        if (hits.isEmpty()) {
            return List.of();
        }
        Map<Long, Double> scoresByChunkId = new LinkedHashMap<>();
        for (SearchHit hit : hits) {
            scoresByChunkId.put(hit.chunkId(), hit.score());
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

    private Map<String, Object> searchBody(List<Double> vector, ChunkSearchScope scope, int limit) {
        return Map.of(
                "collectionName", properties.collectionName(null),
                "data", List.of(vector),
                "annsField", "vector",
                "limit", limit,
                "filter", milvusFilter(scope),
                "outputFields", List.of("chunk_id")
        );
    }

    private static String milvusFilter(ChunkSearchScope scope) {
        List<String> filters = new ArrayList<>();
        filters.add("tenant_id == " + scope.tenantId());
        filters.add("space_id == " + scope.spaceId());
        addStringFilter(filters, "doc_type", scope.docType());
        addStringFilter(filters, "industry", scope.industry());
        addStringFilter(filters, "service_line", scope.serviceLine());
        addStringFilter(filters, "block_type", scope.blockType());
        addStringFilter(filters, "content_type", scope.contentType());
        addStringFilter(filters, "parser", scope.parser());
        addStringFilter(filters, "page_parse_mode", scope.pageParseMode());
        if (scope.minConfidence() != null) {
            filters.add("confidence >= " + scope.minConfidence());
        }
        return String.join(" && ", filters);
    }

    private static void addStringFilter(List<String> filters, String field, String value) {
        String normalized = MilvusVectorStoreClient.normalize(value);
        if (normalized != null) {
            filters.add(field + " == \"" + normalized.replace("\"", "\\\"") + "\"");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<SearchHit> extractHits(Object data) {
        List<SearchHit> hits = new ArrayList<>();
        if (data instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof List<?> nested) {
                    hits.addAll(extractHits(nested));
                } else if (item instanceof Map<?, ?> map) {
                    SearchHit hit = toHit((Map<String, Object>) map);
                    if (hit != null) {
                        hits.add(hit);
                    }
                }
            }
        }
        return hits;
    }

    @SuppressWarnings("unchecked")
    private static SearchHit toHit(Map<String, Object> item) {
        Object rawId = item.getOrDefault("chunk_id", item.get("id"));
        if (rawId == null && item.get("entity") instanceof Map<?, ?> entity) {
            rawId = ((Map<String, Object>) entity).getOrDefault("chunk_id", ((Map<String, Object>) entity).get("id"));
        }
        Long chunkId = longValue(rawId);
        if (chunkId == null) {
            return null;
        }
        return new SearchHit(chunkId, score(item.getOrDefault("distance", item.get("score"))));
    }

    private static Long longValue(Object raw) {
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
            double value = number.doubleValue();
            if (value >= 0.0 && value <= 1.0) {
                return value;
            }
            return Math.min(0.99, Math.max(0.0, value / 10.0));
        }
        return 0.0;
    }

    private record SearchHit(Long chunkId, double score) {
    }
}
