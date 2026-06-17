package com.sunxin.knowledge.retrieval.rerank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RerankService {

    private static final int DEFAULT_TOP_K = 20;
    private static final int DEFAULT_MAX_CHUNKS_PER_DOCUMENT = 3;

    private final Reranker reranker;

    public RerankService(@Qualifier("ruleBasedReranker") Reranker reranker) {
        this.reranker = reranker;
    }

    public List<RerankedChunk> rerank(RerankRequest request) {
        List<RerankedChunk> ranked = reranker.rerank(request);
        int topK = positiveOrDefault(request == null ? null : request.topK(), DEFAULT_TOP_K);
        int maxChunksPerDocument = positiveOrDefault(
                request == null ? null : request.maxChunksPerDocument(),
                DEFAULT_MAX_CHUNKS_PER_DOCUMENT
        );

        Map<Long, Integer> countsByDoc = new HashMap<>();
        List<RerankedChunk> limited = new ArrayList<>();
        for (RerankedChunk chunk : ranked) {
            Long docId = chunk.docId();
            int docCount = countsByDoc.getOrDefault(docId, 0);
            if (docCount >= maxChunksPerDocument) {
                continue;
            }
            countsByDoc.put(docId, docCount + 1);
            limited.add(chunk.withRank(limited.size() + 1));
            if (limited.size() >= topK) {
                break;
            }
        }
        return limited;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }
}
