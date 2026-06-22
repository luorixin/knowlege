package com.sunxin.knowledge.document.cleaning;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderFooterCleanerTest {

    @Test
    void shouldRemoveRepeatedHeaders() {
        HeaderFooterCleaner cleaner = new HeaderFooterCleaner();
        
        ParsedPageRequest page1 = new ParsedPageRequest(1, "Title", "text", "Company Header\nContent 1\nPage 1", Map.of());
        ParsedPageRequest page2 = new ParsedPageRequest(2, "Title", "text", "Company Header\nContent 2\nPage 2", Map.of());
        ParsedPageRequest page3 = new ParsedPageRequest(3, "Title", "text", "Company Header\nContent 3\nPage 3", Map.of());

        DocumentCleaningReport report = new DocumentCleaningReport();
        List<ParsedPageRequest> result = cleaner.clean(List.of(page1, page2, page3), null, report);

        assertEquals(3, result.size());
        assertEquals("Content 1\nPage 1", result.get(0).content());
        assertEquals("Content 2\nPage 2", result.get(1).content());
        assertEquals("Content 3\nPage 3", result.get(2).content());
        assertEquals(3, report.getHeaderFooterRemovedCount());
    }
}
