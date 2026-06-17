package com.sunxin.knowledge.retrieval.search;

import java.util.Collection;
import java.util.List;

public interface VectorChunkSearchClient {

    String storeName();

    List<ScoredChunk> search(String query, Collection<Long> allowedDocIds, int limit);
}
