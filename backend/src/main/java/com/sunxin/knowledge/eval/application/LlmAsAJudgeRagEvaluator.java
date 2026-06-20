package com.sunxin.knowledge.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunxin.knowledge.qa.llm.LlmProvider;
import com.sunxin.knowledge.qa.llm.LlmRequest;
import com.sunxin.knowledge.qa.llm.LlmResponse;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LlmAsAJudgeRagEvaluator {

    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    public LlmAsAJudgeRagEvaluator(LlmProvider llmProvider, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    public JudgeResult evaluate(String question, RetrievalSearchResponse retrieval, AgentChatResponse answer) {
        List<RetrievalSearchResult> results = retrieval == null || retrieval.results() == null ? List.of() : retrieval.results();
        String context = results.stream().map(RetrievalSearchResult::content).collect(Collectors.joining("\n\n"));
        String actualAnswer = answer == null || answer.answer() == null ? "" : answer.answer();

        if (context.isBlank() && actualAnswer.isBlank()) {
            return new JudgeResult(0, 0, "No context and no answer");
        }

        String prompt = "你是一个严厉的 RAG（检索增强生成）系统评测专家。请分别评估：\n" +
                "1. Context Relevance (0-100): 给定的检索上下文(Context)是否包含足以回答用户提问(Question)的信息？\n" +
                "2. Answer Faithfulness (0-100): 最终回答(Answer)是否严格遵循了上下文，没有任何过度延伸或幻觉编造？如果回答明确表示“找不到资料”，且上下文确实没有资料，则视为100分。\n\n" +
                "请直接输出 JSON，不要包含 Markdown 标记或其他多余字符，格式如下：\n" +
                "{\"context_relevance_score\": 90, \"answer_faithfulness_score\": 100, \"reason\": \"打分理由...\"}\n\n" +
                "【Question】\n" + question + "\n\n" +
                "【Context】\n" + (context.isBlank() ? "无上下文" : context) + "\n\n" +
                "【Answer】\n" + (actualAnswer.isBlank() ? "无回答" : actualAnswer);

        try {
            LlmResponse response = llmProvider.generate(new LlmRequest(prompt, "", List.of(), List.of()));
            String rawJson = response.answer().trim();
            // Handle markdown block if model added it
            if (rawJson.startsWith("```json")) {
                rawJson = rawJson.substring(7);
            } else if (rawJson.startsWith("```")) {
                rawJson = rawJson.substring(3);
            }
            if (rawJson.endsWith("```")) {
                rawJson = rawJson.substring(0, rawJson.length() - 3);
            }

            JsonNode node = objectMapper.readTree(rawJson);
            return new JudgeResult(
                    node.path("context_relevance_score").asDouble(0),
                    node.path("answer_faithfulness_score").asDouble(0),
                    node.path("reason").asText("No reason provided")
            );
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(LlmAsAJudgeRagEvaluator.class).error("Failed to parse LLM judge response", e);
            return new JudgeResult(0, 0, "Evaluation failed: " + e.getMessage());
        }
    }

    public record JudgeResult(double contextRelevanceScore, double answerFaithfulnessScore, String reason) {}
}
