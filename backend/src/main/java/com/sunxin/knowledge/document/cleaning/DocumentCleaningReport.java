package com.sunxin.knowledge.document.cleaning;

import java.util.LinkedHashMap;
import java.util.Map;

public class DocumentCleaningReport {
    private String cleaningRuleVersion = "v2.0.0";
    private int cleanedCharCount;
    private int removedNoiseCount;
    private int duplicateRemovedCount;
    private int ocrNoiseRemovedCount;
    private int headerFooterRemovedCount;

    public void addCleanedCharCount(int count) { this.cleanedCharCount += count; }
    public void addRemovedNoiseCount(int count) { this.removedNoiseCount += count; }
    public void addDuplicateRemovedCount(int count) { this.duplicateRemovedCount += count; }
    public void addOcrNoiseRemovedCount(int count) { this.ocrNoiseRemovedCount += count; }
    public void addHeaderFooterRemovedCount(int count) { this.headerFooterRemovedCount += count; }

    public String getCleaningRuleVersion() { return cleaningRuleVersion; }
    public int getCleanedCharCount() { return cleanedCharCount; }
    public int getRemovedNoiseCount() { return removedNoiseCount; }
    public int getDuplicateRemovedCount() { return duplicateRemovedCount; }
    public int getOcrNoiseRemovedCount() { return ocrNoiseRemovedCount; }
    public int getHeaderFooterRemovedCount() { return headerFooterRemovedCount; }

    public void merge(DocumentCleaningReport other) {
        if (other != null) {
            this.cleanedCharCount += other.cleanedCharCount;
            this.removedNoiseCount += other.removedNoiseCount;
            this.duplicateRemovedCount += other.duplicateRemovedCount;
            this.ocrNoiseRemovedCount += other.ocrNoiseRemovedCount;
            this.headerFooterRemovedCount += other.headerFooterRemovedCount;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cleaning_rule_version", cleaningRuleVersion);
        if (cleanedCharCount > 0) map.put("cleaned_char_count", cleanedCharCount);
        if (removedNoiseCount > 0) map.put("removed_noise_count", removedNoiseCount);
        if (duplicateRemovedCount > 0) map.put("duplicate_removed_count", duplicateRemovedCount);
        if (ocrNoiseRemovedCount > 0) map.put("ocr_noise_removed_count", ocrNoiseRemovedCount);
        if (headerFooterRemovedCount > 0) map.put("header_footer_removed_count", headerFooterRemovedCount);
        return map;
    }
}
