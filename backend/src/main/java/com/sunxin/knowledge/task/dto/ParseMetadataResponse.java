package com.sunxin.knowledge.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public record ParseMetadataResponse(
        String parser,
        @JsonProperty("page_count")
        Integer pageCount,
        @JsonProperty("block_count")
        Integer blockCount,
        @JsonProperty("error_count")
        Integer errorCount,
        @JsonProperty("page_modes")
        List<String> pageModes,
        List<Object> errors
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ParseMetadataResponse safeParse(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(metadataJson);
            
            String parser = root.has("parser") ? root.get("parser").asText() : null;
            Integer pageCount = root.has("page_count") ? root.get("page_count").asInt() : null;
            Integer blockCount = root.has("block_count") ? root.get("block_count").asInt() : null;
            Integer errorCount = root.has("error_count") ? root.get("error_count").asInt() : null;
            
            List<String> pageModes = new ArrayList<>();
            if (root.has("page_modes") && root.get("page_modes").isArray()) {
                root.get("page_modes").forEach(node -> pageModes.add(node.asText()));
            }
            
            List<Object> errors = new ArrayList<>();
            if (root.has("errors") && root.get("errors").isArray()) {
                root.get("errors").forEach(node -> errors.add(node));
            }
            
            return new ParseMetadataResponse(parser, pageCount, blockCount, errorCount, pageModes, errors);
        } catch (JsonProcessingException e) {
            return null; // Ignore malformed JSON
        }
    }
}
