package com.sunxin.knowledge.retrieval.rerank;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sunxin.knowledge.retrieval.dto.SearchFilters;

class RerankAndContextServiceTest {

    private final RerankService rerankService = new RerankService(new RuleBasedReranker());
    private final ContextBuilderService contextBuilderService = new ContextBuilderService();

    @Test
    void ruleBasedRerankBoostsKeywordMetadataAndNewnessThenLimitsPerDocument() {
        List<RerankCandidate> candidates = List.of(
                candidate(101L, 1L, "金融数据治理 proposal", "PROPOSAL", "金融", "数据治理",
                        LocalDateTime.of(2025, 1, 3, 10, 0), 0, 12, "项目范围",
                        "金融行业数据治理 proposal 类似案例，包含主数据、数据标准和治理组织设计。", 0.42),
                candidate(102L, 1L, "金融数据治理 proposal", "PROPOSAL", "金融", "数据治理",
                        LocalDateTime.of(2025, 1, 3, 10, 0), 1, 13, "项目范围",
                        "数据治理实施路径、里程碑和交付安排。", 0.39),
                candidate(103L, 1L, "金融数据治理 proposal", "PROPOSAL", "金融", "数据治理",
                        LocalDateTime.of(2025, 1, 3, 10, 0), 2, 14, "项目范围",
                        "项目团队和沟通机制。", 0.38),
                candidate(201L, 2L, "旧版金融数据治理 proposal", "PROPOSAL", "金融", "数据治理",
                        LocalDateTime.of(2021, 5, 1, 10, 0), 0, 3, "背景",
                        "金融行业数据治理 proposal 类似案例。", 0.41),
                candidate(301L, 3L, "金融制度文档", "POLICY", "金融", "数据治理",
                        LocalDateTime.of(2026, 1, 1, 10, 0), 0, 1, "制度",
                        "金融行业数据治理制度流程。", 0.7)
        );

        List<RerankedChunk> reranked = rerankService.rerank(new RerankRequest(
                "金融行业数据治理 proposal 有哪些类似案例？",
                new SearchFilters("proposal", "金融", "数据治理", 2022),
                candidates,
                10,
                2
        ));

        assertThat(reranked).extracting(RerankedChunk::chunkId)
                .contains(101L, 102L)
                .doesNotContain(103L);
        assertThat(reranked.stream().filter(chunk -> chunk.docId().equals(1L))).hasSize(2);
        assertThat(reranked.get(0).chunkId()).isEqualTo(101L);
        assertThat(reranked.get(0).rerankScore()).isGreaterThan(reranked.get(0).retrievalScore());
        assertThat(reranked.indexOf(findByChunkId(reranked, 101L)))
                .isLessThan(reranked.indexOf(findByChunkId(reranked, 201L)));
        assertThat(reranked.indexOf(findByChunkId(reranked, 201L)))
                .isLessThan(reranked.indexOf(findByChunkId(reranked, 301L)));
    }

    @Test
    void contextBuilderMergesAdjacentChunksAndKeepsCitationNumbers() {
        List<RerankedChunk> chunks = List.of(
                ranked(1, 101L, 1L, "金融数据治理 proposal", "PROPOSAL", "金融", "数据治理",
                        LocalDateTime.of(2025, 1, 3, 10, 0), 0, 12, "项目范围",
                        "第一段范围说明。", 0.5, 0.91, "local://proposal"),
                ranked(2, 102L, 1L, "金融数据治理 proposal", "PROPOSAL", "金融", "数据治理",
                        LocalDateTime.of(2025, 1, 3, 10, 0), 1, 13, "项目范围",
                        "第二段交付安排。", 0.48, 0.89, "local://proposal"),
                ranked(3, 201L, 2L, "数据治理 SOW", "SOW", "金融", "数据治理",
                        LocalDateTime.of(2024, 6, 1, 10, 0), 0, 5, "交付物",
                        "SOW 交付物清单。", 0.45, 0.82, "local://sow")
        );

        ContextBuildResult result = contextBuilderService.build(new ContextBuildRequest(chunks, 2_000));

        assertThat(result.context()).contains("""
                [引用1]
                文档：金融数据治理 proposal
                页码：12-13
                章节：项目范围
                内容：第一段范围说明。

                第二段交付安排。
                """);
        assertThat(result.context()).contains("""
                [引用2]
                文档：数据治理 SOW
                页码：5
                章节：交付物
                内容：SOW 交付物清单。
                """);
        assertThat(result.citations()).hasSize(2);
        assertThat(result.citations().get(0).citationNo()).isEqualTo(1);
        assertThat(result.citations().get(0).chunkIds()).containsExactly(101L, 102L);
        assertThat(result.citations().get(1).citationNo()).isEqualTo(2);
        assertThat(result.citations().get(1).chunkIds()).containsExactly(201L);
    }

    private static RerankedChunk findByChunkId(List<RerankedChunk> chunks, Long chunkId) {
        return chunks.stream()
                .filter(chunk -> chunk.chunkId().equals(chunkId))
                .findFirst()
                .orElseThrow();
    }

    private static RerankCandidate candidate(
            Long chunkId,
            Long docId,
            String docTitle,
            String docType,
            String industry,
            String serviceLine,
            LocalDateTime documentCreatedAt,
            Integer chunkIndex,
            Integer pageNo,
            String sectionTitle,
            String content,
            Double retrievalScore
    ) {
        return new RerankCandidate(
                chunkId,
                docId,
                docTitle,
                docType,
                industry,
                serviceLine,
                documentCreatedAt,
                chunkIndex,
                pageNo,
                sectionTitle,
                content,
                retrievalScore,
                "local://" + docId
        );
    }

    private static RerankedChunk ranked(
            Integer rank,
            Long chunkId,
            Long docId,
            String docTitle,
            String docType,
            String industry,
            String serviceLine,
            LocalDateTime documentCreatedAt,
            Integer chunkIndex,
            Integer pageNo,
            String sectionTitle,
            String content,
            Double retrievalScore,
            Double rerankScore,
            String sourceUri
    ) {
        return new RerankedChunk(
                rank,
                chunkId,
                docId,
                docTitle,
                docType,
                industry,
                serviceLine,
                documentCreatedAt,
                chunkIndex,
                pageNo,
                sectionTitle,
                content,
                retrievalScore,
                rerankScore,
                sourceUri
        );
    }
}
