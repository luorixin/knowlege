from app.main import create_app
from fastapi.testclient import TestClient


def test_health_returns_service_status_and_capabilities() -> None:
    client = TestClient(create_app())

    response = client.get("/api/v1/health")

    assert response.status_code == 200
    assert response.json() == {
        "service": "knowledge-ai-service",
        "status": "UP",
        "capabilities": [
            "parser",
            "chunker",
            "embedding",
            "rerank",
            "llm",
        ],
    }
