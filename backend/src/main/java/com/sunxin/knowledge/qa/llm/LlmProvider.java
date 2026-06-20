package com.sunxin.knowledge.qa.llm;

public interface LlmProvider {

    String provider();

    String modelName();

    LlmResponse generate(LlmRequest request);

    void stream(LlmRequest request, java.util.function.Consumer<String> onNext, java.util.function.Consumer<LlmResponse> onComplete, java.util.function.Consumer<Throwable> onError);

    QueryRewriteResult rewriteQuery(String originalQuery, java.util.List<ChatMessage> history);
}
