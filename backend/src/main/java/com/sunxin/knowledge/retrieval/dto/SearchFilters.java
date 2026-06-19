package com.sunxin.knowledge.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchFilters(
        @JsonProperty("doc_type")
        String docType,

        String industry,

        @JsonProperty("service_line")
        String serviceLine,

        @JsonProperty("year_from")
        Integer yearFrom,

        @JsonProperty("block_type")
        String blockType,

        @JsonProperty("content_type")
        String contentType,

        @JsonProperty("min_confidence")
        Double minConfidence,

        String parser,

        @JsonProperty("page_parse_mode")
        String pageParseMode
) {
}
