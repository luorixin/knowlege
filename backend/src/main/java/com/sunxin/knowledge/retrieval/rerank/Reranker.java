package com.sunxin.knowledge.retrieval.rerank;

import java.util.List;

public interface Reranker {

    String name();

    List<RerankedChunk> rerank(RerankRequest request);
}
