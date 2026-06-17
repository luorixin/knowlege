from app.schemas.pipeline import RerankRequest, RerankResponse


class MockReranker:

    def rerank(self, request: RerankRequest) -> RerankResponse:
        documents = sorted(request.documents, key=lambda item: item.score, reverse=True)
        return RerankResponse(query=request.query, documents=documents)
