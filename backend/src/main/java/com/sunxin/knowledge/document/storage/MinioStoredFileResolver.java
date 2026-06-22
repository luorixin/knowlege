package com.sunxin.knowledge.document.storage;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sunxin.knowledge.common.error.BadRequestException;

@Component
@ConditionalOnProperty(prefix = "knowledge.storage", name = "type", havingValue = "minio")
public class MinioStoredFileResolver {

    private static final String MINIO_SCHEME = "minio";

    private final MinioStorageProperties properties;
    private final MinioObjectReader objectReader;

    public MinioStoredFileResolver(MinioStorageProperties properties, MinioObjectReader objectReader) {
        this.properties = properties;
        this.objectReader = objectReader;
    }

    public ResolvedStoredFile resolve(String sourceUri) {
        MinioLocation location = parseLocation(sourceUri);
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("knowledge-parse-", suffix(location.objectName()));
            try (InputStream input = objectReader.open(location.bucket(), location.objectName())) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return ResolvedStoredFile.temporary(temporaryFile);
        } catch (Exception ex) {
            deleteQuietly(temporaryFile);
            throw new IllegalStateException("Failed to download MinIO object for parsing", ex);
        }
    }

    private MinioLocation parseLocation(String sourceUri) {
        URI uri;
        try {
            uri = URI.create(sourceUri);
        } catch (RuntimeException ex) {
            throw new BadRequestException("Invalid MinIO source URI");
        }
        String bucket = uri.getHost();
        String objectName = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (!MINIO_SCHEME.equalsIgnoreCase(uri.getScheme()) || bucket == null || objectName.isBlank()) {
            throw new BadRequestException("Invalid MinIO source URI");
        }
        if (!properties.getBucket().equals(bucket)) {
            throw new BadRequestException("MinIO source URI bucket is not allowed");
        }
        if (objectName.equals("..") || objectName.startsWith("../") || objectName.contains("/../")) {
            throw new BadRequestException("Invalid MinIO object key");
        }
        return new MinioLocation(bucket, objectName);
    }

    private static String suffix(String objectName) {
        int extensionIndex = objectName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == objectName.length() - 1) {
            return ".bin";
        }
        String suffix = objectName.substring(extensionIndex);
        return suffix.length() <= 16 ? suffix : ".bin";
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // The original download error remains the actionable failure.
        }
    }

    private record MinioLocation(String bucket, String objectName) {
    }
}
