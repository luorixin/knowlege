"""Centralised runtime configuration for the knowledge AI service.

Settings are loaded from environment variables (and a local ``.env`` file when
present). All model endpoints default to mock/offline values so the MVP runs
without any external model dependency, while provider switches can route to
OpenAI-compatible or private HTTP services.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # --- Service runtime -------------------------------------------------
    ai_service_host: str = "0.0.0.0"
    ai_service_port: int = 8001
    ai_service_log_level: str = "INFO"

    # --- Parser ----------------------------------------------------------
    parser_enable_ocr: bool = False
    parser_temp_dir: str = "/tmp/knowledge-ai"

    # --- Provider switches (mock = no external dependency) ---------------
    embedding_provider: str = "mock"
    rerank_provider: str = "mock"
    llm_provider: str = "mock"

    # --- Embedding -------------------------------------------------------
    embedding_model_name: str = "mock-embedding-v1"
    embedding_dimension: int = 16
    ai_embedding_endpoint: str = "http://localhost:8001"
    embedding_api_key: str = ""

    # --- LLM -------------------------------------------------------------
    llm_model_name: str = "mock-llm"
    ai_llm_endpoint: str = "http://localhost:8001"
    llm_api_key: str = ""
    openai_api_key: str = ""
    qwen_api_key: str = ""
    llm_temperature: float = 0.0
    llm_max_tokens: int = 2048

    # --- Rerank ----------------------------------------------------------
    ai_rerank_endpoint: str = "http://localhost:8001"
    rerank_model_name: str = "mock-rerank"
    rerank_api_key: str = ""

    # --- Shared HTTP client knobs ---------------------------------------
    ai_request_timeout: int = Field(default=60, description="Per-request timeout in seconds")
    ai_max_concurrency: int = Field(default=8, description="Max in-flight model requests")


@lru_cache
def get_settings() -> Settings:
    """Return a process-wide cached :class:`Settings` instance."""
    return Settings()
