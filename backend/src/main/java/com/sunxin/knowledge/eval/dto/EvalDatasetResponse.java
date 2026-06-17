package com.sunxin.knowledge.eval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunxin.knowledge.persistence.entity.KbEvalDataset;

public record EvalDatasetResponse(
        Long id,
        @JsonProperty("tenant_id")
        Long tenantId,
        @JsonProperty("space_id")
        Long spaceId,
        String name,
        String description,
        String status
) {

    public static EvalDatasetResponse fromEntity(KbEvalDataset dataset) {
        return new EvalDatasetResponse(
                dataset.getId(),
                dataset.getTenantId(),
                dataset.getSpaceId(),
                dataset.getName(),
                dataset.getDescription(),
                dataset.getStatus()
        );
    }
}
