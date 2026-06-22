package com.sunxin.knowledge.document.cleaning;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OcrQualityCleanerTest {

    @Test
    void shouldFilterLowConfidence() {
        OcrQualityCleaner cleaner = new OcrQualityCleaner();
        ReflectionTestUtils.setField(cleaner, "confidenceThreshold", 0.65);
        ReflectionTestUtils.setField(cleaner, "garbageRatioThreshold", 0.4);

        ParsedPageRequest page1 = new ParsedPageRequest(1, "Title", "text", "Good content", Map.of("confidence", 0.9));
        ParsedPageRequest page2 = new ParsedPageRequest(2, "Title", "text", "Bad content", Map.of("confidence", 0.5));

        DocumentCleaningReport report = new DocumentCleaningReport();
        List<ParsedPageRequest> result = cleaner.clean(List.of(page1, page2), null, report);

        assertEquals(1, result.size());
        assertEquals("Good content", result.get(0).content());
        assertEquals(1, report.getOcrNoiseRemovedCount());
    }

    @Test
    void shouldFilterGarbageText() {
        OcrQualityCleaner cleaner = new OcrQualityCleaner();
        ReflectionTestUtils.setField(cleaner, "confidenceThreshold", 0.65);
        ReflectionTestUtils.setField(cleaner, "garbageRatioThreshold", 0.4);

        ParsedPageRequest page1 = new ParsedPageRequest(1, "Title", "text", "This is a normal English sentence with some logic.", Map.of());
        ParsedPageRequest page2 = new ParsedPageRequest(2, "Title", "text", "🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣🔣asdasd", Map.of());

        DocumentCleaningReport report = new DocumentCleaningReport();
        List<ParsedPageRequest> result = cleaner.clean(List.of(page1, page2), null, report);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).pageNo());
        assertEquals(1, report.getOcrNoiseRemovedCount());
    }

    @Test
    void shouldRemoveWatermark() {
        OcrQualityCleaner cleaner = new OcrQualityCleaner();
        ReflectionTestUtils.setField(cleaner, "confidenceThreshold", 0.65);
        ReflectionTestUtils.setField(cleaner, "garbageRatioThreshold", 0.4);

        ParsedPageRequest page1 = new ParsedPageRequest(1, "Title", "text", "Internal Use Only\nActual content", Map.of());

        DocumentCleaningReport report = new DocumentCleaningReport();
        List<ParsedPageRequest> result = cleaner.clean(List.of(page1), null, report);

        assertEquals(1, result.size());
        assertEquals("Actual content", result.get(0).content());
        assertTrue((Boolean) result.get(0).metadata().get("watermark_removed"));
    }
}
