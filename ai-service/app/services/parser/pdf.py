import logging
import time
from pathlib import Path

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResult,
    PageParseError,
    ParsedBlock,
    ParsedPage,
)
from app.services.parser.base import OcrProvider
from app.services.parser.errors import DocumentParseError, OcrProviderError
from app.services.parser.normalization import (
    _block,
    _elapsed_ms,
    _normalize_ocr_result,
    _ocr_metadata,
    _page_error,
    _result,
)
from app.config import get_settings

logger = logging.getLogger(__name__)


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


class PdfNativeParser:
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
                        section_title=request.doc_id,
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
                            section_title=request.doc_id,
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
        settings = get_settings()
        max_ocr_pages = settings.parser_max_ocr_pages
        ocr_fail_mode = settings.ocr_fail_mode
        ocr_count = 0

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
                        section_title=request.doc_id,
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
                        section_title=request.doc_id,
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
            image_uri = f"doc:{request.doc_id}/version:{request.version_id}/page:{page_no}"
            
            # The actual file to pass to OCR might need to be file_path, but OCR provider logic 
            # uses image_uri as the source. We must check how OCR provider uses it.
            # In base.py/mock: it might ignore. If it's a real OCR provider, we should pass file_path 
            # or the original image_uri string. Let's see how `ocr_provider.recognize_page` was called.
            # Previously: `image_uri = f"{file_path}#page-{page_no}"`
            # Since we must desensitize, if OCR provider needs the local path, we must pass it differently. 
            # Actually, `recognize_page(image_uri, page_no)` might be passing it as an identifier or downloading it.
            # If `ocr_provider` is mock, it ignores. If it's an API, it uploads the file.
            # Wait, `ocr_provider.recognize_page` currently takes `image_uri`. But the physical path is `file_path`.
            # If we change `image_uri` passed to `recognize_page`, it might fail to find the local file.
            # The plan says: "在调用各个 parser 提取或构造 image_uri 及传入 _block 的时候，改为通过 request.doc_id 和 request.version_id 组装内部统一标识（UIR / source_ref）。"
            # It doesn't explicitly mention changing the first argument to `recognize_page`. I will keep the original
            # `file_path_str` for the OCR provider (which needs it to read the file), but use `image_uri` for metadata/blocks!
            
            local_image_uri = f"{file_path}#page-{page_no}"
            desensitized_image_uri = image_uri

            if not self.enable_ocr:
                errors.append(
                    PageParseError(
                        page_no=page_no,
                        code="OCR_DISABLED",
                        message="OCR is disabled for scanned PDF page",
                        metadata={"file_type": "pdf", "image_uri": desensitized_image_uri, "page_parse_mode": "scanned_image"},
                    )
                )
                logger.info(
                    "parse_page doc_id=%s version_id=%s parser=OCRParser page_no=%s status=FAILED error_code=OCR_DISABLED",
                    request.doc_id,
                    request.version_id,
                    page_no,
                )
                continue

            if ocr_count >= max_ocr_pages:
                errors.append(
                    PageParseError(
                        page_no=page_no,
                        code="OCR_LIMIT_EXCEEDED",
                        message=f"OCR limit of {max_ocr_pages} pages exceeded",
                        metadata={"file_type": "pdf", "image_uri": desensitized_image_uri, "page_parse_mode": "scanned_image"},
                    )
                )
                logger.warning(
                    "parse_page doc_id=%s version_id=%s parser=OCRParser page_no=%s status=FAILED error_code=OCR_LIMIT_EXCEEDED",
                    request.doc_id,
                    request.version_id,
                    page_no,
                )
                continue

            try:
                ocr_count += 1
                ocr = _normalize_ocr_result(
                    self.ocr_provider.recognize_page(
                        local_image_uri,
                        page_no,
                        {"file_type": "pdf", "page_parse_mode": "scanned_image"},
                    )
                )
                elapsed_ms = ocr.elapsed_ms if ocr.elapsed_ms is not None else _elapsed_ms(started_at)
                metadata = _ocr_metadata(ocr, "pdf", "scanned_image", elapsed_ms)
                metadata["image_uri"] = desensitized_image_uri
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=request.doc_id,
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
                            section_title=request.doc_id,
                            bbox=ocr.bbox,
                            confidence=ocr.confidence,
                            image_uri=desensitized_image_uri,
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
                if ocr_fail_mode == "fail":
                    raise DocumentParseError(
                        code="OCR_FAILED",
                        message=f"OCR failed for {desensitized_image_uri}: {exc.message}",
                        status_code=500,
                    ) from exc
                errors.append(
                    _page_error(
                        page_no,
                        exc,
                        metadata={"file_type": "pdf", "image_uri": desensitized_image_uri, "ocr_error_code": exc.code, "page_parse_mode": "scanned_image"},
                        code=exc.code,
                    )
                )
            except Exception as exc:
                errors.append(_page_error(page_no, exc, {"file_type": "pdf", "image_uri": desensitized_image_uri, "page_parse_mode": "scanned_image"}))

        return _result(request, _pdf_parser_name(modes), pages, blocks, errors, metadata={"page_modes": modes})


