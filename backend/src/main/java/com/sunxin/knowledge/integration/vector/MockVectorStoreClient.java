package com.sunxin.knowledge.integration.vector;

import org.springframework.stereotype.Component;

@Component
public class MockVectorStoreClient implements VectorStoreClient {

    @Override
    public String storeName() {
        return "mock-vector";
    }

    @Override
    public void upsert(VectorRecord record) {
        // The current retrieval MVP still uses database chunks; real Milvus upsert plugs in here.
    }
}
