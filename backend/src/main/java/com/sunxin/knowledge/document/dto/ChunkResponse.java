package com.sunxin.knowledge.document.dto;

import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;

public record ChunkResponse(
        Long id,
        Integer chunkIndex,
        Integer pageNo,
        String sectionTitle,
        String contentType,
        String content,
        Integer tokenCount,
        String status
) {

    public static ChunkResponse fromEntity(KbDocumentChunk chunk, String contentType) {
        return new ChunkResponse(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getPageNo(),
                chunk.getSectionTitle(),
                contentType,
                chunk.getContent(),
                chunk.getTokenCount(),
                chunk.getStatus()
        );
    }
}
