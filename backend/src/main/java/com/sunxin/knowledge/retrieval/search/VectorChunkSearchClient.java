package com.sunxin.knowledge.retrieval.search;

import java.util.List;

public interface VectorChunkSearchClient {

    String storeName();

    List<ScoredChunk> search(String query, ChunkSearchScope scope, int limit);
}
