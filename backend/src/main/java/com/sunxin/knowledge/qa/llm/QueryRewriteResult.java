package com.sunxin.knowledge.qa.llm;

import java.util.List;

public record QueryRewriteResult(
    String rewrittenQuery,
    List<String> subQueries
) {}
