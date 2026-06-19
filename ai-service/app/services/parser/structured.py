from __future__ import annotations

import logging
import re
import time
import zipfile
from pathlib import Path
from typing import Any
from xml.etree import ElementTree

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResponse,
    DocumentParseResult,
    PageParseError,
    ParsedBlock,
    ParsedPage,
)
from app.services.parser.base import (
    DocumentParser,
    MockOcrProvider,
    MockVLMCaptionProvider,
    OcrProvider,
    OcrResult,
    VLMCaptionProvider,
    VLMCaptionResult,
)
from app.services.parser.errors import DocumentParseError, OcrProviderError

logger = logging.getLogger(__name__)

TEXT_TYPES = {"txt", "md"}
OFFICE_TYPES = {"docx", "pptx", "xlsx"}
PDF_TYPES = {"pdf"}
IMAGE_TYPES = {"png", "jpg", "jpeg", "webp", "tif", "tiff", "bmp"}
SUPPORTED_TYPES = TEXT_TYPES | OFFICE_TYPES | PDF_TYPES | IMAGE_TYPES
XML_TEXT_NAMES = {"t"}
EXCEL_TABLE_BLOCK_MAX_ROWS = 5


class StructuredDocumentParser:
    """Routes documents to replaceable format-specific parsers."""

    def __init__(self, router: ParserRouter | None = None) -> None:
        self.router = router or ParserRouter()

    def parse(self, request: DocumentParseRequest) -> DocumentParseResponse:
        file_type = request.file_type.lower().strip().lstrip(".")
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
            parser = self.router.select(file_path, file_type)
            result = parser.parse(request, file_path, file_type)
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
            "Parsed document doc_id=%s version_id=%s status=%s pages=%s blocks=%s errors=%s",
            request.doc_id,
            request.version_id,
            result.status,
            len(result.pages),
            len(result.blocks),
            len(result.errors),
        )
        return DocumentParseResponse(**result.model_dump())


class ParserRouter:
    """Selects the cheapest parser that can preserve useful structure."""

    def __init__(
        self,
        ocr_provider: OcrProvider | None = None,
        caption_provider: VLMCaptionProvider | None = None,
        enable_ocr: bool = True,
    ) -> None:
        self.ocr_provider: OcrProvider = ocr_provider or MockOcrProvider()
        self.caption_provider: VLMCaptionProvider = caption_provider or MockVLMCaptionProvider()
        self.enable_ocr = enable_ocr

    def select(self, file_path: Path, file_type: str) -> DocumentParser:
        if file_type == "txt":
            return TextParser()
        if file_type == "md":
            return MarkdownParser()
        if file_type == "pdf":
            return PdfMixedParser(self.ocr_provider, self.enable_ocr)
        if file_type == "docx":
            return DocxParser()
        if file_type == "pptx":
            return PptParser(self.caption_provider)
        if file_type == "xlsx":
            return ExcelParser()
        if file_type in IMAGE_TYPES:
            return ImageParser(self.caption_provider)
        raise DocumentParseError("UNSUPPORTED_FILE_TYPE", f"Unsupported file_type '{file_type}'")

class TextParser:

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        return _result_from_single_block(
            request=request,
            parser_name="TextParser",
            page=ParsedPage(
                page_no=1,
                section_title=file_path.name,
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
                section_title=file_path.name,
                confidence=1.0,
                metadata={"file_type": file_type},
            ),
        )


class MarkdownParser:

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        title = _first_markdown_heading(content) or file_path.name
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


class PdfNativeParser:

    @staticmethod
    def has_extractable_text(file_path: Path) -> bool:
        try:
            from pypdf import PdfReader
        except ImportError as exc:
            raise DocumentParseError(
                code="DEPENDENCY_MISSING",
                message="pypdf is required to parse PDF documents",
                status_code=500,
            ) from exc

        try:
            reader = PdfReader(str(file_path))
            return any((page.extract_text() or "").strip() for page in reader.pages)
        except Exception:
            return False

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        from pypdf import PdfReader

        reader = PdfReader(str(file_path))
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        for page_no, page in enumerate(reader.pages, start=1):
            try:
                content = (page.extract_text() or "").strip()
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=file_path.name,
                        content_type="text",
                        content=content,
                        metadata={"file_type": "pdf", "page_index": page_no - 1, "parser": "PdfNativeParser"},
                    )
                )
                if content:
                    blocks.append(
                        _block(
                            request,
                            file_path,
                            page_no=page_no,
                            block_type="paragraph",
                            content=content,
                            section_title=file_path.name,
                            confidence=1.0,
                            metadata={"file_type": "pdf", "parser": "PdfNativeParser"},
                        )
                    )
            except Exception as exc:
                errors.append(_page_error(page_no, exc))
        return _result(request, "PdfNativeParser", pages, blocks, errors)


