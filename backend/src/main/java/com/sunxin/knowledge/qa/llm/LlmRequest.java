package com.sunxin.knowledge.qa.llm;

import java.util.List;

import com.sunxin.knowledge.retrieval.rerank.ContextCitation;

public record LlmRequest(
        String query,
        String context,
        List<ContextCitation> citations
) {
}
