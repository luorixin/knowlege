from app.schemas.pipeline import EmbeddingItem, EmbeddingRequest, EmbeddingResponse


class MockEmbeddingProvider:

    def embed(self, request: EmbeddingRequest) -> EmbeddingResponse:
        embeddings = [
            EmbeddingItem(index=index, vector=[float(index), 0.0, 1.0])
            for index, _ in enumerate(request.texts)
        ]
        return EmbeddingResponse(
            model=request.model,
            dimension=3,
            embeddings=embeddings,
        )
