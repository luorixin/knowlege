from app.schemas.pipeline import AnswerRequest, AnswerResponse


class MockLlmClient:

    def answer(self, request: AnswerRequest) -> AnswerResponse:
        citations = [context.chunk_id for context in request.contexts]
        return AnswerResponse(
            answer=f"Mock answer for: {request.question}",
            citations=citations,
        )
