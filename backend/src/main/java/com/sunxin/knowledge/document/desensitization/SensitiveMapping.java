package com.sunxin.knowledge.document.desensitization;

public record SensitiveMapping(
        Integer pageNo,
        String sectionTitle,
        String sensitiveType,
        String originalValue,
        String maskedValue,
        String ruleName,
        Integer occurrenceIndex
) {
}
