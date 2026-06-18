package com.sunxin.knowledge.qa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentCitationResponse(
        @JsonProperty("citation_id")
        Integer citationId,

        @JsonProperty("doc_id")
        Long docId,

        @JsonProperty("doc_title")
        String docTitle,

        @JsonProperty("page_no")
        Integer pageNo,

        @JsonProperty("section_title")
        String sectionTitle,

        @JsonProperty("chunk_content")
        String chunkContent,

        @JsonProperty("source_uri")
        String sourceUri
) {
}
