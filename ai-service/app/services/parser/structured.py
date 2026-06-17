from __future__ import annotations

import logging
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree

from app.schemas.document_parse import DocumentParseRequest, DocumentParseResponse, ParsedPage
from app.services.parser.errors import DocumentParseError

logger = logging.getLogger(__name__)

SUPPORTED_TYPES = {"txt", "md", "pdf", "docx", "pptx", "xlsx"}
XML_TEXT_NAMES = {"t"}


class StructuredDocumentParser:
    """MVP parser with replaceable format-specific extraction methods."""

    def parse(self, request: DocumentParseRequest) -> DocumentParseResponse:
        file_type = request.file_type.lower().strip()
        file_path = Path(request.file_path)
        logger.info(
            "Parsing document doc_id=%s version_id=%s file_type=%s file_path=%s",
            request.doc_id,
            request.version_id,
            file_type,
            file_path,
        )

        if file_type not in SUPPORTED_TYPES:
            raise DocumentParseError(
                code="UNSUPPORTED_FILE_TYPE",
                message=f"Unsupported file_type '{request.file_type}'. Supported types: {sorted(SUPPORTED_TYPES)}",
                status_code=400,
            )
        if not file_path.exists() or not file_path.is_file():
            raise DocumentParseError(
                code="FILE_NOT_FOUND",
                message=f"File not found: {file_path}",
                status_code=404,
            )

        try:
            pages = self._parse_by_type(file_path, file_type)
        except DocumentParseError:
            raise
        except Exception as exc:
            logger.exception("Failed to parse document file_path=%s file_type=%s", file_path, file_type)
            raise DocumentParseError(
                code="PARSE_FAILED",
                message=f"Failed to parse {file_type} document: {exc}",
                status_code=400,
            ) from exc

        logger.info(
            "Parsed document doc_id=%s version_id=%s pages=%s",
            request.doc_id,
            request.version_id,
            len(pages),
        )
        return DocumentParseResponse(
            doc_id=request.doc_id,
            version_id=request.version_id,
            pages=pages,
        )

    def _parse_by_type(self, file_path: Path, file_type: str) -> list[ParsedPage]:
        if file_type == "txt":
            return [self._parse_text_file(file_path, "txt")]
        if file_type == "md":
            return [self._parse_markdown_file(file_path)]
        if file_type == "pdf":
            return self._parse_pdf(file_path)
        if file_type == "docx":
            return [self._parse_docx(file_path)]
        if file_type == "pptx":
            return self._parse_pptx(file_path)
        if file_type == "xlsx":
            return self._parse_xlsx(file_path)
        raise DocumentParseError("UNSUPPORTED_FILE_TYPE", f"Unsupported file_type '{file_type}'")

    def _parse_text_file(self, file_path: Path, file_type: str) -> ParsedPage:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        return ParsedPage(
            page_no=1,
            section_title=file_path.name,
            content_type="text",
            content=content,
            metadata={"file_type": file_type},
        )

    def _parse_markdown_file(self, file_path: Path) -> ParsedPage:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        title = _first_markdown_heading(content) or file_path.name
        return ParsedPage(
            page_no=1,
            section_title=title,
            content_type="text",
            content=content,
            metadata={"file_type": "md"},
        )

    def _parse_pdf(self, file_path: Path) -> list[ParsedPage]:
        try:
            from pypdf import PdfReader
        except ImportError as exc:
            raise DocumentParseError(
                code="DEPENDENCY_MISSING",
                message="pypdf is required to parse PDF documents",
                status_code=500,
            ) from exc

        reader = PdfReader(str(file_path))
        pages: list[ParsedPage] = []
        for index, page in enumerate(reader.pages, start=1):
            content = page.extract_text() or ""
            pages.append(
                ParsedPage(
                    page_no=index,
                    section_title=file_path.name,
                    content_type="text",
                    content=content.strip(),
                    metadata={"file_type": "pdf", "page_index": index - 1},
                )
            )
        return pages

    def _parse_docx(self, file_path: Path) -> ParsedPage:
        with zipfile.ZipFile(file_path) as archive:
            xml_content = archive.read("word/document.xml")
        root = ElementTree.fromstring(xml_content)
        paragraphs = [
            "".join(_texts_in_element(paragraph)).strip()
            for paragraph in root.iter()
            if _local_name(paragraph.tag) == "p"
        ]
        paragraphs = [paragraph for paragraph in paragraphs if paragraph]
        content = "\n".join(paragraphs)
        return ParsedPage(
            page_no=1,
            section_title=_first_non_empty_line(content) or file_path.name,
            content_type="text",
            content=content,
            metadata={"file_type": "docx", "paragraph_count": len(paragraphs)},
        )

    def _parse_pptx(self, file_path: Path) -> list[ParsedPage]:
        pages: list[ParsedPage] = []
        with zipfile.ZipFile(file_path) as archive:
            slide_names = sorted(
                (name for name in archive.namelist() if re.fullmatch(r"ppt/slides/slide\d+\.xml", name)),
                key=_natural_sort_key,
            )
            for page_no, slide_name in enumerate(slide_names, start=1):
                root = ElementTree.fromstring(archive.read(slide_name))
                lines = _group_texts_by_paragraph(root)
                content = "\n".join(line for line in lines if line)
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=_first_non_empty_line(content) or f"Slide {page_no}",
                        content_type="text",
                        content=content,
                        metadata={"file_type": "pptx", "slide_path": slide_name},
                    )
                )
        return pages

    def _parse_xlsx(self, file_path: Path) -> list[ParsedPage]:
        with zipfile.ZipFile(file_path) as archive:
            shared_strings = _read_shared_strings(archive)
            sheet_names = sorted(
                (name for name in archive.namelist() if re.fullmatch(r"xl/worksheets/sheet\d+\.xml", name)),
                key=_natural_sort_key,
            )
            pages = []
            for page_no, sheet_name in enumerate(sheet_names, start=1):
                rows = _read_sheet_rows(archive.read(sheet_name), shared_strings)
                content = "\n".join("\t".join(row).rstrip() for row in rows if any(row))
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=Path(sheet_name).stem,
                        content_type="table",
                        content=content,
                        metadata={"file_type": "xlsx", "sheet_name": Path(sheet_name).stem},
                    )
                )
        return pages


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


def _group_texts_by_paragraph(root: ElementTree.Element) -> list[str]:
    lines: list[str] = []
    for paragraph in root.iter():
        if _local_name(paragraph.tag) == "p":
            text = "".join(_texts_in_element(paragraph)).strip()
            if text:
                lines.append(text)
    if lines:
        return lines
    return ["".join(_texts_in_element(root)).strip()]


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


def _natural_sort_key(value: str) -> list[int | str]:
    parts = re.split(r"(\d+)", value)
    return [int(part) if part.isdigit() else part for part in parts]


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1] if "}" in tag else tag
