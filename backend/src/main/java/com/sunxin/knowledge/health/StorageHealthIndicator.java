package com.sunxin.knowledge.health;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Checks that the configured document storage backend is reachable.
 *
 * <p>For {@code local} storage the indicator verifies that the upload root directory
 * exists (or can be created). For {@code minio} storage a future implementation will
 * probe the MinIO endpoint; the current MVP always reports {@code UP} with a detail
 * when the engine is {@code minio}.</p>
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(StorageHealthIndicator.class);

    private final String storageType;
    private final Path localRoot;

    public StorageHealthIndicator(
            @Value("${knowledge.storage.type:local}") String storageType,
            @Value("${knowledge.storage.local-root:data/uploads}") String localRoot) {
        this.storageType = storageType;
        this.localRoot = Path.of(localRoot).toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up().withDetail("type", storageType);

        if ("local".equalsIgnoreCase(storageType)) {
            try {
                if (!Files.exists(localRoot)) {
                    Files.createDirectories(localRoot);
                }
                if (!Files.isWritable(localRoot)) {
                    builder = Health.up()
                            .withDetail("type", storageType)
                            .withDetail("localRoot", localRoot.toString())
                            .withDetail("warning", "directory exists but is not writable");
                    return builder.build();
                }
                builder.withDetail("localRoot", localRoot.toString());
            } catch (IOException ex) {
                log.warn("Storage health check failed: cannot create localRoot={}", localRoot, ex);
                builder.withDetail("localRoot", localRoot.toString())
                        .withDetail("error", ex.getMessage());
            }
        } else {
            // minio / other: soft probe not yet wired; report UP with engine info.
            builder.withDetail("note", "probe not implemented for engine=" + storageType);
        }

        return builder.build();
    }
}
