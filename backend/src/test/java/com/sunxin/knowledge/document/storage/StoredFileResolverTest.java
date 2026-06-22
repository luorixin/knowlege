package com.sunxin.knowledge.document.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sunxin.knowledge.common.error.BadRequestException;

class StoredFileResolverTest {

    @TempDir
    Path tempDirectory;

    @Test
    void localResolutionBorrowsFileWithoutDeletingIt() throws Exception {
        LocalStorageProperties properties = new LocalStorageProperties();
        properties.setLocalRoot(tempDirectory.toString());
        Path source = tempDirectory.resolve("1/2/sample.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "local content");

        try (ResolvedStoredFile resolved = new LocalStoredFileResolver(properties)
                .resolve("local://1/2/sample.txt")) {
            assertThat(resolved.path()).isEqualTo(source);
            assertThat(resolved.temporary()).isFalse();
        }

        assertThat(source).exists();
    }

    @Test
    void localResolutionRejectsTraversalOutsideStorageRoot() {
        LocalStorageProperties properties = new LocalStorageProperties();
        properties.setLocalRoot(tempDirectory.toString());

        assertThatThrownBy(() -> new LocalStoredFileResolver(properties).resolve("local://../secret.txt"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid local file path");
    }

    @Test
    void minioResolutionDownloadsToTemporaryFileAndDeletesItOnClose() throws Exception {
        MinioStorageProperties properties = new MinioStorageProperties();
        properties.setBucket("knowledge-documents");
        MinioObjectReader reader = (bucket, objectName) -> {
            assertThat(bucket).isEqualTo("knowledge-documents");
            assertThat(objectName).isEqualTo("1/2/sample.pdf");
            return new ByteArrayInputStream("pdf bytes".getBytes(StandardCharsets.UTF_8));
        };
        MinioStoredFileResolver resolver = new MinioStoredFileResolver(properties, reader);

        Path downloaded;
        try (ResolvedStoredFile resolved = resolver.resolve("minio://knowledge-documents/1/2/sample.pdf")) {
            downloaded = resolved.path();
            assertThat(resolved.temporary()).isTrue();
            assertThat(downloaded).exists();
            assertThat(Files.readString(downloaded)).isEqualTo("pdf bytes");
        }

        assertThat(downloaded).doesNotExist();
    }

    @Test
    void minioResolutionRejectsUnexpectedBucketAndTraversalKey() {
        MinioStorageProperties properties = new MinioStorageProperties();
        properties.setBucket("knowledge-documents");
        MinioStoredFileResolver resolver = new MinioStoredFileResolver(
                properties,
                (bucket, objectName) -> new ByteArrayInputStream(new byte[0])
        );

        assertThatThrownBy(() -> resolver.resolve("minio://another-bucket/1/sample.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> resolver.resolve("minio://knowledge-documents/../sample.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("object key");
    }
}
