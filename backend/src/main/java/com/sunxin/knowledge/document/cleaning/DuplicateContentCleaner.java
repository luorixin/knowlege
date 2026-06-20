package com.sunxin.knowledge.document.cleaning;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Component
public class DuplicateContentCleaner implements DocumentCleaner {

    @Override
    public List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report) {
        Set<String> seenBlocks = new HashSet<>();

        return pages.stream().map(page -> {
            String content = page.content();
            if (content == null || content.isBlank()) {
                return page;
            }

            // Simple exact match duplicate removal
            if (seenBlocks.contains(content)) {
                report.addDuplicateRemovedCount(1);
                report.addCleanedCharCount(content.length());
                // Return an empty content page, it will be skipped by the chunker
                return new ParsedPageRequest(
                        page.pageNo(),
                        page.sectionTitle(),
                        page.contentType(),
                        "",
                        page.metadata()
                );
            }

            seenBlocks.add(content);
            return page;
        }).toList();
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
