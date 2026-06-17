from fastapi import APIRouter

from app.schemas.pipeline import (
    AnswerRequest,
    AnswerResponse,
    ChunkRequest,
    ChunkResponse,
    EmbeddingRequest,
    EmbeddingResponse,
    ParseRequest,
    ParseResponse,
    RerankRequest,
    RerankResponse,
)
from app.services.chunker.mock import MockChunker
from app.services.embedding.mock import MockEmbeddingProvider
from app.services.llm.mock import MockLlmClient
from app.services.parser.mock import MockDocumentParser
from app.services.rerank.mock import MockReranker

router = APIRouter(tags=["ai-pipeline"])

parser = MockDocumentParser()
chunker = MockChunker()
embedding_provider = MockEmbeddingProvider()
reranker = MockReranker()
llm_client = MockLlmClient()


@router.post("/documents/parse", response_model=ParseResponse)
def parse_document(request: ParseRequest) -> ParseResponse:
    return parser.parse(request)


@router.post("/chunks/split", response_model=ChunkResponse)
def split_chunks(request: ChunkRequest) -> ChunkResponse:
    return chunker.split(request)


@router.post("/embeddings", response_model=EmbeddingResponse)
def embed_texts(request: EmbeddingRequest) -> EmbeddingResponse:
    return embedding_provider.embed(request)


@router.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest) -> RerankResponse:
    return reranker.rerank(request)


@router.post("/llm/answer", response_model=AnswerResponse)
def answer(request: AnswerRequest) -> AnswerResponse:
    return llm_client.answer(request)
