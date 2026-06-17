package com.sunxin.knowledge.document.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParsedPageRequest(
        @NotNull
        @Min(1)
        Integer pageNo,

        String sectionTitle,

        String contentType,

        @NotBlank
        String content,

        Map<String, Object> metadata
) {
}
