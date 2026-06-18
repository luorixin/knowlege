from app.schemas.pipeline import AnswerRequest, AnswerResponse


class MockLlmClient:

    def answer(self, request: AnswerRequest) -> AnswerResponse:
        citations = [context.chunk_id for context in request.contexts]
        prompt_tokens = len(request.question or "")
        return AnswerResponse(
            answer=f"Mock answer for: {request.question}",
            citations=citations,
            model=request.model or "mock-llm",
            provider="mock",
            usage={"prompt_tokens": prompt_tokens, "completion_tokens": 0, "total_tokens": prompt_tokens},
        )
