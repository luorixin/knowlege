package com.sunxin.knowledge.retrieval.rerank;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("crossEncoderReranker")
@ConditionalOnProperty(prefix = "knowledge", name = "rerank-provider", havingValue = "private-http", matchIfMissing = false)
public class CrossEncoderReranker implements Reranker {

    private final String rerankEndpoint;
    private final String modelName;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrossEncoderReranker(
            @Value("${knowledge.ai-service.rerank-endpoint:http://localhost:8001/api/v1/rerank}") String rerankEndpoint,
            @Value("${knowledge.rerank-model-name:bge-reranker-v2-m3}") String modelName,
            @Value("${knowledge.ai-service.timeout:60s}") Duration timeout,
            ObjectMapper objectMapper
    ) {
        this.rerankEndpoint = rerankEndpoint;
        this.modelName = modelName;
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public String name() {
        return "cross-encoder-" + modelName;
    }

    @Override
    public List<RerankedChunk> rerank(RerankRequest request) {
        if (request == null || request.candidates() == null || request.candidates().isEmpty()) {
            return List.of();
        }
        
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("query", request.query());
            payload.put("model", modelName);
            payload.put("top_n", request.topK());
            
            List<String> documents = request.candidates().stream()
                    .map(c -> c.content() != null ? c.content() : "")
                    .collect(Collectors.toList());
            payload.put("documents", documents);

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(rerankEndpoint))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Rerank API error: " + response.statusCode() + " " + response.body());
            }

            Map<String, Object> responseMap = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.getOrDefault("results", List.of());
            
            Map<Integer, Double> scoreMap = new HashMap<>();
            for (Map<String, Object> res : results) {
                Integer index = (Integer) res.get("index");
                Double score = ((Number) res.get("relevance_score")).doubleValue();
                scoreMap.put(index, score);
            }

            AtomicInteger rank = new AtomicInteger(1);
            return request.candidates().stream()
                    .map(candidate -> {
                        int originalIndex = request.candidates().indexOf(candidate);
                        double newScore = scoreMap.getOrDefault(originalIndex, 0.0);
                        return new Object() {
                            RerankCandidate c = candidate;
                            double s = newScore;
                        };
                    })
                    .sorted((a, b) -> Double.compare(b.s, a.s))
                    .map(obj -> RerankedChunk.fromCandidate(rank.getAndIncrement(), obj.c, safeScore(obj.s)))
                    .toList();

        } catch (Exception e) {
            // Fallback to retrieval score on error
            AtomicInteger rank = new AtomicInteger(1);
            return request.candidates().stream()
                    .sorted(Comparator.comparing((RerankCandidate candidate) -> safeScore(candidate.retrievalScore())).reversed())
                    .map(candidate -> RerankedChunk.fromCandidate(rank.getAndIncrement(), candidate, safeScore(candidate.retrievalScore())))
                    .toList();
        }
    }

    private static double safeScore(Double value) {
        if (value == null || value.isNaN()) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
