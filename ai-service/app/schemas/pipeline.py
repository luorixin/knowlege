from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class ParseRequest(BaseModel):
    document_id: str
    storage_uri: str
    file_type: str


class ParseResponse(BaseModel):
    document_id: str
    status: str = "mocked"
    text: str


class ChunkRequest(BaseModel):
    document_id: str
    text: str
    chunk_size: int = Field(default=800, ge=100)
    overlap: int = Field(default=100, ge=0)


class ChunkItem(BaseModel):
    chunk_id: str
    chunk_no: int
    text: str
    page_no: int | None = None
    section_title: str | None = None
    content_hash: str | None = None
    char_count: int | None = None
    token_count: int | None = None
    metadata: dict = Field(default_factory=dict)


class ChunkResponse(BaseModel):
    document_id: str
    chunks: list[ChunkItem]


class ParseAndChunkRequest(BaseModel):
    document_id: str
    storage_uri: str
    file_type: str
    chunk_size: int = Field(default=800, ge=100)
    overlap: int = Field(default=100, ge=0)
    strategy: str = "default"
    ocr_enabled: bool = False


class ParseAndChunkResponse(BaseModel):
    document_id: str
    chunks: list[ChunkItem]


class EmbeddingRequest(BaseModel):
    texts: list[str]
    model: str = "mock-embedding-v1"
    dimensions: int | None = None


class EmbeddingItem(BaseModel):
    index: int
    vector: list[float]


class EmbeddingResponse(BaseModel):
    model: str
    dimension: int
    embeddings: list[EmbeddingItem]
    usage: dict[str, Any] | None = None
    provider: str | None = None


class RerankDocument(BaseModel):
    chunk_id: str
    text: str
    score: float = 0.0
    metadata: dict[str, Any] = Field(default_factory=dict)


class RerankRequest(BaseModel):
    query: str
    documents: list[RerankDocument]
    model: str | None = None
    top_k: int | None = Field(default=None, ge=1)


class RerankResponse(BaseModel):
    query: str
    documents: list[RerankDocument]
    model: str | None = None
    provider: str | None = None


class AnswerRequest(BaseModel):
    question: str
    contexts: list[RerankDocument]
    model: str | None = None
    system_prompt: str | None = None
    temperature: float | None = Field(default=None, ge=0.0, le=2.0)
    max_tokens: int | None = Field(default=None, ge=1)


class AnswerResponse(BaseModel):
    answer: str
    citations: list[str]
    model: str | None = None
    provider: str | None = None
    usage: dict[str, Any] | None = None
