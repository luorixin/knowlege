import logging
import httpx
from fastapi import HTTPException

from app.schemas.pipeline import AnswerRequest, AnswerResponse
from app.config import get_settings

logger = logging.getLogger(__name__)

class OpenAICompatibleLLMClient:
    def __init__(self):
        settings = get_settings()
        self.api_key = settings.llm_api_key or settings.openai_api_key or settings.qwen_api_key
        self.endpoint = settings.ai_llm_endpoint.rstrip("/")
        self.model_name = settings.llm_model_name
        self.temperature = settings.llm_temperature
        self.max_tokens = settings.llm_max_tokens

    def answer(self, request: AnswerRequest) -> AnswerResponse:
        url = f"{self.endpoint}/chat/completions"
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        
        # Build context
        context_str = "\n\n".join([f"[{i+1}] {doc.text}" for i, doc in enumerate(request.contexts)])
        system_prompt = request.system_prompt or (
            "You are a helpful enterprise knowledge base assistant. "
            "Answer strictly from the provided context and preserve citation numbers."
        )
        user_prompt = f"Context:\n{context_str}\n\nQuestion: {request.question}"

        payload = {
            "model": request.model or self.model_name,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": request.temperature if request.temperature is not None else self.temperature,
            "max_completion_tokens": request.max_tokens or self.max_tokens,
        }
        
        try:
            with httpx.Client(timeout=get_settings().ai_request_timeout, trust_env=False) as client:
                response = client.post(url, headers=headers, json=payload)
                response.raise_for_status()
                data = response.json()
        except httpx.HTTPError as exc:
            logger.error(f"HTTPError in OpenAI LLM provider: {exc}")
            raise HTTPException(status_code=502, detail=f"LLM provider error: {exc}")
        except Exception as exc:
            logger.exception("Unexpected error in OpenAI LLM provider")
            raise HTTPException(status_code=500, detail="Unexpected LLM provider error")

        answer_text = "No answer returned."
        choices = data.get("choices", [])
        if choices:
            answer_text = choices[0].get("message", {}).get("content", "")

        citations = [doc.chunk_id for doc in request.contexts]
        
        return AnswerResponse(
            answer=answer_text,
            citations=citations,
            model=data.get("model") or request.model or self.model_name,
            provider="openai-compatible",
            usage=data.get("usage"),
        )
