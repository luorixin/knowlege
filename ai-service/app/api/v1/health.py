from fastapi import APIRouter

from app.schemas.health import HealthStatus

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthStatus)
def health() -> HealthStatus:
    return HealthStatus(
        service="knowledge-ai-service",
        status="UP",
        capabilities=["parser", "chunker", "embedding", "rerank", "llm"],
    )