class PdfMixedParser:
    """Parse each PDF page with native text first, falling back to OCR per page."""

    def __init__(self, ocr_provider: OcrProvider, enable_ocr: bool = True) -> None:
        self.ocr_provider = ocr_provider
        self.enable_ocr = enable_ocr

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
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
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        modes: list[str] = []

        for page_no, page in enumerate(reader.pages, start=1):
            started_at = time.perf_counter()
            try:
                content = (page.extract_text() or "").strip()
            except Exception as exc:
                errors.append(_page_error(page_no, exc, {"page_parse_mode": "corrupt"}, code="PDF_PAGE_EXTRACT_FAILED"))
                modes.append("corrupt")
                continue

            if content:
                modes.append("native_text")
                elapsed_ms = _elapsed_ms(started_at)
                metadata = {
                    "file_type": "pdf",
                    "page_index": page_no - 1,
                    "parser": "PdfNativeParser",
                    "page_parse_mode": "native_text",
                    "elapsed_ms": elapsed_ms,
                }
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=file_path.name,
                        content_type="text",
                        content=content,
                        metadata=metadata,
                    )
                )
                blocks.append(
                    _block(
                        request,
                        file_path,
                        page_no=page_no,
                        block_type="paragraph",
                        content=content,
                        section_title=file_path.name,
                        confidence=1.0,
                        metadata=metadata,
                        block_no=len(blocks) + 1,
                    )
                )
                logger.info(
                    "parse_page doc_id=%s version_id=%s parser=PdfNativeParser page_no=%s status=SUCCESS elapsed_ms=%s",
                    request.doc_id,
                    request.version_id,
                    page_no,
                    elapsed_ms,
                )
                continue

            modes.append("scanned_image")
            image_uri = f"{file_path}#page-{page_no}"
            if not self.enable_ocr:
                errors.append(
                    PageParseError(
                        page_no=page_no,
                        code="OCR_DISABLED",
                        message="OCR is disabled for scanned PDF page",
                        metadata={"file_type": "pdf", "image_uri": image_uri, "page_parse_mode": "scanned_image"},
                    )
                )
                logger.info(
                    "parse_page doc_id=%s version_id=%s parser=OCRParser page_no=%s status=FAILED error_code=OCR_DISABLED",
                    request.doc_id,
                    request.version_id,
                    page_no,
                )
                continue

            try:
                ocr = _normalize_ocr_result(
                    self.ocr_provider.recognize_page(
                        image_uri,
                        page_no,
                        {"file_type": "pdf", "page_parse_mode": "scanned_image"},
                    )
                )
                elapsed_ms = ocr.elapsed_ms if ocr.elapsed_ms is not None else _elapsed_ms(started_at)
                metadata = _ocr_metadata(ocr, "pdf", "scanned_image", elapsed_ms)
                metadata["image_uri"] = image_uri
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=file_path.name,
                        content_type="text",
                        content=ocr.text,
                        metadata=metadata,
                    )
                )
                if ocr.text.strip():
                    blocks.append(
                        _block(
                            request,
                            file_path,
                            page_no=page_no,
                            block_type="paragraph",
                            content=ocr.text,
                            section_title=file_path.name,
                            bbox=ocr.bbox,
                            confidence=ocr.confidence,
                            image_uri=image_uri,
                            metadata=metadata,
                            block_no=len(blocks) + 1,
                        )
                    )
                logger.info(
                    "parse_page doc_id=%s version_id=%s parser=OCRParser page_no=%s status=SUCCESS elapsed_ms=%s",
                    request.doc_id,
                    request.version_id,
                    page_no,
                    elapsed_ms,
                )
            except OcrProviderError as exc:
                errors.append(
                    _page_error(
                        page_no,
                        exc,
                        metadata={"file_type": "pdf", "image_uri": image_uri, "ocr_error_code": exc.code, "page_parse_mode": "scanned_image"},
                        code=exc.code,
                    )
                )
            except Exception as exc:
                errors.append(_page_error(page_no, exc, {"file_type": "pdf", "image_uri": image_uri, "page_parse_mode": "scanned_image"}))

        return _result(request, _pdf_parser_name(modes), pages, blocks, errors, metadata={"page_modes": modes})


