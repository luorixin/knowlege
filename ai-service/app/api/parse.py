from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.schemas.document_parse import DocumentParseRequest, DocumentParseResponse
from app.services.parser.errors import DocumentParseError
from app.services.parser.factory import build_structured_document_parser

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/parse", tags=["document-parse"])

parser = build_structured_document_parser()


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
