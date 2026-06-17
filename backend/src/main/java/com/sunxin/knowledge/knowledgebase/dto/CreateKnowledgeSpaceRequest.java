package com.sunxin.knowledge.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeSpaceRequest(
        @NotNull Long tenantId,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 1024) String description,
        Long ownerUserId,
        @Size(max = 32) String visibility
) {
}
