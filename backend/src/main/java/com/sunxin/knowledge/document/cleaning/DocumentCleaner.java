package com.sunxin.knowledge.document.cleaning;

import java.util.List;
import com.sunxin.knowledge.document.dto.ParsedPageRequest;

public interface DocumentCleaner {
    /**
     * Clean a list of parsed pages. Modifies the pages in place or returns a new list.
     * Implementations should update the global report and page-level metadata.
     */
    List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report);

    /**
     * Lower value means higher priority (executes earlier).
     */
    int getOrder();
}
