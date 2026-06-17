from app.schemas.pipeline import ChunkItem, ChunkRequest, ChunkResponse


class MockChunker:

    def split(self, request: ChunkRequest) -> ChunkResponse:
        text = request.text[: request.chunk_size]
        return ChunkResponse(
            document_id=request.document_id,
            chunks=[
                ChunkItem(
                    chunk_id=f"{request.document_id}-chunk-1",
                    chunk_no=1,
                    text=text,
                    page_no=1,
                    section_title="mock-section",
                )
            ],
        )
