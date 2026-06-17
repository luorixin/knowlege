package com.sunxin.knowledge.qa.llm;

public interface LlmProvider {

    String provider();

    String modelName();

    LlmResponse generate(LlmRequest request);
}
