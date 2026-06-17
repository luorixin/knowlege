package com.sunxin.knowledge.document.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(StorageRequest request);

    void delete(String sourceUri);

    record StorageRequest(
            Long tenantId,
            Long spaceId,
            String fileHash,
            String originalFilename,
            MultipartFile file
    ) {
    }
}
