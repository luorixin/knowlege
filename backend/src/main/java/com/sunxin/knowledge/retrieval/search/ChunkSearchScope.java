package com.sunxin.knowledge.retrieval.search;

import java.time.LocalDateTime;
import java.util.List;

public record ChunkSearchScope(
        Long tenantId,
        Long spaceId,
        Long userId,
        String userSubjectId,
        List<String> roleSubjects,
        boolean spaceOwner,
        String docType,
        String industry,
        String serviceLine,
        LocalDateTime createdFrom
) {
}
