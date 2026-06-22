package com.sunxin.knowledge.retrieval.rerank;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class ContextBuilderService {

    private static final int DEFAULT_MAX_CONTEXT_CHARS = 8_000;

    public ContextBuildResult build(ContextBuildRequest request) {
        List<RerankedChunk> chunks = request == null || request.chunks() == null
                ? List.of()
                : request.chunks();
        int maxContextChars = positiveOrDefault(
                request == null ? null : request.maxContextChars(),
                DEFAULT_MAX_CONTEXT_CHARS
        );

        StringBuilder context = new StringBuilder();
        List<ContextCitation> citations = new ArrayList<>();
        for (MergedCitation mergedCitation : mergeAdjacent(chunks)) {
            int citationNo = citations.size() + 1;
            String block = formatBlock(citationNo, mergedCitation);
            if (context.length() + block.length() > maxContextChars) {
                if (citations.isEmpty()) {
                    block = truncateBlock(block, maxContextChars);
                } else {
                    break;
                }
            }
            context.append(block);
            if (!block.endsWith("\n\n")) {
                context.append("\n\n");
            }
            citations.add(mergedCitation.toCitation(citationNo));
        }
        return new ContextBuildResult(context.toString(), citations);
    }

    private static List<MergedCitation> mergeAdjacent(List<RerankedChunk> chunks) {
        List<MergedCitation> citations = new ArrayList<>();
        for (RerankedChunk chunk : chunks) {
            if (!citations.isEmpty() && citations.get(citations.size() - 1).canMerge(chunk)) {
                citations.get(citations.size() - 1).merge(chunk);
            } else {
                citations.add(new MergedCitation(chunk));
            }
        }
        return citations;
    }

    private static String formatBlock(int citationNo, MergedCitation citation) {
        return """
                [引用%d]
                文档：%s
                页码：%s
                章节：%s
                内容：%s

                """.formatted(
                citationNo,
                sanitizeUntrustedText(defaultString(citation.docTitle(), "未知文档")),
                pageRange(citation.startPageNo(), citation.endPageNo()),
                sanitizeUntrustedText(defaultString(citation.sectionTitle(), "未命名章节")),
                sanitizeUntrustedText(citation.content())
        );
    }

    private static String pageRange(Integer startPageNo, Integer endPageNo) {
        if (startPageNo == null && endPageNo == null) {
            return "未知";
        }
        if (Objects.equals(startPageNo, endPageNo) || endPageNo == null) {
            return String.valueOf(startPageNo);
        }
        if (startPageNo == null) {
            return String.valueOf(endPageNo);
        }
        return startPageNo + "-" + endPageNo;
    }

    private static String truncateBlock(String block, int maxContextChars) {
        if (maxContextChars <= 0 || block.length() <= maxContextChars) {
            return block;
        }
        if (maxContextChars <= 16) {
            return block.substring(0, maxContextChars);
        }
        return block.substring(0, maxContextChars - 3) + "...";
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private static String defaultString(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static String sanitizeUntrustedText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replaceAll("(?i)</\\s*context\\s*>", "<\\\\/context>")
                .replaceAll("(?i)<\\s*context\\b", "<context-data")
                .replaceAll("\\[引用(\\d+)]", "[文档内引用$1]")
                .replaceAll("(?m)^(\\s*)(System|Developer|Assistant|User)\\s*:", "$1$2\\\\:");
    }

    private static final class MergedCitation {

        private final Long docId;
        private final String docTitle;
        private final List<Long> chunkIds = new ArrayList<>();
        private final Set<String> sectionTitles = new LinkedHashSet<>();
        private final StringBuilder content = new StringBuilder();
        private final String sourceUri;
        private final Double score;
        private Integer lastChunkIndex;
        private Integer startPageNo;
        private Integer endPageNo;

        private MergedCitation(RerankedChunk chunk) {
            this.docId = chunk.docId();
            this.docTitle = chunk.docTitle();
            this.sourceUri = chunk.sourceUri();
            this.score = chunk.rerankScore();
            merge(chunk);
        }

        private boolean canMerge(RerankedChunk chunk) {
            if (!Objects.equals(docId, chunk.docId())) {
                return false;
            }
            if (lastChunkIndex == null || chunk.chunkIndex() == null) {
                return false;
            }
            return chunk.chunkIndex() == lastChunkIndex + 1;
        }

        private void merge(RerankedChunk chunk) {
            chunkIds.add(chunk.chunkId());
            if (chunk.sectionTitle() != null && !chunk.sectionTitle().isBlank()) {
                sectionTitles.add(chunk.sectionTitle());
            }
            if (content.length() > 0) {
                content.append("\n\n");
            }
            content.append(chunk.content() == null ? "" : chunk.content());
            if (chunk.pageNo() != null) {
                startPageNo = startPageNo == null ? chunk.pageNo() : Math.min(startPageNo, chunk.pageNo());
                endPageNo = endPageNo == null ? chunk.pageNo() : Math.max(endPageNo, chunk.pageNo());
            }
            lastChunkIndex = chunk.chunkIndex();
        }

        private ContextCitation toCitation(int citationNo) {
            return new ContextCitation(
                    citationNo,
                    docId,
                    docTitle,
                    List.copyOf(chunkIds),
                    startPageNo,
                    endPageNo,
                    sectionTitle(),
                    score,
                    sourceUri
            );
        }

        private String docTitle() {
            return docTitle;
        }

        private Integer startPageNo() {
            return startPageNo;
        }

        private Integer endPageNo() {
            return endPageNo;
        }

        private String sectionTitle() {
            if (sectionTitles.isEmpty()) {
                return null;
            }
            return String.join(" / ", sectionTitles);
        }

        private String content() {
            return content.toString();
        }
    }
}
