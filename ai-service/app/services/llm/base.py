from typing import Protocol

from app.schemas.pipeline import AnswerRequest, AnswerResponse


class LlmClient(Protocol):

    def answer(self, request: AnswerRequest) -> AnswerResponse:
        """Generate an answer from grounded context."""
