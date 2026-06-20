package com.sunxin.knowledge.qa.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@ConditionalOnProperty(prefix = "knowledge.llm", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final String baseUrl;
    private final String modelName;
    private final String apiKey;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final org.springframework.core.task.TaskExecutor taskExecutor;

    public OpenAiCompatibleLlmProvider(
            @Value("${knowledge.llm.openai-compatible.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${knowledge.llm.model-name:gpt-3.5-turbo}") String modelName,
            @Value("${knowledge.llm.api-key:}") String apiKey,
            @Value("${knowledge.llm.openai-compatible.timeout:60s}") Duration timeout,
            ObjectMapper objectMapper,
            @Qualifier("applicationTaskExecutor")
            org.springframework.core.task.TaskExecutor taskExecutor
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public String provider() {
        return "openai-compatible";
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        long startedAt = System.currentTimeMillis();
        try {
            String payload = buildPayload(request, false);
            HttpRequest httpRequest = buildRequest(payload);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("LLM API error: " + response.statusCode() + " " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String answer = root.path("choices").path(0).path("message").path("content").asText("");
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

            return new LlmResponse(answer, provider(), modelName(), promptTokens, completionTokens, System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate LLM response", e);
        }
    }

    @Override
    public void stream(LlmRequest request, Consumer<String> onNext, Consumer<LlmResponse> onComplete, Consumer<Throwable> onError) {
        long startedAt = System.currentTimeMillis();
        taskExecutor.execute(() -> {
            try {
                String payload = buildPayload(request, true);
                HttpRequest httpRequest = buildRequest(payload);

                StringBuilder fullAnswer = new StringBuilder();
                int[] tokenUsage = new int[]{0, 0};

                httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                        .thenAccept(response -> {
                            if (response.statusCode() >= 400) {
                                onError.accept(new RuntimeException("LLM API error status: " + response.statusCode()));
                                return;
                            }
                            response.body().forEach(line -> {
                                if (line == null || line.isBlank()) return;
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) return;
                                    try {
                                        JsonNode node = objectMapper.readTree(data);
                                        JsonNode delta = node.path("choices").path(0).path("delta");
                                        if (delta.has("content")) {
                                            String content = delta.get("content").asText();
                                            fullAnswer.append(content);
                                            onNext.accept(content);
                                        }
                                        if (node.has("usage") && !node.get("usage").isNull()) {
                                            tokenUsage[0] = node.path("usage").path("prompt_tokens").asInt(0);
                                            tokenUsage[1] = node.path("usage").path("completion_tokens").asInt(0);
                                        }
                                    } catch (Exception e) {
                                        // Ignore parse errors on partial chunks
                                    }
                                }
                            });

                            LlmResponse llmResponse = new LlmResponse(
                                    fullAnswer.toString(),
                                    provider(),
                                    modelName(),
                                    tokenUsage[0] == 0 ? estimateTokens(request.query() + request.context()) : tokenUsage[0],
                                    tokenUsage[1] == 0 ? estimateTokens(fullAnswer.toString()) : tokenUsage[1],
                                    System.currentTimeMillis() - startedAt
                            );
                            onComplete.accept(llmResponse);
                        })
                        .exceptionally(ex -> {
                            onError.accept(ex);
                            return null;
                        });
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpenAiCompatibleLlmProvider.class);

    @Override
    public QueryRewriteResult rewriteQuery(String originalQuery, List<ChatMessage> history) {
        if (originalQuery == null || originalQuery.isBlank()) return new QueryRewriteResult("", List.of());

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("stream", false);
            // body.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            String systemPrompt = "你是一个查询理解与重写专家。当前用户的查询可能包含代词（如它、他、这个）或需要结合历史对话才能理解。" +
                "请根据历史对话记录，将用户的当前查询重写为一个完整、独立且表意清晰的搜索查询。" +
                "如果查询是一个复杂问题，包含多个独立子问题，请将其拆分为多个子查询(sub_queries)。" +
                "如果查询本身就很独立清晰，请直接原样返回作为 rewritten_query。" +
                "请务必返回 JSON 格式，包含以下字段：\n" +
                "{\n" +
                "  \"rewritten_query\": \"重写后的主查询\",\n" +
                "  \"sub_queries\": [\"子查询1\", \"子查询2\"]\n" +
                "}";

            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null) {
                for (ChatMessage msg : history) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
            messages.add(Map.of("role", "user", "content", originalQuery));

            body.put("messages", messages);
            body.put("temperature", 0.0);

            String payload = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = buildRequest(payload);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Failed to rewrite query, API error: {} {}", response.statusCode(), response.body());
                return new QueryRewriteResult(originalQuery, List.of());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            JsonNode resultNode = objectMapper.readTree(content);
            String rewritten = resultNode.path("rewritten_query").asText(originalQuery);
            List<String> subs = new ArrayList<>();
            if (resultNode.has("sub_queries") && resultNode.get("sub_queries").isArray()) {
                for (JsonNode n : resultNode.get("sub_queries")) {
                    subs.add(n.asText());
                }
            }

            log.info("Query Rewrite: original='{}', rewritten='{}', subQueries={}", originalQuery, rewritten, subs);
            return new QueryRewriteResult(rewritten, subs);

        } catch (Exception e) {
            log.error("Exception during query rewrite", e);
            return new QueryRewriteResult(originalQuery, List.of());
        }
    }

    private String buildPayload(LlmRequest request, boolean stream) throws JsonProcessingException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("stream", stream);

        if (stream) {
            body.put("stream_options", Map.of("include_usage", true));
        }

        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = "你是一个智能企业知识库助手。请根据下方提供的检索文档（如果有的话）来回答用户的问题。\n" +
                "回答要求：\n" +
                "1. 如果提供的文档无法解答问题，请回答“未在当前知识库中找到可靠依据”，不要编造。\n" +
                "2. 请在引用文档的句子末尾标明引用来源，格式为 [引用1]、[引用2]。\n\n" +
                "【检索文档】\n" + (request.context() != null ? request.context() : "");
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (request.history() != null) {
            for (ChatMessage msg : request.history()) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }

        messages.add(Map.of("role", "user", "content", request.query()));

        body.put("messages", messages);
        body.put("temperature", 0.0);
        return objectMapper.writeValueAsString(body);
    }

    private HttpRequest buildRequest(String payload) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(timeout)
                .header("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        return builder.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return text.length() / 2;
    }
}