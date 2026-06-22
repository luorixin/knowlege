package com.sunxin.knowledge.document.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "knowledge.storage.type=minio",
        "knowledge.storage.minio.endpoint=http://localhost:9000",
        "knowledge.storage.minio.access-key=test-access-key",
        "knowledge.storage.minio.secret-key=test-secret-key",
        "knowledge.storage.minio.bucket=knowledge-documents"
})
class MinioStorageContextTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StoredFileResolver storedFileResolver;

    @Test
    void minioModeSelectsMinioStorageAndKeepsResolverAvailable() {
        assertThat(fileStorageService).isInstanceOf(MinioFileStorageService.class);
        assertThat(storedFileResolver).isNotNull();
    }
}
