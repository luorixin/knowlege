package com.sunxin.knowledge.knowledgebase.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.persistence.entity.KbSpace;

public record KnowledgeSpaceResponse(
        Long id,
        Long tenantId,
        String name,
        String description,
        Long ownerUserId,
        String visibility,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static KnowledgeSpaceResponse fromEntity(KbSpace space) {
        return new KnowledgeSpaceResponse(
                space.getId(),
                space.getTenantId(),
                space.getName(),
                space.getDescription(),
                space.getOwnerUserId(),
                space.getVisibility(),
                space.getStatus(),
                space.getCreatedAt(),
                space.getUpdatedAt()
        );
    }
}
