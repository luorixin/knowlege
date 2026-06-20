package com.sunxin.knowledge.integration.vector;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MockVectorStoreClient implements VectorStoreClient {
    private static final Logger log = LoggerFactory.getLogger(MockVectorStoreClient.class);

    @Override
    public String storeName() {
        return "mock-vector";
    }

    @Override
    public void upsert(VectorRecord record) {
        // The current retrieval MVP still uses database chunks; real Milvus upsert plugs in here.
    }

    @Override
    public void delete(String collectionName, Long chunkId) {
        log.info("Mock deleting vector chunkId={} from collection={}", chunkId, collectionName);
    }
}