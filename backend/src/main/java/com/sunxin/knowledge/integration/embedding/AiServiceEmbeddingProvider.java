package com.sunxin.knowledge.integration.embedding;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.integration.ai.AiPipelineClient;
import com.sunxin.knowledge.integration.ai.AiServiceProperties;
import com.sunxin.knowledge.integration.ai.EmbeddingItem;
import com.sunxin.knowledge.integration.ai.EmbeddingRequest;
import com.sunxin.knowledge.integration.ai.EmbeddingResponse;

@Component
@ConditionalOnProperty(prefix = "knowledge.embedding", name = "provider", havingValue = "ai-service")
public class AiServiceEmbeddingProvider implements EmbeddingProvider {

    private final AiPipelineClient aiPipelineClient;
    private final AiServiceProperties properties;

    public AiServiceEmbeddingProvider(AiPipelineClient aiPipelineClient, AiServiceProperties properties) {
        this.aiPipelineClient = aiPipelineClient;
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "ai-service";
    }

    @Override
    public EmbeddingResult embed(String text) {
        EmbeddingResponse response = aiPipelineClient.embed(new EmbeddingRequest(
                List.of(text == null ? "" : text),
                properties.getEmbeddingModel()
        ));
        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new BadRequestException("AI embedding service returned no vectors");
        }
        EmbeddingItem item = response.embeddings().getFirst();
        if (item.vector() == null || item.vector().isEmpty()) {
            throw new BadRequestException("AI embedding service returned an empty vector");
        }
        return new EmbeddingResult(providerName(), response.model(), response.dimension(), item.vector());
    }
}
