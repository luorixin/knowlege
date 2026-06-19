import time
from pathlib import Path

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResult,
    PageParseError,
    ParsedPage,
)
from app.services.parser.base import VLMCaptionProvider
from app.services.parser.errors import DocumentParseError
from app.services.parser.normalization import (
    _block,
    _caption_metadata,
    _elapsed_ms,
    _normalize_caption_result,
    _result,
)


from app.config import get_settings


class ImageParser:
    def __init__(self, caption_provider: VLMCaptionProvider) -> None:
        self.caption_provider = caption_provider

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        settings = get_settings()
        vlm_fail_mode = settings.vlm_fail_mode
        started_at = time.perf_counter()
        
        local_image_uri = str(file_path)
        desensitized_image_uri = f"doc:{request.doc_id}/version:{request.version_id}/image:1"

        caption = _normalize_caption_result(self.caption_provider.caption(local_image_uri, {"file_type": file_type}))
        elapsed_ms = caption.elapsed_ms if caption.elapsed_ms is not None else _elapsed_ms(started_at)
        caption_metadata = _caption_metadata(caption)
        if elapsed_ms is not None:
            caption_metadata["elapsed_ms"] = elapsed_ms
        page = ParsedPage(
            page_no=1,
            section_title=request.doc_id,
            content_type="image",
            content=caption.caption,
            metadata={"file_type": file_type, "parser": "ImageParser", "image_uri": desensitized_image_uri, **caption_metadata},
        )
        block = _block(
            request,
            file_path,
            page_no=1,
            block_type="figure",
            content=caption.caption,
            section_title=request.doc_id,
            confidence=caption.confidence,
            image_uri=desensitized_image_uri,
            metadata={"file_type": file_type, "parser": "ImageParser", **caption_metadata},
            block_no=1,
        )
        errors = []
        if caption.error_code:
            if vlm_fail_mode == "fail":
                raise DocumentParseError(
                    code="VLM_FAILED",
                    message=f"Image caption failed for {desensitized_image_uri}: {caption.error_message}",
                    status_code=500,
                )
            if vlm_fail_mode != "ignore":
                errors.append(
                    PageParseError(
                        page_no=1,
                        code=caption.error_code,
                        message=caption.error_message or "Image caption failed",
                        metadata={"file_type": file_type, "image_uri": desensitized_image_uri, **caption_metadata},
                    )
                )
        return _result(request, "ImageParser", [page], [block], errors)