class OCRParser:

    def __init__(self, ocr_provider: OcrProvider) -> None:
        self.ocr_provider = ocr_provider

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        page_count = _pdf_page_count(file_path) if file_type == "pdf" else 1
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        for page_no in range(1, page_count + 1):
            try:
                image_uri = f"{file_path}#page-{page_no}" if file_type == "pdf" else str(file_path)
                started_at = time.perf_counter()
                ocr = _normalize_ocr_result(self.ocr_provider.recognize_page(image_uri, page_no, {"file_type": file_type}))
                elapsed_ms = ocr.elapsed_ms if ocr.elapsed_ms is not None else _elapsed_ms(started_at)
                metadata = _ocr_metadata(ocr, file_type, "scanned_image" if file_type == "pdf" else "image", elapsed_ms)
                metadata["image_uri"] = image_uri
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=file_path.name,
                        content_type="text",
                        content=ocr.text,
                        metadata=metadata,
                    )
                )
                blocks.append(
                    _block(
                        request,
                        file_path,
                        page_no=page_no,
                        block_type="paragraph",
                        content=ocr.text,
                        section_title=file_path.name,
                        bbox=ocr.bbox,
                        confidence=ocr.confidence,
                        image_uri=image_uri,
                        metadata=metadata,
                        block_no=len(blocks) + 1,
                    )
                )
                logger.info(
                    "parse_page doc_id=%s version_id=%s parser=OCRParser page_no=%s status=SUCCESS elapsed_ms=%s",
                    request.doc_id,
                    request.version_id,
                    page_no,
                    elapsed_ms,
                )
            except OcrProviderError as exc:
                # OCR capability failure (missing image, model error, ...).
                # Surface the provider's code so callers can tell failure modes
                # apart, instead of recording a generic PAGE_PARSE_FAILED.
                errors.append(
                    _page_error(
                        page_no,
                        exc,
                        metadata={"file_type": file_type, "image_uri": image_uri, "ocr_error_code": exc.code},
                        code=exc.code,
                    )
                )
            except Exception as exc:
                errors.append(_page_error(page_no, exc))
        return _result(request, "OCRParser", pages, blocks, errors)


class DocxParser:

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        with zipfile.ZipFile(file_path) as archive:
            xml_content = archive.read("word/document.xml")
        root = ElementTree.fromstring(xml_content)
        body_element = _first_child_by_local_name(root, "body")
        body = body_element if body_element is not None else root
        headings: list[dict[str, int | str]] = []
        blocks: list[ParsedBlock] = []
        page_lines: list[str] = []
        current_section: str | None = None

        for child in body:
            local_name = _local_name(child.tag)
            if local_name == "p":
                text = "".join(_texts_in_element(child)).strip()
                if not text:
                    continue
                heading_level = _paragraph_heading_level(child)
                if heading_level is not None:
                    current_section = text
                    headings.append({"level": heading_level, "text": text})
                    block_type = "title"
                else:
                    block_type = "list" if _is_list_paragraph(child) else "paragraph"
                page_lines.append(text)
                blocks.append(
                    _block(
                        request,
                        file_path,
                        page_no=1,
                        block_type=block_type,
                        content=text,
                        section_title=current_section or text,
                        confidence=1.0,
                        metadata={"file_type": "docx", "parser": "DocxParser", "heading_level": heading_level},
                        block_no=len(blocks) + 1,
                    )
                )
            elif local_name == "tbl":
                rows = _table_rows(child)
                markdown = _table_markdown(rows)
                content = "\n".join("\t".join(row).rstrip() for row in rows if any(row))
                if not content.strip():
                    continue
                page_lines.append(content)
                blocks.append(
                    _block(
                        request,
                        file_path,
                        page_no=1,
                        block_type="table",
                        content=content,
                        section_title=current_section or file_path.name,
                        markdown=markdown,
                        confidence=1.0,
                        metadata={"file_type": "docx", "parser": "DocxParser", "headers": rows[0] if rows else []},
                        block_no=len(blocks) + 1,
                    )
                )

        content = "\n".join(page_lines)
        title = _first_non_empty_line(content) or file_path.name
        page = ParsedPage(
            page_no=1,
            section_title=title,
            content_type="text",
            content=content,
            metadata={"file_type": "docx", "parser": "DocxParser", "block_count": len(blocks), "headings": headings},
        )
        return _result(request, "DocxParser", [page], blocks, [])


