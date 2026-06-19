import logging
from pathlib import Path

from app.schemas.document_parse import DocumentParseRequest, DocumentParseResponse
from app.services.parser.base import (
    DocumentParser,
    MockOcrProvider,
    MockVLMCaptionProvider,
    OcrProvider,
    VLMCaptionProvider,
)
from app.services.parser.errors import DocumentParseError
from app.services.parser.image import ImageParser
from app.services.parser.normalization import MarkdownParser, TextParser
from app.services.parser.office_docx import DocxParser
from app.services.parser.office_pptx import PptParser
from app.services.parser.office_xlsx import ExcelParser
from app.services.parser.pdf import PdfMixedParser

logger = logging.getLogger(__name__)

TEXT_TYPES = {"txt", "md"}
OFFICE_TYPES = {"docx", "pptx", "xlsx"}
PDF_TYPES = {"pdf"}
IMAGE_TYPES = {"png", "jpg", "jpeg", "webp", "tif", "tiff", "bmp"}
SUPPORTED_TYPES = TEXT_TYPES | OFFICE_TYPES | PDF_TYPES | IMAGE_TYPES


class MinerUParser:
    """Reserved adapter for future complex PDF layout parsing."""


class DoclingParser:
    """Reserved adapter for future complex document parsing."""


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
