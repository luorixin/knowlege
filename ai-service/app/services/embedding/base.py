from typing import Protocol

from app.schemas.pipeline import EmbeddingRequest, EmbeddingResponse


class EmbeddingProvider(Protocol):

    def embed(self, request: EmbeddingRequest) -> EmbeddingResponse:
        """Create embedding vectors for input texts."""
