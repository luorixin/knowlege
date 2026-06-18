package com.sunxin.knowledge.document.storage;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.common.error.BadRequestException;

@Service
@EnableConfigurationProperties(LocalStorageProperties.class)
public class LocalStoredFileResolver {

    private static final String LOCAL_PREFIX = "local://";

    private final Path rootDirectory;

    public LocalStoredFileResolver(LocalStorageProperties properties) {
        this.rootDirectory = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
    }

    public Path resolve(String sourceUri) {
        if (sourceUri == null || !sourceUri.startsWith(LOCAL_PREFIX)) {
            throw new BadRequestException("Only local file storage can be parsed by the MVP task executor");
        }
        String relativePath = sourceUri.substring(LOCAL_PREFIX.length());
        Path target = rootDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new BadRequestException("Invalid local file path");
        }
        return target;
    }
}
