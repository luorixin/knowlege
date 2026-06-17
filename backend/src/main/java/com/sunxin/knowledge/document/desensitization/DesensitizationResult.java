package com.sunxin.knowledge.document.desensitization;

import java.util.List;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

public record DesensitizationResult(
        List<ParsedPageRequest> pages,
        List<SensitiveMapping> mappings
) {
}
