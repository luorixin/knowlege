import logging
from typing import Any

import httpx
from fastapi import HTTPException

from app.config import get_settings
from app.schemas.pipeline import RerankDocument, RerankRequest, RerankResponse

logger = logging.getLogger(__name__)


class HttpReranker:
    """Generic rerank client for private or OpenAI-compatible rerank endpoints."""

    def __init__(self):
        settings = get_settings()
        self.endpoint = settings.ai_rerank_endpoint.rstrip("/")
        self.api_key = settings.rerank_api_key or settings.llm_api_key or settings.openai_api_key
        self.model_name = settings.rerank_model_name

    def rerank(self, request: RerankRequest) -> RerankResponse:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        payload = {
            "query": request.query,
            "documents": [
                {
                    "chunk_id": document.chunk_id,
                    "text": document.text,
                    "score": document.score,
                    "metadata": document.metadata,
                }
                for document in request.documents
            ],
            "model": request.model or self.model_name,
            "top_k": request.top_k,
        }

        try:
            with httpx.Client(timeout=get_settings().ai_request_timeout, trust_env=False) as client:
                response = client.post(self.endpoint, headers=headers, json=payload)
                response.raise_for_status()
                data = response.json()
        except httpx.HTTPError as exc:
            logger.error("HTTPError in rerank provider: %s", exc)
            raise HTTPException(status_code=502, detail=f"Rerank provider error: {exc}") from exc
        except Exception as exc:
            logger.exception("Unexpected error in rerank provider")
            raise HTTPException(status_code=500, detail="Unexpected rerank provider error") from exc

        documents = _parse_documents(data, request)
        if request.top_k:
            documents = documents[: request.top_k]
        return RerankResponse(
            query=data.get("query") or request.query,
            documents=documents,
            model=data.get("model") or request.model or self.model_name,
            provider="http-rerank",
        )


def _parse_documents(data: dict[str, Any], request: RerankRequest) -> list[RerankDocument]:
    raw_documents = data.get("documents")
    if raw_documents is None:
        raw_documents = data.get("results")
    if raw_documents is None:
        raw_documents = []

    by_id = {document.chunk_id: document for document in request.documents}
    parsed: list[RerankDocument] = []
    for index, item in enumerate(raw_documents):
        if not isinstance(item, dict):
            continue
        chunk_id = str(item.get("chunk_id") or item.get("id") or "")
        original = by_id.get(chunk_id)
        text = item.get("text") or (original.text if original else "")
        score = item.get("score")
        if score is None:
            score = item.get("relevance_score", item.get("rank_score", 0.0))
        parsed.append(
            RerankDocument(
                chunk_id=chunk_id or (original.chunk_id if original else str(index)),
                text=text,
                score=float(score),
                metadata=item.get("metadata") or (original.metadata if original else {}),
            )
        )

    if parsed:
        return sorted(parsed, key=lambda document: document.score, reverse=True)
    return sorted(request.documents, key=lambda document: document.score, reverse=True)
