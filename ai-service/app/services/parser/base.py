from typing import Protocol

from app.schemas.pipeline import ParseRequest, ParseResponse


class DocumentParser(Protocol):

    def parse(self, request: ParseRequest) -> ParseResponse:
        """Parse a stored document into normalized text."""
