package com.sunxin.knowledge.document.support;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum DocumentType {
    PDF("PDF", Set.of("pdf")),
    WORD("WORD", Set.of("doc", "docx")),
    PPT("PPT", Set.of("ppt", "pptx")),
    EXCEL("EXCEL", Set.of("xls", "xlsx")),
    MARKDOWN("MARKDOWN", Set.of("md", "markdown")),
    TXT("TXT", Set.of("txt"));

    private final String code;
    private final Set<String> extensions;

    DocumentType(String code, Set<String> extensions) {
        this.code = code;
        this.extensions = extensions;
    }

    public String code() {
        return code;
    }

    public static Optional<DocumentType> fromFilename(String filename) {
        String extension = extensionOf(filename).orElse("");
        return Arrays.stream(values())
                .filter(type -> type.extensions.contains(extension))
                .findFirst();
    }

    public static Optional<String> extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        String cleanFilename = cleanFilename(filename);
        int dotIndex = cleanFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanFilename.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(cleanFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT));
    }

    public static String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "untitled";
        }
        String normalized = filename.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        String cleanFilename = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return cleanFilename.isBlank() ? "untitled" : cleanFilename;
    }
}
