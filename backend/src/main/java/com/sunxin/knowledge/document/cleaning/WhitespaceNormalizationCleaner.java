package com.sunxin.knowledge.document.cleaning;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Component
public class WhitespaceNormalizationCleaner implements DocumentCleaner {

    @Override
    public List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report) {
        return pages.stream().map(page -> {
            String original = page.content();
            if (original == null || original.isBlank()) {
                return page;
            }

            // Replace \r\n with \n
            String cleaned = original.replace("\r\n", "\n");
            // Replace multiple spaces with a single space
            cleaned = cleaned.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
            // Trim leading/trailing spaces on each line
            cleaned = cleaned.replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
            // Remove multiple blank lines (keep at most one)
            cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

            java.util.Map<String, Object> safeMeta = new java.util.LinkedHashMap<>(page.metadata() == null ? java.util.Map.of() : page.metadata());
            int diff = original.length() - cleaned.length();
            if (diff > 0) {
                report.addCleanedCharCount(diff);
                safeMeta.put("whitespace_cleaned", diff);
            }

            return new ParsedPageRequest(
                    page.pageNo(),
                    page.sectionTitle(),
                    page.contentType(),
                    cleaned.trim(),
                    safeMeta
            );
        }).toList();
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
