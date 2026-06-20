package com.sunxin.knowledge.document.cleaning;

public class DocumentCleaningReport {
    private int cleanedCharCount;
    private int removedNoiseCount;
    private int duplicateRemovedCount;

    public void addCleanedCharCount(int count) {
        this.cleanedCharCount += count;
    }

    public void addRemovedNoiseCount(int count) {
        this.removedNoiseCount += count;
    }

    public void addDuplicateRemovedCount(int count) {
        this.duplicateRemovedCount += count;
    }

    public int getCleanedCharCount() {
        return cleanedCharCount;
    }

    public int getRemovedNoiseCount() {
        return removedNoiseCount;
    }

    public int getDuplicateRemovedCount() {
        return duplicateRemovedCount;
    }

    public void merge(DocumentCleaningReport other) {
        if (other != null) {
            this.cleanedCharCount += other.cleanedCharCount;
            this.removedNoiseCount += other.removedNoiseCount;
            this.duplicateRemovedCount += other.duplicateRemovedCount;
        }
    }
}
