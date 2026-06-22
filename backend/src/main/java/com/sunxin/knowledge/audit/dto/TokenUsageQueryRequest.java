package com.sunxin.knowledge.audit.dto;

import java.time.LocalDateTime;

public record TokenUsageQueryRequest(
        String modelProvider,
        String modelName,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        Integer page,
        Integer size
) {
}
