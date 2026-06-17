package com.sunxin.knowledge.document.dto;

import java.util.List;

public record RebuildChunksResponse(
        Long documentId,
        Long versionId,
        Integer chunkCount,
        String parseStatus,
        List<ChunkResponse> chunks
) {
}
