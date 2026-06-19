from __future__ import annotations

import re
import time
import zipfile
from pathlib import Path
from typing import Any
from xml.etree import ElementTree

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResult,
    PageParseError,
    ParsedBlock,
    ParsedPage,
)
from app.services.parser.base import OcrResult, VLMCaptionResult

XML_TEXT_NAMES = {"t"}
EXCEL_TABLE_BLOCK_MAX_ROWS = 5


class TextParser:
    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        return _result_from_single_block(
            request=request,
            parser_name="TextParser",
            page=ParsedPage(
                page_no=1,
                section_title=request.doc_id, # Desensitize section title, use doc_id instead of file_path.name
                content_type="text",
                content=content,
                metadata={"file_type": file_type},
            ),
            block=_block(
                request,
                file_path,
                page_no=1,
                block_type="paragraph",
                content=content,
                section_title=request.doc_id, # Desensitize section title
                confidence=1.0,
                metadata={"file_type": file_type},
            ),
        )


class MarkdownParser:
    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        title = _first_markdown_heading(content) or request.doc_id # Desensitize
        return _result_from_single_block(
            request=request,
            parser_name="MarkdownParser",
            page=ParsedPage(
                page_no=1,
                section_title=title,
                content_type="text",
                content=content,
                metadata={"file_type": "md", "parser": "MarkdownParser"},
            ),
            block=_block(
                request,
                file_path,
                page_no=1,
                block_type="paragraph",
                content=content,
                section_title=title,
                markdown=content,
                confidence=1.0,
                metadata={"file_type": "md"},
            ),
        )


def _result_from_single_block(
    request: DocumentParseRequest,
    parser_name: str,
    page: ParsedPage,
    block: ParsedBlock,
) -> DocumentParseResult:
    return _result(request, parser_name, [page], [block], [])


def _result(
    request: DocumentParseRequest,
    parser_name: str,
    pages: list[ParsedPage],
    blocks: list[ParsedBlock],
    errors: list[PageParseError],
    metadata: dict[str, Any] | None = None,
) -> DocumentParseResult:
    status = "SUCCESS"
    if errors and pages:
        status = "PARTIAL_SUCCESS"
    elif errors and not pages:
        status = "FAILED"
    markdown = "\n\n".join(block.markdown or block.content for block in blocks)
    return DocumentParseResult(
        doc_id=request.doc_id,
        version_id=request.version_id,
        status=status,
        pages=pages,
        blocks=blocks,
        errors=errors,
        markdown=markdown,
        metadata={"parser": parser_name, "block_count": len(blocks), "page_count": len(pages), **(metadata or {})},
    )


def _block(
    request: DocumentParseRequest,
    file_path: Path,
    page_no: int,
    block_type: str,
    content: str,
    section_title: str | None,
    markdown: str | None = None,
    html: str | None = None,
    bbox: list[float] | None = None,
    confidence: float | None = None,
    image_uri: str | None = None,
    metadata: dict | None = None,
    block_no: int = 1,
) -> ParsedBlock:
    return ParsedBlock(
        block_id=f"{request.doc_id}:{request.version_id}:{page_no}:{block_type}:{block_no}",
        block_type=block_type,
        page_no=page_no,
        section_title=section_title,
        content=content,
        markdown=markdown if markdown is not None else _markdown_for_block(block_type, content),
        html=html,
        bbox=bbox,
        confidence=confidence,
        image_uri=image_uri,
        source_uri=image_uri or f"doc:{request.doc_id}/version:{request.version_id}/page:{page_no}",
        metadata=metadata or {},
    )


def _markdown_for_block(block_type: str, content: str) -> str:
    if block_type == "title":
        return f"# {content.strip()}" if content.strip() else ""
    return content


def _page_error(page_no: int | None, exc: Exception, metadata: dict | None = None, code: str = "PAGE_PARSE_FAILED") -> PageParseError:
    return PageParseError(
        page_no=page_no,
        code=code,
        message=str(exc) or exc.__class__.__name__,
        metadata=metadata or {},
    )


def _normalize_ocr_result(value: str | OcrResult) -> OcrResult:
    if isinstance(value, OcrResult):
        return value
    return OcrResult(text=value, confidence=0.75, provider="legacy-string")


def _normalize_caption_result(value: str | VLMCaptionResult) -> VLMCaptionResult:
    if isinstance(value, VLMCaptionResult):
        return value
    return VLMCaptionResult(caption=value, provider="legacy-string", confidence=0.8)


