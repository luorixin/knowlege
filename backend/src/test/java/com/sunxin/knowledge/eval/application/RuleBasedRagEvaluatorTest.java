package com.sunxin.knowledge.eval.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sunxin.knowledge.eval.dto.EvalCaseReportResponse;
import com.sunxin.knowledge.eval.dto.EvalCaseSpec;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.qa.dto.AgentCitationResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult;

class RuleBasedRagEvaluatorTest {

    private final RuleBasedRagEvaluator evaluator = new RuleBasedRagEvaluator();

    @Test
    void marksPermissionViolationWhenAnswerCitesUnauthorizedDocument() {
        EvalCaseSpec spec = new EvalCaseSpec(
                List.of(10L),
                List.of(100L),
                false,
                null,
                List.of()
        );
        RetrievalSearchResponse retrieval = new RetrievalSearchResponse(List.of(new RetrievalSearchResult(
                100L,
                10L,
                "金融数据治理 Proposal",
                12,
                "解决方案",
                "金融行业数据治理 proposal 常见结构。",
                0.91,
                "local://proposal.pdf"
        )));
        AgentChatResponse answer = new AgentChatResponse(
                1L,
                "根据资料可回答。[引用1]",
                List.of(new AgentCitationResponse(1, 999L, "无权限文档", 3, "保密章节", "Some content", "local://secret.pdf")),
                null
        );

        EvalCaseReportResponse report = evaluator.evaluate(
                1L,
                "金融行业数据治理 proposal",
                spec,
                retrieval,
                answer,
                0,
                1,
                0
        );

        assertThat(report.recallHit()).isTrue();
        assertThat(report.citationAccuracy()).isZero();
        assertThat(report.unauthorizedCitationCount()).isEqualTo(1);
        assertThat(report.permissionViolation()).isTrue();
        assertThat(report.reciprocalRank()).isEqualTo(1.0);
    }
}
