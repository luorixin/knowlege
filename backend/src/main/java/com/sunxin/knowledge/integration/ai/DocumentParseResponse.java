package com.sunxin.knowledge.integration.ai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentParseResponse(
        @JsonProperty("doc_id")
        String docId,

        @JsonProperty("version_id")
        String versionId,

        String status,

        List<ParsedPage> pages,

        List<ParsedBlock> blocks,

        List<PageParseError> errors,

        String markdown,

        Map<String, Object> metadata
) {
    public DocumentParseResponse {
        status = status == null || status.isBlank() ? "SUCCESS" : status;
        pages = pages == null ? List.of() : pages;
        blocks = blocks == null ? List.of() : blocks;
        errors = errors == null ? List.of() : errors;
        metadata = metadata == null ? Map.of() : metadata;
    }

    public DocumentParseResponse(String docId, String versionId, List<ParsedPage> pages) {
        this(docId, versionId, "SUCCESS", pages, List.of(), List.of(), null, Map.of());
    }
}
