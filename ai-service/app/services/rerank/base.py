from typing import Protocol

from app.schemas.pipeline import RerankRequest, RerankResponse


class Reranker(Protocol):

    def rerank(self, request: RerankRequest) -> RerankResponse:
        """Rank retrieved chunks against the user query."""
