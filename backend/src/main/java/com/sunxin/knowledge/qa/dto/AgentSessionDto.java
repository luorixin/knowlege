package com.sunxin.knowledge.qa.dto;

import java.time.LocalDateTime;

public record AgentSessionDto(
        Long id,
        Long spaceId,
        String title,
        String status,
        LocalDateTime createdAt
) {
}
