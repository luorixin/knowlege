package com.sunxin.knowledge.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RetrievalSearchResult(
        @JsonProperty("chunk_id")
        Long chunkId,

        @JsonProperty("doc_id")
        Long docId,

        @JsonProperty("doc_title")
        String docTitle,

        @JsonProperty("page_no")
        Integer pageNo,

        @JsonProperty("section_title")
        String sectionTitle,

        String content,

        Double score,

        @JsonProperty("source_uri")
        String sourceUri
) {
}
