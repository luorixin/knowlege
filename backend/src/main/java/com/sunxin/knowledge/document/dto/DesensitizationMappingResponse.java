package com.sunxin.knowledge.document.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.persistence.entity.KbDesensitizationMapping;

public record DesensitizationMappingResponse(
        Long id,
        Long docId,
        Long versionId,
        Integer pageNo,
        String sectionTitle,
        String sensitiveType,
        String originalValue,
        String maskedValue,
        String ruleName,
        Integer occurrenceIndex,
        LocalDateTime createdAt
) {

    public static DesensitizationMappingResponse fromEntity(KbDesensitizationMapping mapping) {
        return new DesensitizationMappingResponse(
                mapping.getId(),
                mapping.getDocId(),
                mapping.getVersionId(),
                mapping.getPageNo(),
                mapping.getSectionTitle(),
                mapping.getSensitiveType(),
                mapping.getOriginalValue(),
                mapping.getMaskedValue(),
                mapping.getRuleName(),
                mapping.getOccurrenceIndex(),
                mapping.getCreatedAt()
        );
    }
}
