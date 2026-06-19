package com.sunxin.knowledge.integration.ai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ParsedBlock(
        @JsonProperty("block_id")
        String blockId,

        @JsonProperty("block_type")
        String blockType,

        @JsonProperty("page_no")
        Integer pageNo,

        @JsonProperty("section_title")
        String sectionTitle,

        String content,

        String markdown,

        String html,

        List<Double> bbox,

        Double confidence,

        @JsonProperty("image_uri")
        String imageUri,

        @JsonProperty("source_uri")
        String sourceUri,

        Map<String, Object> metadata
) {
}