def _ocr_metadata(ocr: OcrResult, file_type: str, page_parse_mode: str, elapsed_ms: int | None) -> dict[str, Any]:
    metadata: dict[str, Any] = {
        "file_type": file_type,
        "parser": "OCRParser",
        "page_parse_mode": page_parse_mode,
    }
    if ocr.provider:
        metadata["ocr_provider"] = ocr.provider
    if ocr.language:
        metadata["language"] = ocr.language
    if elapsed_ms is not None:
        metadata["elapsed_ms"] = elapsed_ms
    if ocr.metadata:
        metadata["ocr_metadata"] = ocr.metadata
    return metadata


def _caption_metadata(caption: VLMCaptionResult) -> dict[str, Any]:
    metadata: dict[str, Any] = {}
    if caption.provider:
        metadata["caption_provider"] = caption.provider
    if caption.model_name:
        metadata["caption_model"] = caption.model_name
    if caption.token_count is not None:
        metadata["caption_tokens"] = caption.token_count
    if caption.error_code:
        metadata["caption_error_code"] = caption.error_code
    if caption.error_message:
        metadata["caption_error_message"] = caption.error_message
    if caption.metadata:
        metadata["caption_metadata"] = caption.metadata
    return metadata


def _elapsed_ms(started_at: float) -> int:
    return int((time.perf_counter() - started_at) * 1000)


def _first_markdown_heading(content: str) -> str | None:
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            return stripped.lstrip("#").strip() or None
    return None


def _first_non_empty_line(content: str) -> str | None:
    for line in content.splitlines():
        if line.strip():
            return line.strip()
    return None


def _texts_in_element(element: ElementTree.Element) -> list[str]:
    return [
        child.text or ""
        for child in element.iter()
        if _local_name(child.tag) in XML_TEXT_NAMES and child.text
    ]


def _first_child_by_local_name(element: ElementTree.Element, local_name: str) -> ElementTree.Element | None:
    for child in element:
        if _local_name(child.tag) == local_name:
            return child
        nested = _first_child_by_local_name(child, local_name)
        if nested is not None:
            return nested
    return None


def _group_texts_by_paragraph(root: ElementTree.Element) -> list[str]:
    lines: list[str] = []
    for paragraph in root.iter():
        if _local_name(paragraph.tag) == "p":
            text = "".join(_texts_in_element(paragraph)).strip()
            if text:
                lines.append(text)
    if lines:
        return lines
    fallback = "".join(_texts_in_element(root)).strip()
    return [fallback] if fallback else []


def _is_list_paragraph(paragraph: ElementTree.Element) -> bool:
    return any(_local_name(child.tag) == "numPr" for child in paragraph.iter())


def _paragraph_heading_level(paragraph: ElementTree.Element) -> int | None:
    for child in paragraph.iter():
        if _local_name(child.tag) != "pStyle":
            continue
        style = child.attrib.get("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}val") or child.attrib.get("val")
        if not style or not style.startswith("Heading"):
            return None
        try:
            return int(style.replace("Heading", ""))
        except ValueError:
            return None
    return None


def _table_rows(table: ElementTree.Element) -> list[list[str]]:
    rows: list[list[str]] = []
    for row in table:
        if _local_name(row.tag) != "tr":
            continue
        cells = []
        for cell in row:
            if _local_name(cell.tag) in {"tc", "th"}:
                cells.append("\n".join(text.strip() for text in _texts_in_element(cell) if text.strip()))
        if cells:
            rows.append(cells)
    if rows:
        return rows

    for row in table.iter():
        if _local_name(row.tag) != "tr":
            continue
        cells = [
            "\n".join(text.strip() for text in _texts_in_element(cell) if text.strip())
            for cell in row
            if _local_name(cell.tag) in {"tc", "th"}
        ]
        if cells:
            rows.append(cells)
    return rows


def _tables_in_element(root: ElementTree.Element) -> list[list[list[str]]]:
    return [_table_rows(table) for table in root.iter() if _local_name(table.tag) == "tbl" and _table_rows(table)]


def _has_xml_local_name(root: ElementTree.Element, local_name: str) -> bool:
    return any(_local_name(element.tag) == local_name for element in root.iter())


def _read_shared_strings(archive: zipfile.ZipFile) -> list[str]:
    try:
        root = ElementTree.fromstring(archive.read("xl/sharedStrings.xml"))
    except KeyError:
        return []
    shared_strings = []
    for item in root.iter():
        if _local_name(item.tag) == "si":
            shared_strings.append("".join(_texts_in_element(item)))
    return shared_strings


