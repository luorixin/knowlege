package com.sunxin.knowledge.qa.llm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.retrieval.rerank.ContextCitation;

@Service
public class MockLlmProvider implements LlmProvider {

    private static final String NO_EVIDENCE = "未在当前知识库中找到可靠依据，无法基于现有资料回答该问题。";
    private static final Pattern CITATION_BLOCK = Pattern.compile(
            "\\[引用(\\d+)]\\s*文档：.*?\\s*页码：.*?\\s*章节：.*?\\s*内容：([\\s\\S]*?)(?=\\n\\n\\[引用\\d+]|\\z)"
    );

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public String modelName() {
        return "mock-llm-provider";
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        long startedAt = System.currentTimeMillis();
        List<ContextCitation> citations = request.citations() == null ? List.of() : request.citations();
        String context = request.context() == null ? "" : request.context();
        String answer = citations.isEmpty() || context.isBlank()
                ? NO_EVIDENCE
                : answerFromContext(context);
        long latencyMs = System.currentTimeMillis() - startedAt;
        return new LlmResponse(
                answer,
                provider(),
                modelName(),
                estimateTokens(request.query()) + estimateTokens(context),
                estimateTokens(answer),
                latencyMs
        );
    }

    private static String answerFromContext(String context) {
        StringBuilder answer = new StringBuilder("根据当前知识库检索到的资料，可归纳如下：\n");
        Matcher matcher = CITATION_BLOCK.matcher(context);
        int item = 1;
        while (matcher.find()) {
            String citationNo = matcher.group(1);
            String content = compact(matcher.group(2));
            if (!content.isBlank()) {
                answer.append(item++)
                        .append(". ")
                        .append(content)
                        .append(" [引用")
                        .append(citationNo)
                        .append("]\n");
            }
        }
        if (item == 1) {
            return NO_EVIDENCE;
        }
        return answer.toString().stripTrailing();
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        String compacted = value.replaceAll("\\s+", " ").trim();
        if (compacted.length() <= 180) {
            return compacted;
        }
        return compacted.substring(0, 177) + "...";
    }

    private static int estimateTokens(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Math.max(1, value.length() / 2);
    }
}
