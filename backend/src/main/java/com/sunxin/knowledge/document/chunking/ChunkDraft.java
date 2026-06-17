package com.sunxin.knowledge.document.chunking;

import java.util.Map;

public record ChunkDraft(
        int chunkIndex,
        Integer pageNo,
        String sectionTitle,
        String contentType,
        String content,
        Map<String, Object> metadata
) {
}
