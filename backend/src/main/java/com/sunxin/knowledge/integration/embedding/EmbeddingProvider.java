package com.sunxin.knowledge.integration.embedding;

public interface EmbeddingProvider {

    String providerName();

    EmbeddingResult embed(String text);
}