def _read_workbook_sheet_titles(archive: zipfile.ZipFile) -> dict[str, str]:
    try:
        workbook = ElementTree.fromstring(archive.read("xl/workbook.xml"))
        rels = ElementTree.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
    except KeyError:
        return {}

    rel_targets: dict[str, str] = {}
    for relationship in rels.iter():
        if _local_name(relationship.tag) != "Relationship":
            continue
        rel_id = relationship.attrib.get("Id")
        target = relationship.attrib.get("Target")
        if rel_id and target:
            rel_targets[rel_id] = "xl/" + target.lstrip("/")

    titles: dict[str, str] = {}
    for sheet in workbook.iter():
        if _local_name(sheet.tag) != "sheet":
            continue
        name = sheet.attrib.get("name")
        rel_id = sheet.attrib.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id")
        if name and rel_id and rel_id in rel_targets:
            titles[rel_targets[rel_id]] = name
    return titles


def _read_sheet_rows(xml_content: bytes, shared_strings: list[str]) -> list[list[str]]:
    root = ElementTree.fromstring(xml_content)
    rows: list[list[str]] = []
    for row in root.iter():
        if _local_name(row.tag) != "row":
            continue
        values = []
        for cell in sorted([child for child in row if _local_name(child.tag) == "c"], key=_cell_column_index):
            values.append(_cell_value(cell, shared_strings))
        rows.append(values)
    return rows


def _read_sheet_formulas(xml_content: bytes) -> list[dict[str, str]]:
    root = ElementTree.fromstring(xml_content)
    formulas: list[dict[str, str]] = []
    for cell in root.iter():
        if _local_name(cell.tag) != "c":
            continue
        reference = cell.attrib.get("r", "")
        for child in cell:
            if _local_name(child.tag) == "f" and child.text:
                formulas.append({"cell": reference, "formula": child.text})
                break
    return formulas


def _split_table_rows(rows: list[list[str]], max_rows: int) -> list[list[list[str]]]:
    rows = [row for row in rows if any(row)]
    if not rows:
        return []
    if len(rows) <= max_rows:
        return [rows]
    header = rows[0]
    chunks: list[list[list[str]]] = []
    body = rows[1:]
    step = max(max_rows - 1, 1)
    for start in range(0, len(body), step):
        chunks.append([header, *body[start:start + step]])
    return chunks


def _cell_value(cell: ElementTree.Element, shared_strings: list[str]) -> str:
    cell_type = cell.attrib.get("t")
    if cell_type == "inlineStr":
        return "".join(_texts_in_element(cell))

    value = None
    for child in cell:
        if _local_name(child.tag) == "v":
            value = child.text
            break
    if value is None:
        return ""
    if cell_type == "s":
        try:
            return shared_strings[int(value)]
        except (IndexError, ValueError):
            return ""
    return value


def _cell_column_index(cell: ElementTree.Element) -> int:
    reference = cell.attrib.get("r", "")
    letters = "".join(ch for ch in reference if ch.isalpha()).upper()
    if not letters:
        return 0
    column = 0
    for letter in letters:
        column = column * 26 + (ord(letter) - ord("A") + 1)
    return column


def _sheet_dimension(root: ElementTree.Element) -> str:
    for dim in root.iter():
        if _local_name(dim.tag) == "dimension":
            return dim.attrib.get("ref", "unknown")
    return "unknown"


def _merged_cells(root: ElementTree.Element) -> list[str]:
    cells = []
    for cell in root.iter():
        if _local_name(cell.tag) == "mergeCell":
            ref = cell.attrib.get("ref")
            if ref:
                cells.append(ref)
    return cells


def _table_markdown(rows: list[list[str]]) -> str:
    rows = [row for row in rows if any(row)]
    if not rows:
        return ""
    width = max(len(row) for row in rows)
    normalized = [row + [""] * (width - len(row)) for row in rows]
    header = normalized[0]
    lines = [
        "| " + " | ".join(header) + " |",
        "| " + " | ".join(["---"] * width) + " |",
    ]
    for row in normalized[1:]:
        lines.append("| " + " | ".join(row) + " |")
    return "\n".join(lines)


def _natural_sort_key(value: str) -> list[int | str]:
    parts = re.split(r"(\d+)", value)
    return [int(part) if part.isdigit() else part for part in parts]


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1] if "}" in tag else tag
