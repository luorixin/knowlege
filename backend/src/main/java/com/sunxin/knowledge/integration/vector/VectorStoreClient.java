package com.sunxin.knowledge.integration.vector;

public interface VectorStoreClient {

    String storeName();

    void upsert(VectorRecord record);
}
