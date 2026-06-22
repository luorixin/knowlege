package com.sunxin.knowledge.document.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.chunking")
public class ChunkingProperties {

    private int defaultSize = 800;

    private int defaultOverlap = 100;

    private int targetSize = 700;

    private int maxSize = 1000;

    private int minSize = 120;

    private int overlap = 80;

    private boolean sentenceBoundaryEnabled = true;

    private boolean tableHeaderRepeatEnabled = true;

    private String strategyVersion = "semantic-v1";

    public int getDefaultSize() {
        return defaultSize;
    }

    public void setDefaultSize(int defaultSize) {
        this.defaultSize = defaultSize;
    }

    public int getDefaultOverlap() {
        return defaultOverlap;
    }

    public void setDefaultOverlap(int defaultOverlap) {
        this.defaultOverlap = defaultOverlap;
    }

    public int getTargetSize() {
        return targetSize;
    }

    public void setTargetSize(int targetSize) {
        this.targetSize = targetSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }

    public boolean isSentenceBoundaryEnabled() {
        return sentenceBoundaryEnabled;
    }

    public void setSentenceBoundaryEnabled(boolean sentenceBoundaryEnabled) {
        this.sentenceBoundaryEnabled = sentenceBoundaryEnabled;
    }

    public boolean isTableHeaderRepeatEnabled() {
        return tableHeaderRepeatEnabled;
    }

    public void setTableHeaderRepeatEnabled(boolean tableHeaderRepeatEnabled) {
        this.tableHeaderRepeatEnabled = tableHeaderRepeatEnabled;
    }

    public String getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(String strategyVersion) {
        this.strategyVersion = strategyVersion;
    }
}
