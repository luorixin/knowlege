package com.sunxin.knowledge.integration.ai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ParsedPage(
        @JsonProperty("page_no")
        Integer pageNo,

        @JsonProperty("section_title")
        String sectionTitle,

        @JsonProperty("content_type")
        String contentType,

        String content,

        Map<String, Object> metadata
) {
}
