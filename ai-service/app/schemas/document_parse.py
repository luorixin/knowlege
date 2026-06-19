from __future__ import annotations

from typing import Any, Literal

from pydantic import AliasChoices, BaseModel, ConfigDict, Field


class DocumentParseRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    doc_id: str = Field(min_length=1, validation_alias=AliasChoices("doc_id", "document_id"))
    version_id: str = Field(default="1", min_length=1, validation_alias=AliasChoices("version_id", "document_version_id"))
    file_path: str = Field(min_length=1, validation_alias=AliasChoices("file_path", "storage_uri"))
    file_type: str = Field(min_length=1)


class ParsedPage(BaseModel):
    page_no: int = Field(ge=1)
    section_title: str | None = None
    content_type: Literal["text", "table", "image"]
    content: str
    metadata: dict[str, Any] = Field(default_factory=dict)


class ParsedBlock(BaseModel):
    block_id: str
    block_type: Literal["title", "paragraph", "table", "figure", "formula", "list", "header", "footer"]
    page_no: int = Field(ge=1)
    section_title: str | None = None
    content: str
    markdown: str | None = None
    html: str | None = None
    bbox: list[float] | None = None
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    image_uri: str | None = None
    source_uri: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class PageParseError(BaseModel):
    page_no: int | None = Field(default=None, ge=1)
    code: str
    message: str
    metadata: dict[str, Any] = Field(default_factory=dict)


ParseStatus = Literal["PENDING", "RUNNING", "SUCCESS", "FAILED", "PARTIAL_SUCCESS"]


class DocumentParseResult(BaseModel):
    doc_id: str
    version_id: str
    status: ParseStatus = "SUCCESS"
    pages: list[ParsedPage]
    blocks: list[ParsedBlock] = Field(default_factory=list)
    errors: list[PageParseError] = Field(default_factory=list)
    markdown: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class DocumentParseResponse(DocumentParseResult):
    """Backward-compatible response name used by the existing API route."""
