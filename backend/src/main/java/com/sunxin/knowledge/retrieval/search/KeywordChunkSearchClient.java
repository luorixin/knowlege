package com.sunxin.knowledge.retrieval.search;

import java.util.List;

public interface KeywordChunkSearchClient {

    String engineName();

    List<ScoredChunk> search(String query, ChunkSearchScope scope, int limit);
}