class PptParser:

    def __init__(self, caption_provider: VLMCaptionProvider) -> None:
        self.caption_provider = caption_provider

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        with zipfile.ZipFile(file_path) as archive:
            slide_names = sorted(
                (name for name in archive.namelist() if re.fullmatch(r"ppt/slides/slide\d+\.xml", name)),
                key=_natural_sort_key,
            )
            for page_no, slide_name in enumerate(slide_names, start=1):
                try:
                    root = ElementTree.fromstring(archive.read(slide_name))
                    lines = _group_texts_by_paragraph(root)
                    content = "\n".join(line for line in lines if line)
                    title = _first_non_empty_line(content) or f"Slide {page_no}"
                    image_uri = f"{file_path}#slide-{page_no}"
                    caption = (
                        _normalize_caption_result(self.caption_provider.caption(image_uri, {"file_type": "pptx", "page_no": page_no}))
                        if not content.strip()
                        else VLMCaptionResult(caption="", provider=None)
                    )
                    summary = _first_non_empty_line(content) or caption.caption
                    figure_content = content if content.strip() else caption.caption
                    caption_metadata = _caption_metadata(caption) if caption.provider or caption.error_code else {}
                    pages.append(
                        ParsedPage(
                            page_no=page_no,
                            section_title=title,
                            content_type="text",
                            content=figure_content,
                            metadata={
                                "file_type": "pptx",
                                "parser": "PptParser",
                                "slide_path": slide_name,
                                "slide_page": page_no,
                                "has_tables": _has_xml_local_name(root, "tbl"),
                                "slide_image": image_uri,
                                "slide_summary": summary,
                                **caption_metadata,
                            },
                        )
                    )
                    blocks.append(
                        _block(
                            request,
                            file_path,
                            page_no=page_no,
                            block_type="figure",
                            content=figure_content,
                            section_title=title,
                            confidence=caption.confidence if not content.strip() else 0.95,
                            image_uri=image_uri,
                            metadata={
                                "file_type": "pptx",
                                "slide_text": content,
                                "slide_image": image_uri,
                                "slide_summary": summary,
                                **caption_metadata,
                            },
                            block_no=len(blocks) + 1,
                        )
                    )
                    for rows in _tables_in_element(root):
                        table_content = "\n".join("\t".join(row).rstrip() for row in rows if any(row))
                        if not table_content.strip():
                            continue
                        blocks.append(
                            _block(
                                request,
                                file_path,
                                page_no=page_no,
                                block_type="table",
                                content=table_content,
                                section_title=title,
                                markdown=_table_markdown(rows),
                                confidence=1.0,
                                metadata={
                                    "file_type": "pptx",
                                    "parser": "PptParser",
                                    "headers": rows[0] if rows else [],
                                    "slide_image": image_uri,
                                },
                                block_no=len(blocks) + 1,
                            )
                        )
                except Exception as exc:
                    errors.append(_page_error(page_no, exc, {"slide_path": slide_name}))
        return _result(request, "PptParser", pages, blocks, errors)


