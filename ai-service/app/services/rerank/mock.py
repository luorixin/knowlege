from app.schemas.pipeline import RerankRequest, RerankResponse


class MockReranker:

    def rerank(self, request: RerankRequest) -> RerankResponse:
        documents = sorted(request.documents, key=lambda item: item.score, reverse=True)
        if request.top_k:
            documents = documents[: request.top_k]
        return RerankResponse(
            query=request.query,
            documents=documents,
            model=request.model or "mock-rerank",
            provider="mock",
        )
