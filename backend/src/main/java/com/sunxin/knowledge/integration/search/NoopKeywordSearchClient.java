package com.sunxin.knowledge.integration.search;

import org.springframework.stereotype.Component;

@Component
public class NoopKeywordSearchClient implements KeywordSearchClient {

    @Override
    public String engineName() {
        return "mock-keyword";
    }

    @Override
    public void indexChunk(IndexedChunkDocument document) {
        // MVP keeps keyword search in the metadata database; this seam allows OpenSearch later.
    }
}
