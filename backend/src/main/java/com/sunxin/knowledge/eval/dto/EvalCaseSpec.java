package com.sunxin.knowledge.eval.dto;

import java.util.List;

import com.sunxin.knowledge.retrieval.dto.SearchFilters;

public record EvalCaseSpec(
        List<Long> expectedDocIds,
        List<Long> expectedChunkIds,
        Boolean expectNoAnswer,
        SearchFilters filters,
        List<String> tags,
        Integer expectedBlockCount,
        Integer expectedTableCount,
        Integer expectedErrorCount
) {
}
