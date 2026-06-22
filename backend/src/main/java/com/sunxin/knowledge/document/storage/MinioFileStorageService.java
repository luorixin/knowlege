package com.sunxin.knowledge.document.storage;

import java.io.InputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.document.support.DocumentType;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@Service
@ConditionalOnProperty(prefix = "knowledge.storage", name = "type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public MinioFileStorageService(MinioClient minioClient, MinioStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public StoredFile store(StorageRequest request) {
        String extension = DocumentType.extensionOf(request.originalFilename())
                .map(value -> "." + value)
                .orElse("");
        String objectName = request.tenantId() + "/" + request.spaceId() + "/" + request.fileHash() + extension;
        try {
            ensureBucket();
            try (InputStream input = request.file().getInputStream()) {
                String contentType = request.file().getContentType();
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .stream(input, request.file().getSize(), -1L)
                        .contentType(contentType == null || contentType.isBlank()
                                ? "application/octet-stream"
                                : contentType)
                        .build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store uploaded file in MinIO", ex);
        }
        String sourceUri = "minio://" + properties.getBucket() + "/" + objectName;
        return new StoredFile(sourceUri, sourceUri, request.file().getSize());
    }

    @Override
    public void delete(String sourceUri) {
        MinioLocation location = parseOwnedLocation(sourceUri);
        if (location == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(location.bucket())
                    .object(location.objectName())
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete MinIO object", ex);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
        }
    }

    private MinioLocation parseOwnedLocation(String sourceUri) {
        String prefix = "minio://" + properties.getBucket() + "/";
        if (sourceUri == null || !sourceUri.startsWith(prefix)) {
            return null;
        }
        String objectName = sourceUri.substring(prefix.length());
        return objectName.isBlank() ? null : new MinioLocation(properties.getBucket(), objectName);
    }

    private record MinioLocation(String bucket, String objectName) {
    }
}
