from __future__ import annotations

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


class ChunkResponse(BaseModel):
    document_id: str
    chunks: list[ChunkItem]


class EmbeddingRequest(BaseModel):
    texts: list[str]
    model: str = "mock-embedding"


class EmbeddingItem(BaseModel):
    index: int
    vector: list[float]


class EmbeddingResponse(BaseModel):
    model: str
    dimension: int
    embeddings: list[EmbeddingItem]


class RerankDocument(BaseModel):
    chunk_id: str
    text: str
    score: float = 0.0


class RerankRequest(BaseModel):
    query: str
    documents: list[RerankDocument]


class RerankResponse(BaseModel):
    query: str
    documents: list[RerankDocument]


class AnswerRequest(BaseModel):
    question: str
    contexts: list[RerankDocument]


class AnswerResponse(BaseModel):
    answer: str
    citations: list[str]
