package com.sunxin.knowledge.document.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.persistence.entity.KbDocument;

class DocumentChunkerTest {

    @Test
    void longTextSplitsOnSentenceBoundaryBeforeFallingBackToCharacterCuts() {
        DocumentChunker chunker = new DocumentChunker(testProperties(34, 48, 12, 8));
        String content = """
                # 项目背景
                第一段说明客户当前的数据治理背景。
                第二段描述当前系统割裂和指标口径不一致。
                第三段说明建设目标和阶段性成果。
                第四段补充实施路径和组织保障。
                """;

        List<ChunkDraft> chunks = chunker.chunk(document("proposal"), request(page(1, "项目背景", "text", content, Map.of())));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks)
                .allSatisfy(chunk -> assertThat(chunk.content()).hasSizeLessThanOrEqualTo(48));
        assertThat(chunks)
                .filteredOn(chunk -> !chunk.content().startsWith("#"))
                .allSatisfy(chunk -> assertThat(chunk.content()).doesNotStartWith("。"));
        assertThat(chunks.get(0).content()).endsWith("。");
        assertThat(chunks.get(0).metadata())
                .containsEntry("split_strategy", "semantic-boundary")
                .containsEntry("truncated_by_fallback", false);
    }

    @Test
    void headingsProduceSectionPathAndHeadingMetadata() {
        DocumentChunker chunker = new DocumentChunker(testProperties(120, 160, 12, 0));
        String content = """
                # 项目背景
                背景说明。
                ## 业务痛点
                痛点说明。
                """;

        List<ChunkDraft> chunks = chunker.chunk(document("proposal"), request(page(2, "Proposal", "text", content, Map.of("block_id", "b-2"))));

        assertThat(chunks).hasSize(2);
        ChunkDraft painPoint = chunks.get(1);
        assertThat(painPoint.sectionTitle()).isEqualTo("业务痛点");
        assertThat(painPoint.metadata())
                .containsEntry("section_path", List.of("项目背景", "业务痛点"))
                .containsEntry("heading_level", 2)
                .containsEntry("parent_section", "项目背景")
                .containsEntry("chunking_strategy_version", "semantic-test");
        assertThat(painPoint.metadata().get("source_block_ids")).isEqualTo(List.of("b-2"));
    }

    @Test
    void tableChunksUseRowWindowsRepeatHeaderAndSkipMechanicalOverlap() {
        DocumentChunker chunker = new DocumentChunker(testProperties(72, 96, 12, 20));
        String content = """
                | 指标 | 金额 |
                | --- | --- |
                | 第一阶段 | 100万 |
                | 第二阶段 | 120万 |
                | 第三阶段 | 150万 |
                | 第四阶段 | 180万 |
                | 第五阶段 | 220万 |
                """;

        List<ChunkDraft> chunks = chunker.chunk(document("excel"), request(page(1, "预算表", "table", content, Map.of("sheet_name", "预算", "range", "A1:B7"))));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.content()).startsWith("| 指标 | 金额 |");
            assertThat(chunk.metadata())
                    .containsEntry("split_strategy", "table-row-window")
                    .containsEntry("overlap", 0)
                    .containsEntry("table_header_repeated", true);
        });
    }

    @Test
    void pptKeepsOneSlideAsOneChunkWhenInsideBudget() {
        DocumentChunker chunker = new DocumentChunker(testProperties(80, 120, 12, 20));
        List<ChunkDraft> chunks = chunker.chunk(document("ppt"), request(page(3, "解决方案", "text", "本页说明整体方案和关键能力。", Map.of("slide_no", 3))));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).pageNo()).isEqualTo(3);
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("解决方案");
        assertThat(chunks.get(0).metadata())
                .containsEntry("split_strategy", "ppt-page")
                .containsEntry("overlap", 0);
    }

    @Test
    void figureCaptionKeepsImageMetadataAndDoesNotUseOverlap() {
        DocumentChunker chunker = new DocumentChunker(testProperties(80, 120, 12, 20));
        Map<String, Object> metadata = Map.of(
                "image_uri", "minio://bucket/slide-1.png",
                "caption_provider", "mock-vlm",
                "confidence", 0.82
        );

        List<ChunkDraft> chunks = chunker.chunk(document("pdf"), request(page(4, "架构图", "figure", "图中展示数据采集、治理、服务和运营闭环。", metadata)));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).contentType()).isEqualTo("figure");
        assertThat(chunks.get(0).metadata())
                .containsEntry("split_strategy", "figure-caption")
                .containsEntry("image_uri", "minio://bucket/slide-1.png")
                .containsEntry("caption_provider", "mock-vlm")
                .containsEntry("confidence", 0.82)
                .containsEntry("overlap", 0);
    }

    private static ChunkingProperties testProperties(int targetSize, int maxSize, int minSize, int overlap) {
        ChunkingProperties properties = new ChunkingProperties();
        properties.setTargetSize(targetSize);
        properties.setMaxSize(maxSize);
        properties.setMinSize(minSize);
        properties.setOverlap(overlap);
        properties.setStrategyVersion("semantic-test");
        return properties;
    }

    private static KbDocument document(String docType) {
        KbDocument document = new KbDocument();
        document.setDocType(docType);
        return document;
    }

    private static RebuildChunksRequest request(ParsedPageRequest page) {
        return new RebuildChunksRequest(null, null, List.of(page));
    }

    private static ParsedPageRequest page(
            Integer pageNo,
            String sectionTitle,
            String contentType,
            String content,
            Map<String, Object> metadata
    ) {
        return new ParsedPageRequest(pageNo, sectionTitle, contentType, content, metadata);
    }
}
