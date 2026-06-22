package com.sunxin.knowledge.document.cleaning;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateBlockCleanerTest {

    @Test
    void shouldRemoveDuplicateBlocks() {
        DuplicateBlockCleaner cleaner = new DuplicateBlockCleaner();
        
        String longText = "This is a very long text block that repeats. It needs to be at least fifty characters long so that it gets processed by the duplicate block cleaner.";
        ParsedPageRequest page1 = new ParsedPageRequest(1, "Title", "text", longText, Map.of());
        ParsedPageRequest page2 = new ParsedPageRequest(2, "Title", "text", longText + " slight difference.", Map.of());

        DocumentCleaningReport report = new DocumentCleaningReport();
        List<ParsedPageRequest> result = cleaner.clean(List.of(page1, page2), null, report);

        assertEquals(1, result.size()); // page2 should be removed because of high jaccard similarity
        assertEquals(longText, result.get(0).content());
        assertEquals(1, report.getDuplicateRemovedCount());
    }
}
