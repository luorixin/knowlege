from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.schemas.document_parse import DocumentParseRequest, DocumentParseResponse
from app.services.parser.errors import DocumentParseError
from app.services.parser.structured import StructuredDocumentParser, ParserRouter
from app.config import get_settings

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/parse", tags=["document-parse"])

def get_parser():
    settings = get_settings()

    # 1. Setup OCR Provider
    ocr_provider = None
    if settings.ocr_provider == "paddle":
        from app.services.parser.ocr_paddle import PaddleOcrProvider
        try:
            ocr_provider = PaddleOcrProvider()
        except Exception as e:
            logger.error(f"Failed to initialize PaddleOCR: {e}")

    # 2. Setup VLM Caption Provider
    vlm_provider = None
    if settings.vlm_provider == "openai":
        from app.services.parser.vlm_openai import OpenAIVLMCaptionProvider
        try:
            vlm_provider = OpenAIVLMCaptionProvider()
        except Exception as e:
            logger.error(f"Failed to initialize OpenAI VLM: {e}")

    # ParserRouter uses Mock components if None is passed
    parser_router = ParserRouter(
        ocr_provider=ocr_provider,
        caption_provider=vlm_provider,
        enable_ocr=settings.parser_enable_ocr,
    )
    return StructuredDocumentParser(router=parser_router)

parser = get_parser()


@router.post("/document", response_model=DocumentParseResponse)
def parse_document(request: DocumentParseRequest) -> DocumentParseResponse:
    try:
        return parser.parse(request)
    except DocumentParseError as exc:
        logger.warning(
            "Document parse request failed code=%s doc_id=%s version_id=%s file_type=%s file_path=%s",
            exc.code,
            request.doc_id,
            request.version_id,
            request.file_type,
            request.file_path,
        )
        raise HTTPException(
            status_code=exc.status_code,
            detail={
                "code": exc.code,
                "message": exc.message,
                "doc_id": request.doc_id,
                "version_id": request.version_id,
            },
        ) from exc
    except Exception as exc:
        logger.exception("Unexpected document parse failure")
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INTERNAL_ERROR",
                "message": "Unexpected document parse failure",
                "doc_id": request.doc_id,
                "version_id": request.version_id,
            },
        ) from exc
