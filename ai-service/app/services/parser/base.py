from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Protocol

from app.schemas.document_parse import DocumentParseRequest, DocumentParseResult


@dataclass(frozen=True)
class OcrResult:
    text: str
    bbox: list[float] | None = None
    confidence: float | None = None
    language: str | None = None
    provider: str | None = None
    elapsed_ms: int | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class VLMCaptionResult:
    caption: str
    provider: str | None = None
    model_name: str | None = None
    confidence: float | None = None
    elapsed_ms: int | None = None
    token_count: int | None = None
    error_code: str | None = None
    error_message: str | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


class DocumentParser(Protocol):

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        """Parse a stored document into normalized structured blocks."""


class OcrProvider(Protocol):

    def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> str | OcrResult:
        """Recognize text from a page image or image-like PDF page."""


class VLMCaptionProvider(Protocol):

    def caption(self, image_uri: str, metadata: dict[str, Any] | None = None) -> str | VLMCaptionResult:
        """Describe an image, screenshot, chart, diagram, or rendered slide."""


class LayoutAnalyzer(Protocol):

    def analyze(self, image_uri: str, metadata: dict[str, Any] | None = None) -> list[dict[str, Any]]:
        """Return layout regions for future OCR/table/formula recognition."""


class TableRecognizer(Protocol):

    def recognize(self, image_uri: str, metadata: dict[str, Any] | None = None) -> str:
        """Return markdown or structured table output from an image region."""


class FormulaRecognizer(Protocol):

    def recognize(self, image_uri: str, metadata: dict[str, Any] | None = None) -> str:
        """Return formula text/LaTeX from an image region."""


class MockOcrProvider:

    def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> OcrResult:
        return OcrResult(
            text=f"Mock OCR text from {image_uri} page {page_no}",
            confidence=0.75,
            provider="mock",
            metadata={"mock": True},
        )


class MockVLMCaptionProvider:

    def caption(self, image_uri: str, metadata: dict[str, Any] | None = None) -> VLMCaptionResult:
        return VLMCaptionResult(
            caption=f"Mock image caption for {image_uri}",
            provider="mock",
            confidence=0.8,
            metadata={"mock": True},
        )


class MockLayoutAnalyzer:

    def analyze(self, image_uri: str, metadata: dict[str, Any] | None = None) -> list[dict[str, Any]]:
        return []


class MockTableRecognizer:

    def recognize(self, image_uri: str, metadata: dict[str, Any] | None = None) -> str:
        return ""


class MockFormulaRecognizer:

    def recognize(self, image_uri: str, metadata: dict[str, Any] | None = None) -> str:
        return ""
