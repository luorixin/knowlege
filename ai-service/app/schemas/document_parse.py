from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class DocumentParseRequest(BaseModel):
    doc_id: str = Field(min_length=1)
    version_id: str = Field(min_length=1)
    file_path: str = Field(min_length=1)
    file_type: str = Field(min_length=1)


class ParsedPage(BaseModel):
    page_no: int = Field(ge=1)
    section_title: str | None = None
    content_type: Literal["text", "table"]
    content: str
    metadata: dict[str, Any] = Field(default_factory=dict)


class DocumentParseResponse(BaseModel):
    doc_id: str
    version_id: str
    pages: list[ParsedPage]
