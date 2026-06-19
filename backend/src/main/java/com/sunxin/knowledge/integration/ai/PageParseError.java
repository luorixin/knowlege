package com.sunxin.knowledge.integration.ai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PageParseError(
        @JsonProperty("page_no")
        Integer pageNo,

        String code,

        String message,

        Map<String, Object> metadata
) {
}
