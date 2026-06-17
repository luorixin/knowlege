package com.sunxin.knowledge.document.storage;

public record StoredFile(
        String sourceUri,
        String storagePath,
        long size
) {
}
