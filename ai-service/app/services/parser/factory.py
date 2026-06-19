import logging

from app.config import get_settings
from app.services.parser.base import OcrProvider, VLMCaptionProvider
from app.services.parser.router import ParserRouter, StructuredDocumentParser

logger = logging.getLogger(__name__)


def build_parser_router() -> ParserRouter:
    settings = get_settings()
    return ParserRouter(
        ocr_provider=_build_ocr_provider(settings.ocr_provider),
        caption_provider=_build_vlm_provider(settings.vlm_provider),
        enable_ocr=settings.parser_enable_ocr,
    )


def build_structured_document_parser() -> StructuredDocumentParser:
    return StructuredDocumentParser(router=build_parser_router())


def _build_ocr_provider(provider: str) -> OcrProvider | None:
    if provider != "paddle":
        return None
    try:
        from app.services.parser.ocr_paddle import PaddleOcrProvider

        return PaddleOcrProvider()
    except Exception as exc:
        logger.error("Failed to initialize PaddleOCR provider: %s", exc)
        return None


def _build_vlm_provider(provider: str) -> VLMCaptionProvider | None:
    if provider not in {"openai", "openai-compatible", "qwen"}:
        return None
    try:
        from app.services.parser.vlm_openai import OpenAIVLMCaptionProvider

        return OpenAIVLMCaptionProvider()
    except Exception as exc:
        logger.error("Failed to initialize OpenAI-compatible VLM provider: %s", exc)
        return None
