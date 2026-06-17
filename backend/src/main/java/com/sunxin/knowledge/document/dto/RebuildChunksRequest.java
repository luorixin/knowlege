package com.sunxin.knowledge.document.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public record RebuildChunksRequest(
        @Min(1)
        Integer chunkSize,

        @Min(0)
        Integer overlap,

        @Valid
        @NotEmpty
        List<ParsedPageRequest> pages
) {
}
