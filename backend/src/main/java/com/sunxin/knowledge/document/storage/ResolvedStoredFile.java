package com.sunxin.knowledge.document.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResolvedStoredFile implements AutoCloseable {

    private final Path path;
    private final boolean temporary;

    private ResolvedStoredFile(Path path, boolean temporary) {
        this.path = path.toAbsolutePath().normalize();
        this.temporary = temporary;
    }

    public static ResolvedStoredFile borrowed(Path path) {
        return new ResolvedStoredFile(path, false);
    }

    public static ResolvedStoredFile temporary(Path path) {
        return new ResolvedStoredFile(path, true);
    }

    public Path path() {
        return path;
    }

    public boolean temporary() {
        return temporary;
    }

    @Override
    public void close() {
        if (!temporary) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete temporary parse file", ex);
        }
    }
}
