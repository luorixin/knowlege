package com.sunxin.knowledge.document.storage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.common.error.BadRequestException;

@Service
public class StoredFileResolver {

    private final LocalStoredFileResolver localResolver;
    private final ObjectProvider<MinioStoredFileResolver> minioResolverProvider;

    public StoredFileResolver(
            LocalStoredFileResolver localResolver,
            ObjectProvider<MinioStoredFileResolver> minioResolverProvider
    ) {
        this.localResolver = localResolver;
        this.minioResolverProvider = minioResolverProvider;
    }

    public ResolvedStoredFile resolve(String sourceUri) {
        if (sourceUri != null && sourceUri.startsWith("local://")) {
            return localResolver.resolve(sourceUri);
        }
        if (sourceUri != null && sourceUri.startsWith("minio://")) {
            MinioStoredFileResolver minioResolver = minioResolverProvider.getIfAvailable();
            if (minioResolver == null) {
                throw new BadRequestException("MinIO storage is not enabled");
            }
            return minioResolver.resolve(sourceUri);
        }
        throw new BadRequestException("Unsupported document storage URI");
    }
}
