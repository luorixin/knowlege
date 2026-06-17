from app.schemas.pipeline import ParseRequest, ParseResponse


class MockDocumentParser:

    def parse(self, request: ParseRequest) -> ParseResponse:
        return ParseResponse(
            document_id=request.document_id,
            text=f"Mock parsed text for {request.file_type} document at {request.storage_uri}",
        )
