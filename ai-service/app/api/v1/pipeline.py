from fastapi import APIRouter

from app.config import get_settings
from app.schemas.document_parse import DocumentParseRequest
from app.schemas.pipeline import (
    AnswerRequest,
    AnswerResponse,
    ChunkRequest,
    ChunkResponse,
    EmbeddingRequest,
    EmbeddingResponse,
    ParseAndChunkRequest,
    ParseAndChunkResponse,
    ParseRequest,
    ParseResponse,
    RerankRequest,
    RerankResponse,
)
from app.services.chunker.default import DefaultChunker
from app.services.embedding.mock import MockEmbeddingProvider
from app.services.embedding.openai import OpenAICompatibleHTTPProvider
from app.services.llm.mock import MockLlmClient
from app.services.llm.openai import OpenAICompatibleLLMClient
from app.services.parser.factory import build_structured_document_parser
from app.services.rerank.http import HttpReranker
from app.services.rerank.mock import MockReranker

router = APIRouter(tags=["ai-pipeline"])


def _build_structured_parser():
    return build_structured_document_parser()


def _build_chunker():
    return DefaultChunker()


def _build_embedding_provider():
    settings = get_settings()
    if settings.embedding_provider in {"openai-compatible", "openai", "qwen", "private-http"}:
        return OpenAICompatibleHTTPProvider()
    return MockEmbeddingProvider()


def _build_reranker():
    settings = get_settings()
    if settings.rerank_provider in {"http", "private-http", "openai-compatible", "qwen"}:
        return HttpReranker()
    return MockReranker()


def _build_llm_client():
    settings = get_settings()
    if settings.llm_provider in {"openai-compatible", "openai", "qwen", "private-http"}:
        return OpenAICompatibleLLMClient()
    return MockLlmClient()


chunker = _build_chunker()
structured_parser = _build_structured_parser()


@router.post("/pipeline/parse-and-chunk", response_model=ParseAndChunkResponse)
def parse_and_chunk(request: ParseAndChunkRequest) -> ParseAndChunkResponse:
    # 1. Parse document into pages
    parse_request = DocumentParseRequest(
        doc_id=request.document_id,
        version_id="1",
        file_type=request.file_type,
        file_path=request.storage_uri,
    )
    parse_response = structured_parser.parse(parse_request)

    # 2. Chunk each page
    items = []
    chunk_no = 1
    for page in parse_response.pages:
        if not page.content.strip():
            continue
        text_chunks = chunker._chunk_text(page.content, request.chunk_size, request.overlap)
        for text in text_chunks:
            items.append(
                chunker._build_chunk_item(
                    doc_id=request.document_id,
                    chunk_no=chunk_no,
                    text=text,
                    page_no=page.page_no,
                    section_title=page.section_title,
                    metadata={"strategy": request.strategy, **page.metadata},
                )
            )
            chunk_no += 1

    return ParseAndChunkResponse(document_id=request.document_id, chunks=items)


@router.post("/documents/parse", response_model=ParseResponse)
def parse_document(request: ParseRequest) -> ParseResponse:
    parse_response = structured_parser.parse(
        DocumentParseRequest(
            doc_id=request.document_id,
            version_id="1",
            file_type=request.file_type,
            file_path=request.storage_uri,
        )
    )
    text = "\n\n".join(page.content for page in parse_response.pages if page.content.strip())
    return ParseResponse(
        document_id=request.document_id,
        status=parse_response.status,
        text=text or parse_response.markdown or "",
    )


@router.post("/chunks/split", response_model=ChunkResponse)
def split_chunks(request: ChunkRequest) -> ChunkResponse:
    return chunker.split(request)


@router.post("/embeddings", response_model=EmbeddingResponse)
def embed_texts(request: EmbeddingRequest) -> EmbeddingResponse:
    return _build_embedding_provider().embed(request)


@router.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest) -> RerankResponse:
    return _build_reranker().rerank(request)


@router.post("/llm/answer", response_model=AnswerResponse)
def answer(request: AnswerRequest) -> AnswerResponse:
    return _build_llm_client().answer(request)
