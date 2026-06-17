package com.sunxin.knowledge.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchFilters(
        @JsonProperty("doc_type")
        String docType,

        String industry,

        @JsonProperty("service_line")
        String serviceLine,

        @JsonProperty("year_from")
        Integer yearFrom
) {
}
