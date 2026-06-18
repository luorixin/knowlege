from app.config import get_settings
from app.schemas.pipeline import EmbeddingItem, EmbeddingRequest, EmbeddingResponse


class MockEmbeddingProvider:

    def embed(self, request: EmbeddingRequest) -> EmbeddingResponse:
        settings = get_settings()
        prompt_tokens = sum(len(text or "") for text in request.texts)
        embeddings = [
            EmbeddingItem(index=index, vector=_deterministic_vector(text, settings.embedding_dimension))
            for index, text in enumerate(request.texts)
        ]
        return EmbeddingResponse(
            model=request.model or settings.embedding_model_name,
            dimension=settings.embedding_dimension,
            embeddings=embeddings,
            usage={"prompt_tokens": prompt_tokens, "total_tokens": prompt_tokens},
            provider="mock",
        )


def _deterministic_vector(text: str, dimension: int) -> list[float]:
    values = [0.0] * dimension
    normalized = (text or "").strip()
    if not normalized:
        return values
    for index, char in enumerate(normalized):
        values[index % dimension] += float(ord(char))
    norm = sum(value * value for value in values) ** 0.5
    if norm == 0.0:
        return values
    return [value / norm for value in values]
