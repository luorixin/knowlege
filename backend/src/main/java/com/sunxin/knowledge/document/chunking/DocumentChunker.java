package com.sunxin.knowledge.document.chunking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.document.dto.ParsedPageRequest;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.persistence.entity.KbDocument;

@Service
@EnableConfigurationProperties(ChunkingProperties.class)
public class DocumentChunker {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern NUMBERED_HEADING = Pattern.compile(
            "^(?:第[一二三四五六七八九十百千万0-9]+[章节条]\\s*|[一二三四五六七八九十]+[、.．]\\s*|\\d+(?:\\.\\d+)*[、.．\\s]+)(.+)$"
    );
    private static final Pattern SOW_HEADING = Pattern.compile("^(工作范围|服务范围|实施范围|交付物|里程碑|项目里程碑).*$");

    private final ChunkingProperties properties;

    public DocumentChunker(ChunkingProperties properties) {
        this.properties = properties;
    }

    public List<ChunkDraft> chunk(KbDocument document, RebuildChunksRequest request) {
        int chunkSize = request.chunkSize() == null ? properties.getDefaultSize() : request.chunkSize();
        int overlap = request.overlap() == null ? properties.getDefaultOverlap() : request.overlap();
        validateChunkOptions(chunkSize, overlap);

        List<SourceSegment> segments = new ArrayList<>();
        for (ParsedPageRequest page : request.pages()) {
            if (page.content() == null || page.content().isBlank()) {
                continue;
            }
            if (usesPageStrategy(document, page)) {
                segments.add(pageSegment(page));
            } else {
                segments.addAll(splitByHeadings(page));
            }
        }

        List<ChunkDraft> chunks = new ArrayList<>();
        for (SourceSegment segment : segments) {
            List<String> pieces = splitByLength(segment.content(), chunkSize, overlap);
            for (int partIndex = 0; partIndex < pieces.size(); partIndex++) {
                String content = pieces.get(partIndex);
                chunks.add(new ChunkDraft(
                        chunks.size(),
                        segment.pageNo(),
                        segment.sectionTitle(),
                        segment.contentType(),
                        content,
                        metadata(segment, partIndex, pieces.size(), chunkSize, overlap, content.length())
                ));
            }
        }
        return chunks;
    }

    private static void validateChunkOptions(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new BadRequestException("chunkSize must be greater than 0");
        }
        if (overlap < 0) {
            throw new BadRequestException("overlap must not be negative");
        }
        if (overlap >= chunkSize) {
            throw new BadRequestException("overlap must be smaller than chunkSize");
        }
    }

    private static boolean usesPageStrategy(KbDocument document, ParsedPageRequest page) {
        String docType = document.getDocType() == null ? "" : document.getDocType().toUpperCase(Locale.ROOT);
        String contentType = normalizedContentType(page.contentType());
        return "PPT".equals(docType)
                || "EXCEL".equals(docType)
                || "table".equals(contentType);
    }

    private static SourceSegment pageSegment(ParsedPageRequest page) {
        String sectionTitle = defaultSectionTitle(page);
        return new SourceSegment(
                page.pageNo(),
                sectionTitle,
                normalizedContentType(page.contentType()),
                page.content().strip(),
                page.metadata()
        );
    }

    private static List<SourceSegment> splitByHeadings(ParsedPageRequest page) {
        List<SourceSegment> segments = new ArrayList<>();
        String[] lines = page.content().split("\\R", -1);
        String currentTitle = defaultSectionTitle(page);
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            String headingTitle = headingTitle(line);
            if (headingTitle != null) {
                addSegmentIfPresent(segments, page, currentTitle, currentContent);
                currentTitle = headingTitle;
                currentContent = new StringBuilder();
            }
            appendLine(currentContent, line);
        }
        addSegmentIfPresent(segments, page, currentTitle, currentContent);

        if (segments.isEmpty()) {
            segments.add(pageSegment(page));
        }
        return segments;
    }

    private static void appendLine(StringBuilder content, String line) {
        if (content.length() > 0) {
            content.append('\n');
        }
        content.append(line);
    }

    private static void addSegmentIfPresent(
            List<SourceSegment> segments,
            ParsedPageRequest page,
            String sectionTitle,
            StringBuilder content
    ) {
        String value = content.toString().strip();
        if (!value.isBlank()) {
            segments.add(new SourceSegment(
                    page.pageNo(),
                    sectionTitle,
                    normalizedContentType(page.contentType()),
                    value,
                    page.metadata()
            ));
        }
    }

    private static String headingTitle(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        Matcher markdown = MARKDOWN_HEADING.matcher(trimmed);
        if (markdown.matches()) {
            return markdown.group(2).trim();
        }

        Matcher numbered = NUMBERED_HEADING.matcher(trimmed);
        if (numbered.matches()) {
            return numbered.group(1).trim();
        }

        Matcher sow = SOW_HEADING.matcher(trimmed);
        if (sow.matches()) {
            return trimmed;
        }

        return null;
    }

    private static List<String> splitByLength(String content, int chunkSize, int overlap) {
        String normalized = content.strip();
        if (normalized.length() <= chunkSize) {
            return List.of(normalized);
        }

        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            pieces.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
            int nextStart = end - overlap;
            start = nextStart <= start ? end : nextStart;
        }
        return pieces;
    }

    private static String defaultSectionTitle(ParsedPageRequest page) {
        if (page.sectionTitle() != null && !page.sectionTitle().isBlank()) {
            return page.sectionTitle().trim();
        }
        Object sheetName = page.metadata() == null ? null : page.metadata().get("sheet_name");
        if (sheetName != null && !sheetName.toString().isBlank()) {
            return sheetName.toString().trim();
        }
        return "page-" + page.pageNo();
    }

    private static String normalizedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "text";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, Object> metadata(
            SourceSegment segment,
            int partIndex,
            int partCount,
            int chunkSize,
            int overlap,
            int charCount
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("content_type", segment.contentType());
        metadata.put("page_no", segment.pageNo());
        metadata.put("section_title", segment.sectionTitle());
        metadata.put("chunk_size", chunkSize);
        metadata.put("overlap", overlap);
        metadata.put("part_index", partIndex);
        metadata.put("part_count", partCount);
        metadata.put("char_count", charCount);
        if (segment.metadata() != null && !segment.metadata().isEmpty()) {
            metadata.put("source_metadata", segment.metadata());
        }
        return metadata;
    }

    private record SourceSegment(
            Integer pageNo,
            String sectionTitle,
            String contentType,
            String content,
            Map<String, Object> metadata
    ) {
    }
}