class ExcelParser:

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        with zipfile.ZipFile(file_path) as archive:
            shared_strings = _read_shared_strings(archive)
            sheet_titles = _read_workbook_sheet_titles(archive)
            sheet_names = sorted(
                (name for name in archive.namelist() if re.fullmatch(r"xl/worksheets/sheet\d+\.xml", name)),
                key=_natural_sort_key,
            )
            for page_no, sheet_name in enumerate(sheet_names, start=1):
                try:
                    xml_content = archive.read(sheet_name)
                    rows = _read_sheet_rows(xml_content, shared_strings)
                    formulas = _read_sheet_formulas(xml_content)
                    content = "\n".join("\t".join(row).rstrip() for row in rows if any(row))
                    root = ElementTree.fromstring(xml_content)
                    table_region = _sheet_dimension(root)
                    headers = next((row for row in rows if any(row)), [])
                    merged_cells = _merged_cells(root)
                    sheet_title = sheet_titles.get(sheet_name, Path(sheet_name).stem)
                    metadata = {
                        "file_type": "xlsx",
                        "parser": "ExcelParser",
                        "sheet_name": sheet_title,
                        "table_region": table_region,
                        "headers": headers,
                        "merged_cells": merged_cells,
                        "formulas": formulas,
                        "row_count": len(rows),
                    }
                    pages.append(
                        ParsedPage(
                            page_no=page_no,
                            section_title=sheet_title,
                            content_type="table",
                            content=content,
                            metadata=metadata,
                        )
                    )
                    for table_index, table_rows in enumerate(_split_table_rows(rows, EXCEL_TABLE_BLOCK_MAX_ROWS), start=1):
                        if not any(any(cell for cell in row) for row in table_rows):
                            continue
                        block_metadata = {
                            **metadata,
                            "table_block_index": table_index,
                            "table_block_count": len(_split_table_rows(rows, EXCEL_TABLE_BLOCK_MAX_ROWS)),
                        }
                        blocks.append(
                            _block(
                                request,
                                file_path,
                                page_no=page_no,
                                block_type="table",
                                content="\n".join("\t".join(row).rstrip() for row in table_rows if any(row)),
                                section_title=sheet_title,
                                markdown=_table_markdown(table_rows),
                                html=None,
                                confidence=1.0,
                                metadata=block_metadata,
                                block_no=len(blocks) + 1,
                            )
                        )
                except Exception as exc:
                    errors.append(_page_error(page_no, exc, {"sheet_path": sheet_name}))
        return _result(request, "ExcelParser", pages, blocks, errors)


class ImageParser:

    def __init__(self, caption_provider: VLMCaptionProvider) -> None:
        self.caption_provider = caption_provider

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        started_at = time.perf_counter()
        caption = _normalize_caption_result(self.caption_provider.caption(str(file_path), {"file_type": file_type}))
        elapsed_ms = caption.elapsed_ms if caption.elapsed_ms is not None else _elapsed_ms(started_at)
        caption_metadata = _caption_metadata(caption)
        if elapsed_ms is not None:
            caption_metadata["elapsed_ms"] = elapsed_ms
        page = ParsedPage(
            page_no=1,
            section_title=file_path.name,
            content_type="image",
            content=caption.caption,
            metadata={"file_type": file_type, "parser": "ImageParser", "image_uri": str(file_path), **caption_metadata},
        )
        block = _block(
            request,
            file_path,
            page_no=1,
            block_type="figure",
            content=caption.caption,
            section_title=file_path.name,
            confidence=caption.confidence,
            image_uri=str(file_path),
            metadata={"file_type": file_type, "parser": "ImageParser", **caption_metadata},
            block_no=1,
        )
        errors = []
        if caption.error_code:
            errors.append(
                PageParseError(
                    page_no=1,
                    code=caption.error_code,
                    message=caption.error_message or "Image caption failed",
                    metadata={"file_type": file_type, "image_uri": str(file_path), **caption_metadata},
                )
            )
        return _result(request, "ImageParser", [page], [block], errors)


class MinerUParser:
    """Reserved adapter for future complex PDF layout parsing."""


class DoclingParser:
    """Reserved adapter for future complex document parsing."""


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
        source_uri=str(file_path),
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


def _pdf_parser_name(modes: list[str]) -> str:
    meaningful = {mode for mode in modes if mode != "corrupt"}
    if meaningful == {"native_text"}:
        return "PdfNativeParser"
    if meaningful == {"scanned_image"}:
        return "OCRParser"
    if not meaningful and "corrupt" in modes:
        return "PdfMixedParser"
    return "PdfMixedParser"


def _pdf_page_count(file_path: Path) -> int:
    try:
        from pypdf import PdfReader

        return max(len(PdfReader(str(file_path)).pages), 1)
    except Exception:
        return 1


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

    # Some OOXML fragments nest rows/cells deeper than direct children.
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
