package com.sunxin.knowledge.document.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.chunking")
public class ChunkingProperties {

    private int defaultSize = 800;

    private int defaultOverlap = 100;

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
}
