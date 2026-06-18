from __future__ import annotations

from typing import Any, Literal

from pydantic import AliasChoices, BaseModel, ConfigDict, Field


class DocumentParseRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    doc_id: str = Field(
        min_length=1,
        validation_alias=AliasChoices("doc_id", "document_id", "documentId"),
    )
    version_id: str = Field(
        default="1",
        min_length=1,
        validation_alias=AliasChoices("version_id", "versionId"),
    )
    file_path: str = Field(
        min_length=1,
        validation_alias=AliasChoices("file_path", "storage_uri", "filePath", "storageUri"),
    )
    file_type: str = Field(
        min_length=1,
        validation_alias=AliasChoices("file_type", "fileType"),
    )


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
