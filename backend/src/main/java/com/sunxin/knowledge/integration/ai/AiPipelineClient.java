package com.sunxin.knowledge.integration.ai;

public interface AiPipelineClient {

    String serviceName();

    DocumentParseResponse parseDocument(DocumentParseRequest request);

    EmbeddingResponse embed(EmbeddingRequest request);
}
