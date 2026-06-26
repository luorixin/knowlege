from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.config import get_settings
from app.main import create_app
from app.schemas.pipeline import AnswerRequest, EmbeddingRequest, RerankDocument, RerankRequest
from app.services.embedding.openai import OpenAICompatibleHTTPProvider
from app.services.llm.openai import OpenAICompatibleLLMClient
from app.services.rerank.http import HttpReranker


@pytest.fixture(autouse=True)
def default_mock_providers(monkeypatch):
    monkeypatch.setenv("EMBEDDING_PROVIDER", "mock")
    monkeypatch.setenv("RERANK_PROVIDER", "mock")
    monkeypatch.setenv("LLM_PROVIDER", "mock")
    monkeypatch.setenv("EMBEDDING_MODEL_NAME", "mock-embedding-v1")
    monkeypatch.setenv("EMBEDDING_DIMENSION", "16")
    monkeypatch.setenv("RERANK_MODEL_NAME", "mock-rerank")
    monkeypatch.setenv("LLM_MODEL_NAME", "mock-llm")
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()


class _FakeResponse:
    def __init__(self, payload: dict) -> None:
        self.payload = payload

    def raise_for_status(self) -> None:
        return None

    def json(self) -> dict:
        return self.payload


class _FakeHttpClient:
    def __init__(self, payload: dict) -> None:
        self.payload = payload
        self.calls: list[dict] = []

    def __enter__(self) -> "_FakeHttpClient":
        return self

    def __exit__(self, *args: object) -> None:
        return None

    def post(self, url: str, headers: dict | None = None, json: dict | None = None) -> _FakeResponse:
        self.calls.append({"url": url, "headers": headers or {}, "json": json or {}})
        return _FakeResponse(self.payload)