class OCRParser:
    def __init__(self, ocr_provider: OcrProvider) -> None:
        self.ocr_provider = ocr_provider

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        page_count = _pdf_page_count(file_path) if file_type == "pdf" else 1
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        settings = get_settings()
        max_ocr_pages = settings.parser_max_ocr_pages
        ocr_fail_mode = settings.ocr_fail_mode
        ocr_count = 0
        for page_no in range(1, page_count + 1):
            try:
                local_image_uri = f"{file_path}#page-{page_no}" if file_type == "pdf" else str(file_path)
                desensitized_image_uri = f"doc:{request.doc_id}/version:{request.version_id}/page:{page_no}" if file_type == "pdf" else f"doc:{request.doc_id}/version:{request.version_id}/image:1"
                
                if ocr_count >= max_ocr_pages:
                    errors.append(
                        PageParseError(
                            page_no=page_no,
                            code="OCR_LIMIT_EXCEEDED",
                            message=f"OCR limit of {max_ocr_pages} pages exceeded",
                            metadata={"file_type": file_type, "image_uri": desensitized_image_uri},
                        )
                    )
                    logger.warning(
                        "parse_page doc_id=%s version_id=%s parser=OCRParser page_no=%s status=FAILED error_code=OCR_LIMIT_EXCEEDED",
                        request.doc_id,
                        request.version_id,
                        page_no,
                    )
                    continue

                started_at = time.perf_counter()
                ocr_count += 1
                ocr = _normalize_ocr_result(self.ocr_provider.recognize_page(local_image_uri, page_no, {"file_type": file_type}))
                elapsed_ms = ocr.elapsed_ms if ocr.elapsed_ms is not None else _elapsed_ms(started_at)
                metadata = _ocr_metadata(ocr, file_type, "scanned_image" if file_type == "pdf" else "image", elapsed_ms)
                metadata["image_uri"] = desensitized_image_uri
                pages.append(
                    ParsedPage(
                        page_no=page_no,
                        section_title=request.doc_id,
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
                        section_title=request.doc_id,
                        bbox=ocr.bbox,
                        confidence=ocr.confidence,
                        image_uri=desensitized_image_uri,
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
                if ocr_fail_mode == "fail":
                    raise DocumentParseError(
                        code="OCR_FAILED",
                        message=f"OCR failed for {desensitized_image_uri}: {exc.message}",
                        status_code=500,
                    ) from exc
                errors.append(
                    _page_error(
                        page_no,
                        exc,
                        metadata={"file_type": file_type, "image_uri": desensitized_image_uri, "ocr_error_code": exc.code},
                        code=exc.code,
                    )
                )
            except Exception as exc:
                errors.append(_page_error(page_no, exc))
        return _result(request, "OCRParser", pages, blocks, errors)
