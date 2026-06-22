package com.sunxin.knowledge.document.cleaning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Component
public class HeaderFooterCleaner implements DocumentCleaner {

    @Override
    public List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report) {
        if (pages.size() < 3) {
            return pages;
        }

        Map<String, Integer> lineFrequencies = new HashMap<>();
        
        // Count frequencies of first and last lines
        for (ParsedPageRequest page : pages) {
            if (page.content() == null) continue;
            String[] lines = page.content().split("\n");
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                if (!firstLine.isBlank() && firstLine.length() < 100) {
                    lineFrequencies.put(firstLine, lineFrequencies.getOrDefault(firstLine, 0) + 1);
                }
                String lastLine = lines[lines.length - 1].trim();
                if (!lastLine.isBlank() && lastLine.length() < 100) {
                    lineFrequencies.put(lastLine, lineFrequencies.getOrDefault(lastLine, 0) + 1);
                }
            }
        }

        int threshold = Math.max(3, (int)(pages.size() * 0.5));
        
        List<ParsedPageRequest> result = new ArrayList<>();
        for (ParsedPageRequest page : pages) {
            if (page.content() == null) {
                result.add(page);
                continue;
            }
            String[] lines = page.content().split("\n");
            List<String> keptLines = new ArrayList<>();
            boolean removedAny = false;
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                // Only check first 2 and last 2 lines
                if ((i < 2 || i >= lines.length - 2) && lineFrequencies.getOrDefault(line, 0) >= threshold) {
                    removedAny = true;
                    report.addHeaderFooterRemovedCount(1);
                    report.addCleanedCharCount(line.length());
                } else {
                    keptLines.add(lines[i]);
                }
            }
            
            if (removedAny) {
                Map<String, Object> newMeta = new LinkedHashMap<>(page.metadata() == null ? Map.of() : page.metadata());
                newMeta.put("header_footer_cleaned", true);
                result.add(new ParsedPageRequest(page.pageNo(), page.sectionTitle(), page.contentType(), String.join("\n", keptLines), newMeta));
            } else {
                result.add(page);
            }
        }

        return result;
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
