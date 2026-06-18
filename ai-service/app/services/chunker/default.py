import hashlib
import logging

from app.schemas.pipeline import ChunkItem, ChunkRequest, ChunkResponse, ParseAndChunkRequest, ParseAndChunkResponse

logger = logging.getLogger(__name__)


class DefaultChunker:
    """A naive overlapping text chunker."""

    def split(self, request: ChunkRequest) -> ChunkResponse:
        chunks = self._chunk_text(request.text, request.chunk_size, request.overlap)
        items = []
        for i, text in enumerate(chunks, start=1):
            items.append(self._build_chunk_item(request.document_id, i, text, 1, None, {}))
        return ChunkResponse(document_id=request.document_id, chunks=items)

    def _chunk_text(self, text: str, chunk_size: int, overlap: int) -> list[str]:
        if not text:
            return []
        if chunk_size <= 0:
            chunk_size = 800
        if overlap < 0 or overlap >= chunk_size:
            overlap = 100
        
        step = chunk_size - overlap
        chunks = []
        for i in range(0, len(text), step):
            chunk = text[i : i + chunk_size]
            chunks.append(chunk)
        return chunks

    def _build_chunk_item(
        self, doc_id: str, chunk_no: int, text: str, page_no: int | None, section_title: str | None, metadata: dict
    ) -> ChunkItem:
        content_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
        char_count = len(text)
        token_count = char_count  # Mock token count
        return ChunkItem(
            chunk_id=f"{doc_id}-chunk-{chunk_no}",
            chunk_no=chunk_no,
            text=text,
            page_no=page_no,
            section_title=section_title,
            content_hash=content_hash,
            char_count=char_count,
            token_count=token_count,
            metadata=metadata,
        )
