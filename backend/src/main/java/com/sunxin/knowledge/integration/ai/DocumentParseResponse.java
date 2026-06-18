package com.sunxin.knowledge.integration.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentParseResponse(
        @JsonProperty("doc_id")
        String docId,

        @JsonProperty("version_id")
        String versionId,

        List<ParsedPage> pages
) {
}
