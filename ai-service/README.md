# Knowledge AI Service

FastAPI service for document parsing, chunking, embedding, rerank, and LLM abstraction.

## Install

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e '.[dev]'
```

`requirements.txt` is also provided for environments that do not install from `pyproject.toml`:

```bash
python -m pip install -r requirements.txt
```

## Run

```bash
uvicorn app.main:app --reload --port 8001
```

Health check:

```bash
curl http://localhost:8001/api/v1/health
```

## Parse Document

Endpoint:

```text
POST /api/parse/document
```

Request:

```json
{
  "doc_id": "123",
  "version_id": "1",
  "file_path": "/absolute/path/to/document.pdf",
  "file_type": "pdf"
}
```

Response:

```json
{
  "doc_id": "123",
  "version_id": "1",
  "pages": [
    {
      "page_no": 1,
      "section_title": "document.pdf",
      "content_type": "text",
      "content": "Extracted text",
      "metadata": {
        "file_type": "pdf"
      }
    }
  ]
}
```

Supported `file_type` values:

- `txt`
- `md`
- `pdf`
- `docx`
- `pptx`
- `xlsx`

Current parser scope is basic text/table extraction without OCR. The parser structure is intentionally split by format so OCR, table recognition, and layout recognition can be added later.

## Test

```bash
cd ai-service
.venv/bin/python -m pytest
```
