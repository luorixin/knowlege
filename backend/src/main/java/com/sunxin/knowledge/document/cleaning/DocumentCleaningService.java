package com.sunxin.knowledge.document.cleaning;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Service
public class DocumentCleaningService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCleaningService.class);

    private final List<DocumentCleaner> cleaners;

    public DocumentCleaningService(List<DocumentCleaner> cleaners) {
        this.cleaners = cleaners.stream()
                .sorted(Comparator.comparingInt(DocumentCleaner::getOrder))
                .toList();
    }

    public CleanedDocumentResult clean(List<ParsedPageRequest> pages, DocumentCleaningContext context) {
        if (pages == null || pages.isEmpty()) {
            return new CleanedDocumentResult(pages, new DocumentCleaningReport());
        }

        DocumentCleaningReport globalReport = new DocumentCleaningReport();
        List<ParsedPageRequest> currentPages = new java.util.ArrayList<>(pages);

        for (DocumentCleaner cleaner : cleaners) {
            try {
                currentPages = new java.util.ArrayList<>(cleaner.clean(currentPages, context, globalReport));
            } catch (Exception ex) {
                log.warn("Document cleaner {} failed for docId={}, continuing with next cleaner", 
                        cleaner.getClass().getSimpleName(), context.docId(), ex);
            }
        }

        return new CleanedDocumentResult(currentPages, globalReport);
    }

    
    public record CleanedDocumentResult(List<ParsedPageRequest> pages, DocumentCleaningReport report) {}
}
