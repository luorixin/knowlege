package com.sunxin.knowledge.document.cleaning;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Component
public class HeaderFooterCleaner implements DocumentCleaner {

    @Override
    public List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report) {
        if (pages.size() <= 1) {
            return pages;
        }

        // Group lines by page, find common first line if it appears in > 80% of pages
        // MVP: Just strip matching exact first line across consecutive pages
        String commonHeader = null;
        for (int i = 0; i < pages.size() - 1; i++) {
            String curr = pages.get(i).content();
            String next = pages.get(i + 1).content();
            if (curr != null && next != null) {
                String[] currLines = curr.split("\n");
                String[] nextLines = next.split("\n");
                if (currLines.length > 0 && nextLines.length > 0 && currLines[0].trim().equals(nextLines[0].trim()) && !currLines[0].isBlank()) {
                    commonHeader = currLines[0].trim();
                    break;
                }
            }
        }

        final String finalHeader = commonHeader;
        return pages.stream().map(page -> {
            String content = page.content();
            if (content != null && finalHeader != null) {
                String[] lines = content.split("\n");
                if (lines.length > 0 && lines[0].trim().equals(finalHeader)) {
                    int diff = lines[0].length() + 1; // including \n
                    report.addRemovedNoiseCount(1);
                    report.addCleanedCharCount(diff);
                    content = content.substring(lines[0].length()).trim();
                    if (content.startsWith("\n")) {
                        content = content.substring(1).trim();
                    }
                    return new ParsedPageRequest(
                            page.pageNo(),
                            page.sectionTitle(),
                            page.contentType(),
                            content,
                            page.metadata()
                    );
                }
            }
            return page;
        }).toList();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