def test_pipeline_parse_and_chunk(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.txt"
    file_path.write_text("Hello world.\n" * 100, encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/v1/pipeline/parse-and-chunk",
        json={
            "document_id": "doc-pipeline-1",
            "storage_uri": str(file_path),
            "file_type": "txt",
            "chunk_size": 100,
            "overlap": 20,
            "strategy": "default",
            "ocr_enabled": False
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["document_id"] == "doc-pipeline-1"
    assert len(payload["chunks"]) > 0
    first_chunk = payload["chunks"][0]
    assert first_chunk["chunk_id"] == "doc-pipeline-1-chunk-1"
    assert "char_count" in first_chunk
    assert "token_count" in first_chunk
    assert "content_hash" in first_chunk
    assert "metadata" in first_chunk
    assert first_chunk["metadata"]["strategy"] == "default"


def test_legacy_documents_parse_uses_structured_parser(tmp_path: Path) -> None:
    file_path = tmp_path / "legacy.txt"
    file_path.write_text("真实 legacy parse 内容", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/v1/documents/parse",
        json={
            "document_id": "legacy-doc-1",
            "storage_uri": str(file_path),
            "file_type": "txt",
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["document_id"] == "legacy-doc-1"
    assert payload["status"] == "SUCCESS"
    assert "真实 legacy parse 内容" in payload["text"]
    assert "Mock parsed text" not in payload["text"]


def test_embedding_endpoint_returns_unified_mock_dimension() -> None:
    client = TestClient(create_app())

    response = client.post(
        "/api/v1/embeddings",
        json={
            "texts": ["金融行业数据治理 proposal"],
            "model": "mock-embedding-v1",
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["model"] == "mock-embedding-v1"
    assert payload["dimension"] == 16
    assert payload["provider"] == "mock"
    assert payload["usage"]["total_tokens"] > 0
    assert len(payload["embeddings"]) == 1
    assert len(payload["embeddings"][0]["vector"]) == 16


def test_rerank_endpoint_respects_top_k() -> None:
    client = TestClient(create_app())

    response = client.post(
        "/api/v1/rerank",
        json={
            "query": "金融 proposal",
            "top_k": 1,
            "documents": [
                {"chunk_id": "c1", "text": "教育制度", "score": 0.1, "metadata": {"doc_type": "policy"}},
                {"chunk_id": "c2", "text": "金融数据治理 proposal", "score": 0.9, "metadata": {"doc_type": "proposal"}},
            ],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["provider"] == "mock"
    assert len(payload["documents"]) == 1
    assert payload["documents"][0]["chunk_id"] == "c2"


def test_llm_endpoint_returns_mock_metadata() -> None:
    client = TestClient(create_app())

    response = client.post(
        "/api/v1/llm/answer",
        json={
            "question": "总结常见结构",
            "contexts": [
                {"chunk_id": "c1", "text": "proposal 常见结构包括背景、范围、交付物。", "score": 0.9}
            ],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["provider"] == "mock"
    assert payload["model"] == "mock-llm"
    assert payload["citations"] == ["c1"]
    assert payload["usage"]["total_tokens"] > 0


def test_openai_compatible_embedding_provider_builds_expected_payload(monkeypatch) -> None:
    get_settings.cache_clear()
    monkeypatch.setenv("AI_EMBEDDING_ENDPOINT", "http://model.local/v1")
    monkeypatch.setenv("EMBEDDING_API_KEY", "embedding-key")
    monkeypatch.setenv("EMBEDDING_MODEL_NAME", "text-embedding-test")
    fake_client = _FakeHttpClient(
        {
            "model": "text-embedding-test",
            "data": [{"index": 0, "embedding": [0.1, 0.2]}],
            "usage": {"total_tokens": 5},
        }
    )
    monkeypatch.setattr(
        "app.services.embedding.openai.httpx.Client",
        lambda **kwargs: fake_client,
    )

    provider = OpenAICompatibleHTTPProvider()
    response = provider.embed(EmbeddingRequest(texts=["hello"], model="text-embedding-test", dimensions=2))

    assert response.provider == "openai-compatible"
    assert response.dimension == 2
    assert response.usage == {"total_tokens": 5}
    assert fake_client.calls[0]["url"] == "http://model.local/v1/embeddings"
    assert fake_client.calls[0]["headers"]["Authorization"] == "Bearer embedding-key"
    assert fake_client.calls[0]["json"] == {
        "model": "text-embedding-test",
        "input": ["hello"],
        "dimensions": 2,
    }
    get_settings.cache_clear()


def test_openai_compatible_llm_provider_builds_expected_payload(monkeypatch) -> None:
    get_settings.cache_clear()
    monkeypatch.setenv("AI_LLM_ENDPOINT", "http://llm.local/v1")
    monkeypatch.setenv("LLM_API_KEY", "llm-key")
    monkeypatch.setenv("LLM_MODEL_NAME", "qwen-test")
    fake_client = _FakeHttpClient(
        {
            "model": "qwen-test",
            "choices": [{"message": {"content": "答案来自上下文 [1]"}}],
            "usage": {"total_tokens": 12},
        }
    )
    monkeypatch.setattr("app.services.llm.openai.httpx.Client", lambda **kwargs: fake_client)

    provider = OpenAICompatibleLLMClient()
    response = provider.answer(
        AnswerRequest(
            question="怎么做?",
            contexts=[RerankDocument(chunk_id="c1", text="只能基于此上下文回答。", score=0.8)],
            temperature=0.2,
            max_tokens=128,
        )
    )

    assert response.provider == "openai-compatible"
    assert response.answer == "答案来自上下文 [1]"
    assert response.citations == ["c1"]
    call = fake_client.calls[0]
    assert call["url"] == "http://llm.local/v1/chat/completions"
    assert call["headers"]["Authorization"] == "Bearer llm-key"
    assert call["json"]["model"] == "qwen-test"
    assert call["json"]["temperature"] == 0.2
    assert call["json"]["max_completion_tokens"] == 128
    assert call["json"]["messages"][0]["role"] == "system"
    assert "Context:" in call["json"]["messages"][1]["content"]
    get_settings.cache_clear()


def test_http_reranker_builds_expected_payload_and_parses_results(monkeypatch) -> None:
    get_settings.cache_clear()
    monkeypatch.setenv("AI_RERANK_ENDPOINT", "http://rerank.local/api/rerank")
    monkeypatch.setenv("RERANK_API_KEY", "rerank-key")
    monkeypatch.setenv("RERANK_MODEL_NAME", "bge-reranker-test")
    fake_client = _FakeHttpClient(
        {
            "model": "bge-reranker-test",
            "results": [
                {"chunk_id": "c2", "score": 0.96},
                {"chunk_id": "c1", "score": 0.31},
            ],
        }
    )
    monkeypatch.setattr("app.services.rerank.http.httpx.Client", lambda **kwargs: fake_client)

    provider = HttpReranker()
    response = provider.rerank(
        RerankRequest(
            query="金融数据治理",
            top_k=1,
            documents=[
                RerankDocument(chunk_id="c1", text="教育制度", score=0.4),
                RerankDocument(chunk_id="c2", text="金融数据治理方案", score=0.5),
            ],
        )
    )

    assert response.provider == "http-rerank"
    assert response.model == "bge-reranker-test"
    assert [document.chunk_id for document in response.documents] == ["c2"]
    call = fake_client.calls[0]
    assert call["url"] == "http://rerank.local/api/rerank"
    assert call["headers"]["Authorization"] == "Bearer rerank-key"
    assert call["json"]["query"] == "金融数据治理"
    assert call["json"]["model"] == "bge-reranker-test"
    assert call["json"]["top_k"] == 1
    assert call["json"]["documents"][0]["chunk_id"] == "c1"
    get_settings.cache_clear()
