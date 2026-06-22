package com.sunxin.knowledge.document.cleaning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Component
public class OcrQualityCleaner implements DocumentCleaner {

    @Value("${knowledge.cleaning.ocr.confidence-threshold:0.65}")
    private double confidenceThreshold;

    @Value("${knowledge.cleaning.ocr.garbage-ratio-threshold:0.4}")
    private double garbageRatioThreshold;

    private static final Pattern GARBAGE_PATTERN = Pattern.compile("[^\\x00-\\x7F\\u4e00-\\u9fa5\\p{Punct}\\s]");
    private static final Pattern WATERMARK_PATTERN = Pattern.compile("(?i)(confidential|internal use only|do not distribute|机密|内部使用)");

    @Override
    public List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report) {
        List<ParsedPageRequest> result = new ArrayList<>();
        
        for (ParsedPageRequest page : pages) {
            String content = page.content();
            if (content == null || content.isBlank()) {
                result.add(page);
                continue;
            }

            Map<String, Object> meta = page.metadata() == null ? Map.of() : page.metadata();
            
            // Check confidence
            if (meta.containsKey("confidence")) {
                try {
                    double conf = Double.parseDouble(meta.get("confidence").toString());
                    if (conf < confidenceThreshold) {
                        report.addOcrNoiseRemovedCount(1);
                        continue; // Drop the page
                    }
                } catch (Exception ignored) {}
            }

            // Check garbage ratio
            long garbageChars = GARBAGE_PATTERN.matcher(content).results().count();
            if (content.length() > 20 && (double) garbageChars / content.length() > garbageRatioThreshold) {
                report.addOcrNoiseRemovedCount(1);
                continue; // Drop the page
            }

            // Watermark removal
            String newContent = WATERMARK_PATTERN.matcher(content).replaceAll("").trim();
            if (newContent.length() < content.length()) {
                report.addCleanedCharCount(content.length() - newContent.length());
                Map<String, Object> newMeta = new LinkedHashMap<>(meta);
                newMeta.put("watermark_removed", true);
                result.add(new ParsedPageRequest(page.pageNo(), page.sectionTitle(), page.contentType(), newContent, newMeta));
            } else {
                result.add(page);
            }
        }
        return result;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
