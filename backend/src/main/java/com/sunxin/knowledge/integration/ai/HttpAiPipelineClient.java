package com.sunxin.knowledge.integration.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@EnableConfigurationProperties(AiServiceProperties.class)
public class HttpAiPipelineClient implements AiPipelineClient {

    private final RestClient restClient;
    private final AiServiceProperties properties;

    public HttpAiPipelineClient(AiServiceProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = properties.getTimeout();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getEndpoint()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String serviceName() {
        return "fastapi-ai-service";
    }

    @Override
    public DocumentParseResponse parseDocument(DocumentParseRequest request) {
        try {
            return restClient.post()
                    .uri(properties.getParsePath())
                    .body(request)
                    .retrieve()
                    .body(DocumentParseResponse.class);
        } catch (RestClientException ex) {
            throw new AiServiceException("AI document parse request failed", ex);
        }
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        try {
            return restClient.post()
                    .uri(properties.getEmbeddingPath())
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);
        } catch (RestClientException ex) {
            throw new AiServiceException("AI embedding request failed", ex);
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8001";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
