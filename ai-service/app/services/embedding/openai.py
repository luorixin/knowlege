import logging
import httpx

from fastapi import HTTPException

from app.schemas.pipeline import EmbeddingItem, EmbeddingRequest, EmbeddingResponse
from app.config import get_settings

logger = logging.getLogger(__name__)

class OpenAICompatibleHTTPProvider:
    def __init__(self):
        settings = get_settings()
        self.api_key = settings.embedding_api_key or settings.openai_api_key or settings.llm_api_key
        self.endpoint = settings.ai_embedding_endpoint.rstrip("/")
        self.default_model = settings.embedding_model_name
        self.default_dimension = settings.embedding_dimension

    def embed(self, request: EmbeddingRequest) -> EmbeddingResponse:
        url = f"{self.endpoint.rstrip('/')}/embeddings"
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        payload = {
            "model": request.model or self.default_model,
            "input": request.texts,
        }
        if request.dimensions or self.default_dimension:
            payload["dimensions"] = request.dimensions or self.default_dimension
        
        try:
            with httpx.Client(timeout=get_settings().ai_request_timeout, trust_env=False) as client:
                response = client.post(url, headers=headers, json=payload)
                response.raise_for_status()
                data = response.json()
        except httpx.HTTPError as exc:
            logger.error(f"HTTPError in OpenAI embedding provider: {exc}")
            raise HTTPException(status_code=502, detail=f"Embedding provider error: {exc}")
        except Exception as exc:
            logger.exception("Unexpected error in OpenAI embedding provider")
            raise HTTPException(status_code=500, detail="Unexpected embedding provider error")

        items = []
        dimension = 0
        for item in data.get("data", []):
            vector = item.get("embedding", [])
            items.append(EmbeddingItem(index=item.get("index", 0), vector=vector))
            if dimension == 0:
                dimension = len(vector)

        return EmbeddingResponse(
            model=data.get("model") or request.model or self.default_model,
            dimension=dimension,
            embeddings=items,
            usage=data.get("usage"),
            provider="openai-compatible",
        )
