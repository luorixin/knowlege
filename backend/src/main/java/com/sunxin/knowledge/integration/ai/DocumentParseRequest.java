package com.sunxin.knowledge.integration.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DocumentParseRequest(
        @JsonProperty("doc_id")
        String docId,

        @JsonProperty("version_id")
        String versionId,

        @JsonProperty("file_path")
        String filePath,

        @JsonProperty("file_type")
        String fileType
) {
}
