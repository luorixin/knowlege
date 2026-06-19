import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResult,
    PageParseError,
    ParsedBlock,
    ParsedPage,
)
from app.services.parser.base import VLMCaptionProvider, VLMCaptionResult
from app.services.parser.errors import DocumentParseError
from app.services.parser.normalization import (
    _block,
    _caption_metadata,
    _first_non_empty_line,
    _group_texts_by_paragraph,
    _has_xml_local_name,
    _natural_sort_key,
    _normalize_caption_result,
    _page_error,
    _result,
    _table_markdown,
    _tables_in_element,
)
from app.config import get_settings


class PptParser:
    def __init__(self, caption_provider: VLMCaptionProvider) -> None:
        self.caption_provider = caption_provider

    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        settings = get_settings()
        vlm_fail_mode = settings.vlm_fail_mode
        max_vlm_images = settings.parser_max_vlm_images
        vlm_count = 0
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
                    local_image_uri = f"{file_path}#slide-{page_no}"
                    desensitized_image_uri = f"doc:{request.doc_id}/version:{request.version_id}/slide:{page_no}"
                    
                    if not content.strip():
                        if vlm_count >= max_vlm_images:
                            caption = VLMCaptionResult(
                                caption="", provider=None, error_code="VLM_LIMIT_EXCEEDED", error_message=f"VLM limit of {max_vlm_images} images exceeded"
                            )
                        else:
                            vlm_count += 1
                            caption = _normalize_caption_result(self.caption_provider.caption(local_image_uri, {"file_type": "pptx", "page_no": page_no}))
                    else:
                        caption = VLMCaptionResult(caption="", provider=None)
                        
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
                                "slide_image": desensitized_image_uri,
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
                            image_uri=desensitized_image_uri,
                            metadata={
                                "file_type": "pptx",
                                "slide_text": content,
                                "slide_image": desensitized_image_uri,
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
                                    "slide_image": desensitized_image_uri,
                                },
                                block_no=len(blocks) + 1,
                            )
                        )
                    
                    if caption.error_code and caption.error_code != "VLM_LIMIT_EXCEEDED":
                        if vlm_fail_mode == "fail":
                            raise DocumentParseError(
                                code="VLM_FAILED",
                                message=f"VLM caption failed for {desensitized_image_uri}: {caption.error_message}",
                                status_code=500,
                            )
                        if vlm_fail_mode != "ignore":
                            errors.append(
                                PageParseError(
                                    page_no=page_no,
                                    code=caption.error_code,
                                    message=caption.error_message or "VLM caption failed",
                                    metadata={"file_type": "pptx", "image_uri": desensitized_image_uri},
                                )
                            )
                    elif caption.error_code == "VLM_LIMIT_EXCEEDED":
                        errors.append(
                            PageParseError(
                                page_no=page_no,
                                code=caption.error_code,
                                message=caption.error_message,
                                metadata={"file_type": "pptx", "image_uri": desensitized_image_uri},
                            )
                        )
                        
                except Exception as exc:
                    errors.append(_page_error(page_no, exc, {"slide_path": slide_name}))
        return _result(request, "PptParser", pages, blocks, errors)
