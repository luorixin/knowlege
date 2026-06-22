package com.sunxin.knowledge.document.cleaning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Component
public class DuplicateBlockCleaner implements DocumentCleaner {

    @Override
    public List<ParsedPageRequest> clean(List<ParsedPageRequest> pages, DocumentCleaningContext context, DocumentCleaningReport report) {
        List<ParsedPageRequest> result = new ArrayList<>();
        List<Set<String>> previousNgramsList = new ArrayList<>();

        for (ParsedPageRequest page : pages) {
            if (page.content() == null || page.content().length() < 50) {
                result.add(page);
                continue;
            }

            Set<String> currentNgrams = extractTrigrams(page.content());
            boolean isDuplicate = false;

            for (Set<String> prevNgrams : previousNgramsList) {
                double jaccard = calculateJaccard(currentNgrams, prevNgrams);
                if (jaccard > 0.85) { // 85% similarity threshold
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                report.addDuplicateRemovedCount(1);
                report.addCleanedCharCount(page.content().length());
            } else {
                previousNgramsList.add(currentNgrams);
                result.add(page);
            }
        }

        return result;
    }

    private Set<String> extractTrigrams(String text) {
        Set<String> ngrams = new HashSet<>();
        String normalized = text.replaceAll("\\s+", "").toLowerCase();
        if (normalized.length() < 3) return ngrams;
        for (int i = 0; i < normalized.length() - 2; i++) {
            ngrams.add(normalized.substring(i, i + 3));
        }
        return ngrams;
    }

    private double calculateJaccard(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        
        return (double) intersection.size() / union.size();
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
