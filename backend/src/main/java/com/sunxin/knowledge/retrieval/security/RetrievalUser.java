package com.sunxin.knowledge.retrieval.security;

public record RetrievalUser(
        Long userId,
        Long tenantId
) {
}
