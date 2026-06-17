package com.sunxin.knowledge.document.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.document.support.DocumentType;

@Service
@EnableConfigurationProperties(LocalStorageProperties.class)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDirectory;

    public LocalFileStorageService(LocalStorageProperties properties) {
        this.rootDirectory = Path.of(properties.getLocalRoot());
    }

    @Override
    public StoredFile store(StorageRequest request) {
        String extension = DocumentType.extensionOf(request.originalFilename())
                .map(value -> "." + value)
                .orElse("");
        String storedFilename = request.fileHash() + extension;
        Path relativePath = Path.of(
                String.valueOf(request.tenantId()),
                String.valueOf(request.spaceId()),
                storedFilename
        );
        Path target = rootDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(rootDirectory.normalize())) {
            throw new BadRequestException("Invalid storage path");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = request.file().getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }

        String sourceUri = "local://"
                + request.tenantId() + "/"
                + request.spaceId() + "/"
                + storedFilename;
        return new StoredFile(sourceUri, target.toString(), request.file().getSize());
    }

    @Override
    public void delete(String sourceUri) {
        if (sourceUri == null || !sourceUri.startsWith("local://")) {
            return;
        }
        String relative = sourceUri.substring("local://".length());
        Path target = rootDirectory.resolve(relative).normalize();
        if (!target.startsWith(rootDirectory.normalize())) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete stored file", ex);
        }
    }
}
