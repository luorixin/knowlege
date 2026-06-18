package com.sunxin.knowledge.integration.embedding;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(prefix = "knowledge.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSION = 16;
    private static final String MODEL_PROVIDER = "mock";
    private static final String MODEL_NAME = "mock-embedding-v1";

    @Override
    public String providerName() {
        return MODEL_PROVIDER;
    }

    @Override
    public EmbeddingResult embed(String text) {
        double[] values = new double[DIMENSION];
        String normalized = text == null ? "" : text.strip();
        if (normalized.isBlank()) {
            return new EmbeddingResult(MODEL_PROVIDER, MODEL_NAME, DIMENSION, zeros());
        }
        for (int i = 0; i < normalized.length(); i++) {
            values[i % DIMENSION] += normalized.charAt(i);
        }
        double norm = 0D;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> vector = new ArrayList<>(DIMENSION);
        for (double value : values) {
            vector.add(norm == 0D ? 0D : value / norm);
        }
        return new EmbeddingResult(MODEL_PROVIDER, MODEL_NAME, DIMENSION, vector);
    }

    private static List<Double> zeros() {
        List<Double> vector = new ArrayList<>(DIMENSION);
        for (int i = 0; i < DIMENSION; i++) {
            vector.add(0D);
        }
        return vector;
    }
}
