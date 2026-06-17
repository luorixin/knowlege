from typing import Protocol

from app.schemas.pipeline import ChunkRequest, ChunkResponse


class Chunker(Protocol):

    def split(self, request: ChunkRequest) -> ChunkResponse:
        """Split normalized text into retrievable chunks."""
