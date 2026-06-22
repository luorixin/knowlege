package com.sunxin.knowledge.qa.application;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.qa.llm.LlmResponse;
import com.sunxin.knowledge.retrieval.rerank.ContextCitation;

@Service
public class AnswerSafetyGuard {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[引用(\\d+)]");

    public LlmResponse guard(LlmResponse response, List<ContextCitation> citations) {
        if (response == null || response.answer() == null) {
            return response;
        }
        Set<Integer> allowedCitationNumbers = citations == null
                ? Set.of()
                : citations.stream()
                .map(ContextCitation::citationNo)
                .collect(Collectors.toUnmodifiableSet());

        String guardedAnswer = CITATION_PATTERN.matcher(response.answer())
                .replaceAll(match -> {
                    Integer citationNo = Integer.valueOf(match.group(1));
                    return allowedCitationNumbers.contains(citationNo) ? match.group() : "";
                })
                .replaceAll("(?i)</\\s*context\\s*>", "<\\\\/context>")
                .strip();

        return new LlmResponse(
                guardedAnswer,
                response.provider(),
                response.modelName(),
                response.promptTokens(),
                response.completionTokens(),
                response.latencyMs()
        );
    }
}
