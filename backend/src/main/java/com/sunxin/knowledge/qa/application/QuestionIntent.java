package com.sunxin.knowledge.qa.application;

import com.sunxin.knowledge.retrieval.dto.SearchFilters;

public record QuestionIntent(
        String query,
        SearchFilters filters
) {
}
