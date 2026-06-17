package com.sunxin.knowledge.document.dto;

import java.time.LocalDateTime;

import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;

public record DocumentVersionResponse(
        Long id,
        Integer versionNo,
        String sourceUri,
        String fileHash,
        Long fileSize,
        String parseStatus,
        String desensitizeStatus,
        LocalDateTime desensitizedAt,
        Integer chunkCount,
        Integer totalTokens,
        String status,
        LocalDateTime createdAt
) {

    public static DocumentVersionResponse fromEntity(KbDocumentVersion version) {
        if (version == null) {
            return null;
        }
        return new DocumentVersionResponse(
                version.getId(),
                version.getVersionNo(),
                version.getSourceUri(),
                version.getFileHash(),
                version.getFileSize(),
                version.getParseStatus(),
                version.getDesensitizeStatus(),
                version.getDesensitizedAt(),
                version.getChunkCount(),
                version.getTotalTokens(),
                version.getStatus(),
                version.getCreatedAt()
        );
    }
}
