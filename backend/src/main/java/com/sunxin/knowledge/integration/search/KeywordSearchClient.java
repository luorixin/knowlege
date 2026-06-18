package com.sunxin.knowledge.integration.search;

public interface KeywordSearchClient {

    String engineName();

    void indexChunk(IndexedChunkDocument document);
}
