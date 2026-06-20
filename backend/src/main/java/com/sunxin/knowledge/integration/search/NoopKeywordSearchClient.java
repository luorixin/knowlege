package com.sunxin.knowledge.integration.search;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NoopKeywordSearchClient implements KeywordSearchClient {
    private static final Logger log = LoggerFactory.getLogger(NoopKeywordSearchClient.class);

    @Override
    public String engineName() {
        return "mock-keyword";
    }

    @Override
    public void indexChunk(IndexedChunkDocument document) {
        // MVP keeps keyword search in the metadata database; this seam allows OpenSearch later.
    }

    @Override
    public void deleteChunk(String indexName, Long chunkId) {
        log.info("Noop keyword client deleting chunkId={} from index={}", chunkId, indexName);
    }
}