package com.sunxin.knowledge.document.chunking;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final Pattern SENTENCE = Pattern.compile("[^。！？；;.!?]+[。！？；;.!?]?");
    private static final List<String> PASSTHROUGH_METADATA_KEYS = List.of(
            "block_type",
            "bbox",
            "confidence",
            "image_uri",
            "source_uri",
            "caption_provider",
            "sheet_name",
            "range"
    );

    private final ChunkingProperties properties;

    public DocumentChunker(ChunkingProperties properties) {
        this.properties = properties;
    }

    public List<ChunkDraft> chunk(KbDocument document, RebuildChunksRequest request) {
        ChunkOptions options = chunkOptions(request);
        validateChunkOptions(options);

        List<SourceSegment> segments = new ArrayList<>();
        for (ParsedPageRequest page : request.pages()) {
            if (page.content() == null || page.content().isBlank()) {
                continue;
            }
            if (usesPageStrategy(document, page)) {
                segments.add(pageSegment(document, page));
            } else {
                segments.addAll(splitByHeadings(page));
            }
        }

        List<ChunkDraft> chunks = new ArrayList<>();
        for (SourceSegment segment : segments) {
            List<ChunkPiece> pieces = splitSegment(segment, options);
            for (int partIndex = 0; partIndex < pieces.size(); partIndex++) {
                ChunkPiece piece = pieces.get(partIndex);
                chunks.add(new ChunkDraft(
                        chunks.size(),
                        segment.pageNo(),
                        segment.sectionTitle(),
                        segment.contentType(),
                        piece.content(),
                        metadata(segment, piece, partIndex, pieces.size(), options)
                ));
            }
        }
        return chunks;
    }

    private ChunkOptions chunkOptions(RebuildChunksRequest request) {
        int maxSize = request.chunkSize() == null ? properties.getMaxSize() : request.chunkSize();
        int targetSize = request.chunkSize() == null
                ? properties.getTargetSize()
                : Math.min(request.chunkSize(), Math.max(properties.getMinSize(), request.chunkSize() - 100));
        int overlap = request.overlap() == null ? properties.getOverlap() : request.overlap();
        int minSize = Math.min(properties.getMinSize(), targetSize);
        return new ChunkOptions(
                targetSize,
                maxSize,
                minSize,
                overlap,
                properties.isSentenceBoundaryEnabled(),
                properties.isTableHeaderRepeatEnabled(),
                properties.getStrategyVersion()
        );
    }

    private static void validateChunkOptions(ChunkOptions options) {
        if (options.maxSize() <= 0) {
            throw new BadRequestException("chunkSize must be greater than 0");
        }
        if (options.targetSize() <= 0) {
            throw new BadRequestException("targetSize must be greater than 0");
        }
        if (options.targetSize() > options.maxSize()) {
            throw new BadRequestException("targetSize must not be greater than maxSize");
        }
        if (options.minSize() < 0) {
            throw new BadRequestException("minSize must not be negative");
        }
        if (options.overlap() < 0) {
            throw new BadRequestException("overlap must not be negative");
        }
        if (options.overlap() >= options.maxSize()) {
            throw new BadRequestException("overlap must be smaller than chunkSize");
        }
    }

    private static boolean usesPageStrategy(KbDocument document, ParsedPageRequest page) {
        String docType = document.getDocType() == null ? "" : document.getDocType().toUpperCase(Locale.ROOT);
        String contentType = normalizedContentType(page.contentType());
        return "PPT".equals(docType)
                || "EXCEL".equals(docType)
                || "table".equals(contentType)
                || "figure".equals(contentType)
                || "image".equals(contentType);
    }

    private static SourceSegment pageSegment(KbDocument document, ParsedPageRequest page) {
        String sectionTitle = defaultSectionTitle(page);
        String contentType = normalizedContentType(page.contentType());
        return new SourceSegment(
                page.pageNo(),
                sectionTitle,
                contentType,
                page.content().strip(),
                page.metadata(),
                List.of(sectionTitle),
                null,
                null,
                pageSplitStrategy(document, contentType),
                allowsOverlap(document, contentType)
        );
    }

    private static List<SourceSegment> splitByHeadings(ParsedPageRequest page) {
        List<SourceSegment> segments = new ArrayList<>();
        String[] lines = page.content().split("\\R", -1);
        String currentTitle = defaultSectionTitle(page);
        List<Heading> headingStack = new ArrayList<>();
        List<String> currentPath = List.of(currentTitle);
        Integer currentHeadingLevel = null;
        String currentParentSection = null;
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            Heading heading = heading(line);
            if (heading != null) {
                addSegmentIfPresent(
                        segments,
                        page,
                        currentTitle,
                        currentContent,
                        currentPath,
                        currentHeadingLevel,
                        currentParentSection
                );
                while (!headingStack.isEmpty() && headingStack.get(headingStack.size() - 1).level() >= heading.level()) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(heading);
                currentTitle = heading.title();
                currentPath = headingStack.stream().map(Heading::title).toList();
                currentHeadingLevel = heading.level();
                currentParentSection = headingStack.size() > 1
                        ? headingStack.get(headingStack.size() - 2).title()
                        : null;
                currentContent = new StringBuilder();
            }
            appendLine(currentContent, line);
        }
        addSegmentIfPresent(
                segments,
                page,
                currentTitle,
                currentContent,
                currentPath,
                currentHeadingLevel,
                currentParentSection
        );

        if (segments.isEmpty()) {
            segments.add(genericTextSegment(page));
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
            StringBuilder content,
            List<String> sectionPath,
            Integer headingLevel,
            String parentSection
    ) {
        String value = content.toString().strip();
        if (!value.isBlank()) {
            segments.add(new SourceSegment(
                    page.pageNo(),
                    sectionTitle,
                    normalizedContentType(page.contentType()),
                    value,
                    page.metadata(),
                    sectionPath,
                    headingLevel,
                    parentSection,
                    "semantic-boundary",
                    true
            ));
        }
    }

    private static Heading heading(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        Matcher markdown = MARKDOWN_HEADING.matcher(trimmed);
        if (markdown.matches()) {
            return new Heading(markdown.group(2).trim(), markdown.group(1).length());
        }

        Matcher numbered = NUMBERED_HEADING.matcher(trimmed);
        if (numbered.matches()) {
            String title = numbered.group(1).trim();
            String prefix = trimmed.substring(0, Math.max(0, trimmed.length() - numbered.group(1).length()));
            int level = prefix.contains(".") ? (int) prefix.chars().filter(ch -> ch == '.').count() + 1 : 1;
            return new Heading(title, level);
        }

        Matcher sow = SOW_HEADING.matcher(trimmed);
        if (sow.matches()) {
            return new Heading(trimmed, 1);
        }

        return null;
    }

    private static SourceSegment genericTextSegment(ParsedPageRequest page) {
        String sectionTitle = defaultSectionTitle(page);
        return new SourceSegment(
                page.pageNo(),
                sectionTitle,
                normalizedContentType(page.contentType()),
                page.content().strip(),
                page.metadata(),
                List.of(sectionTitle),
                null,
                null,
                "semantic-boundary",
                true
        );
    }

    private static String pageSplitStrategy(KbDocument document, String contentType) {
        String docType = document.getDocType() == null ? "" : document.getDocType().toUpperCase(Locale.ROOT);
        if ("table".equals(contentType) || "EXCEL".equals(docType)) {
            return "table-row-window";
        }
        if ("figure".equals(contentType) || "image".equals(contentType)) {
            return "figure-caption";
        }
        if ("PPT".equals(docType)) {
            return "ppt-page";
        }
        return "page";
    }

    private static boolean allowsOverlap(KbDocument document, String contentType) {
        String docType = document.getDocType() == null ? "" : document.getDocType().toUpperCase(Locale.ROOT);
        return !"PPT".equals(docType)
                && !"EXCEL".equals(docType)
                && !"table".equals(contentType)
                && !"figure".equals(contentType)
                && !"image".equals(contentType);
    }

    private static List<ChunkPiece> splitSegment(SourceSegment segment, ChunkOptions options) {
        if ("table-row-window".equals(segment.splitStrategy())) {
            return splitTable(segment.content(), options);
        }
        if (segment.content().length() <= options.maxSize()) {
            return List.of(new ChunkPiece(segment.content().strip(), segment.splitStrategy(), false, 0, Map.of()));
        }
        List<ChunkPiece> pieces = splitBySemanticBoundary(
                segment.content(),
                options,
                segment.allowOverlap(),
                segment.splitStrategy()
        );
        if (!segment.allowOverlap()) {
            return pieces.stream()
                    .map(piece -> new ChunkPiece(
                            piece.content(),
                            piece.splitStrategy(),
                            piece.truncatedByFallback(),
                            0,
                            piece.extraMetadata()
                    ))
                    .toList();
        }
        return pieces;
    }

    private static List<ChunkPiece> splitTable(String content, ChunkOptions options) {
        String normalized = content.strip();
        if (normalized.length() <= options.maxSize()) {
            return List.of(tablePiece(normalized, false));
        }
        List<String> lines = normalized.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return List.of();
        }

        List<String> headerLines = new ArrayList<>();
        headerLines.add(lines.get(0));
        int rowStart = 1;
        if (lines.size() > 1 && isMarkdownTableSeparator(lines.get(1))) {
            headerLines.add(lines.get(1));
            rowStart = 2;
        }
        String header = String.join("\n", headerLines);
        List<ChunkPiece> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder(header);
        for (int index = rowStart; index < lines.size(); index++) {
            String row = lines.get(index);
            int candidateLength = current.length() + 1 + row.length();
            if (current.length() > header.length() && candidateLength > options.maxSize()) {
                pieces.add(tablePiece(current.toString(), true));
                current = new StringBuilder(header);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(row);
        }
        if (!current.isEmpty()) {
            pieces.add(tablePiece(current.toString(), options.tableHeaderRepeatEnabled()));
        }
        return pieces;
    }

    private static boolean isMarkdownTableSeparator(String line) {
        String normalized = line.replace("|", "").replace(":", "").replace("-", "").strip();
        return normalized.isBlank() && line.contains("-");
    }

    private static ChunkPiece tablePiece(String content, boolean tableHeaderRepeated) {
        return new ChunkPiece(
                content.strip(),
                "table-row-window",
                false,
                0,
                Map.of("table_header_repeated", tableHeaderRepeated)
        );
    }

    private static List<ChunkPiece> splitBySemanticBoundary(
            String content,
            ChunkOptions options,
            boolean allowOverlap,
            String splitStrategy
    ) {
        List<ChunkPiece> basePieces = packUnits(semanticUnits(content, options), options, splitStrategy);
        if (!allowOverlap || options.overlap() == 0 || basePieces.size() <= 1) {
            return basePieces;
        }

        List<ChunkPiece> withOverlap = new ArrayList<>();
        withOverlap.add(basePieces.get(0));
        for (int index = 1; index < basePieces.size(); index++) {
            ChunkPiece previous = withOverlap.get(index - 1);
            ChunkPiece current = basePieces.get(index);
            String overlapText = boundarySuffix(previous.content(), options.overlap());
            if (!overlapText.isBlank()) {
                String candidate = overlapText + "\n" + current.content();
                if (candidate.length() <= options.maxSize()) {
                    withOverlap.add(new ChunkPiece(
                            candidate,
                            current.splitStrategy(),
                            current.truncatedByFallback(),
                            overlapText.length(),
                            current.extraMetadata()
                    ));
                    continue;
                }
            }
            withOverlap.add(current);
        }
        return withOverlap;
    }

    private static List<ChunkPiece> packUnits(List<String> units, ChunkOptions options, String splitStrategy) {
        List<ChunkPiece> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean currentFallback = false;

        for (String unit : units) {
            if (unit.isBlank()) {
                continue;
            }
            if (unit.length() > options.maxSize()) {
                flushPiece(pieces, current, splitStrategy, currentFallback);
                currentFallback = false;
                pieces.addAll(splitLongUnit(unit, options.maxSize(), splitStrategy));
                continue;
            }
            if (current.isEmpty()) {
                current.append(unit);
                continue;
            }
            String separator = separator(current.toString(), unit);
            int candidateLength = current.length() + separator.length() + unit.length();
            if (candidateLength <= options.maxSize()
                    && (current.length() < options.targetSize() || candidateLength <= options.targetSize())) {
                current.append(separator).append(unit);
            } else {
                flushPiece(pieces, current, splitStrategy, currentFallback);
                currentFallback = false;
                current.append(unit);
            }
        }
        flushPiece(pieces, current, splitStrategy, currentFallback);
        return mergeShortPieces(pieces, options);
    }

    private static List<String> semanticUnits(String content, ChunkOptions options) {
        String normalized = content.strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (!options.sentenceBoundaryEnabled()) {
            return List.of(normalized);
        }

        List<String> units = new ArrayList<>();
        for (String paragraph : normalized.split("\\R\\s*\\R")) {
            String trimmedParagraph = paragraph.strip();
            if (trimmedParagraph.isBlank()) {
                continue;
            }
            for (String line : trimmedParagraph.split("\\R")) {
                String trimmedLine = line.strip();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                if (heading(trimmedLine) != null || isListItem(trimmedLine)) {
                    units.add(trimmedLine);
                    continue;
                }
                Matcher matcher = SENTENCE.matcher(trimmedLine);
                boolean matched = false;
                while (matcher.find()) {
                    String sentence = matcher.group().strip();
                    if (!sentence.isBlank()) {
                        units.add(sentence);
                        matched = true;
                    }
                }
                if (!matched) {
                    units.add(trimmedLine);
                }
            }
        }
        return units;
    }

    private static boolean isListItem(String line) {
        return line.startsWith("- ")
                || line.startsWith("* ")
                || line.startsWith("• ")
                || line.matches("^\\d+[、.．)]\\s+.+$");
    }

    private static List<ChunkPiece> splitLongUnit(String unit, int maxSize, String splitStrategy) {
        List<ChunkPiece> pieces = new ArrayList<>();
        int start = 0;
        while (start < unit.length()) {
            int end = Math.min(start + maxSize, unit.length());
            pieces.add(new ChunkPiece(unit.substring(start, end).strip(), splitStrategy, true, 0, Map.of()));
            start = end;
        }
        return pieces;
    }

    private static void flushPiece(
            List<ChunkPiece> pieces,
            StringBuilder current,
            String splitStrategy,
            boolean truncatedByFallback
    ) {
        String content = current.toString().strip();
        if (!content.isBlank()) {
            pieces.add(new ChunkPiece(content, splitStrategy, truncatedByFallback, 0, Map.of()));
        }
        current.setLength(0);
    }

    private static List<ChunkPiece> mergeShortPieces(List<ChunkPiece> pieces, ChunkOptions options) {
        if (pieces.size() <= 1 || options.minSize() <= 0) {
            return pieces;
        }
        List<ChunkPiece> merged = new ArrayList<>();
        for (ChunkPiece piece : pieces) {
            if (!merged.isEmpty()) {
                ChunkPiece previous = merged.get(merged.size() - 1);
                String candidate = previous.content() + separator(previous.content(), piece.content()) + piece.content();
                if ((previous.content().length() < options.minSize() || piece.content().length() < options.minSize())
                        && candidate.length() <= options.maxSize()) {
                    merged.set(merged.size() - 1, new ChunkPiece(
                            candidate,
                            piece.splitStrategy(),
                            previous.truncatedByFallback() || piece.truncatedByFallback(),
                            0,
                            mergedExtraMetadata(previous, piece)
                    ));
                    continue;
                }
            }
            merged.add(piece);
        }
        return merged;
    }

    private static Map<String, Object> mergedExtraMetadata(ChunkPiece first, ChunkPiece second) {
        if (first.extraMetadata().isEmpty()) {
            return second.extraMetadata();
        }
        if (second.extraMetadata().isEmpty()) {
            return first.extraMetadata();
        }
        Map<String, Object> merged = new LinkedHashMap<>(first.extraMetadata());
        merged.putAll(second.extraMetadata());
        return merged;
    }

    private static String separator(String current, String next) {
        if (current.endsWith("\n") || next.startsWith("\n")) {
            return "";
        }
        return "\n";
    }

    private static String boundarySuffix(String content, int overlap) {
        String normalized = content.strip();
        if (normalized.length() <= overlap) {
            return normalized;
        }
        int preferredStart = Math.max(0, normalized.length() - overlap);
        for (int index = preferredStart; index >= 0; index--) {
            char ch = normalized.charAt(index);
            if (ch == '\n' || ch == '。' || ch == '！' || ch == '？' || ch == ';' || ch == '；'
                    || ch == '.' || ch == '!' || ch == '?') {
                String suffix = normalized.substring(index + 1).strip();
                if (suffix.length() >= Math.min(6, overlap)) {
                    return suffix;
                }
            }
        }
        return "";
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
            ChunkPiece piece,
            int partIndex,
            int partCount,
            ChunkOptions options
    ) {
        int charCount = piece.content().length();
        boolean tooShort = charCount < options.minSize();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("content_type", segment.contentType());
        metadata.put("block_type", sourceMetadataValue(segment, "block_type", segment.contentType()));
        metadata.put("page_no", segment.pageNo());
        metadata.put("section_title", segment.sectionTitle());
        metadata.put("section_path", segment.sectionPath());
        metadata.put("heading_level", segment.headingLevel());
        metadata.put("parent_section", segment.parentSection());
        metadata.put("split_strategy", piece.splitStrategy());
        metadata.put("chunking_strategy_version", options.strategyVersion());
        metadata.put("target_size", options.targetSize());
        metadata.put("max_size", options.maxSize());
        metadata.put("min_size", options.minSize());
        metadata.put("chunk_size", options.maxSize());
        metadata.put("overlap", piece.overlap());
        metadata.put("part_index", partIndex);
        metadata.put("part_count", partCount);
        metadata.put("char_count", charCount);
        metadata.put("estimated_token_count", estimateTokenCount(charCount));
        metadata.put("too_short", tooShort);
        metadata.put("truncated_by_fallback", piece.truncatedByFallback());
        metadata.put("chunk_quality_score", chunkQualityScore(tooShort, piece.truncatedByFallback()));
        metadata.put("source_block_ids", sourceBlockIds(segment.metadata()));
        metadata.putAll(piece.extraMetadata());
        copyPassthroughMetadata(segment, metadata);
        if (segment.metadata() != null && !segment.metadata().isEmpty()) {
            metadata.put("source_metadata", segment.metadata());
        }
        return metadata;
    }

    private static Object sourceMetadataValue(SourceSegment segment, String key, Object fallback) {
        if (segment.metadata() == null || !segment.metadata().containsKey(key)) {
            return fallback;
        }
        return segment.metadata().get(key);
    }

    private static void copyPassthroughMetadata(SourceSegment segment, Map<String, Object> metadata) {
        if (segment.metadata() == null || segment.metadata().isEmpty()) {
            return;
        }
        for (String key : PASSTHROUGH_METADATA_KEYS) {
            if (segment.metadata().containsKey(key)) {
                metadata.put(key, segment.metadata().get(key));
            }
        }
    }

    private static List<Object> sourceBlockIds(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return List.of();
        }
        Object blockIds = metadata.get("block_ids");
        if (blockIds instanceof List<?> list) {
            return Collections.unmodifiableList(new ArrayList<>(list));
        }
        Object blockId = metadata.get("block_id");
        if (blockId == null) {
            return List.of();
        }
        return List.of(blockId);
    }

    private static int estimateTokenCount(int charCount) {
        return Math.max(1, charCount);
    }

    private static double chunkQualityScore(boolean tooShort, boolean truncatedByFallback) {
        if (truncatedByFallback) {
            return 0.62;
        }
        if (tooShort) {
            return 0.78;
        }
        return 1.0;
    }

    private record SourceSegment(
            Integer pageNo,
            String sectionTitle,
            String contentType,
            String content,
            Map<String, Object> metadata,
            List<String> sectionPath,
            Integer headingLevel,
            String parentSection,
            String splitStrategy,
            boolean allowOverlap
    ) {
    }

    private record Heading(String title, int level) {
    }

    private record ChunkOptions(
            int targetSize,
            int maxSize,
            int minSize,
            int overlap,
            boolean sentenceBoundaryEnabled,
            boolean tableHeaderRepeatEnabled,
            String strategyVersion
    ) {
    }

    private record ChunkPiece(
            String content,
            String splitStrategy,
            boolean truncatedByFallback,
            int overlap,
            Map<String, Object> extraMetadata
    ) {
    }
}
