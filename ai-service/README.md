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

## Docker Deployment

By default, the Docker image is lightweight and does NOT include the heavy dependencies required for OCR (like PaddleOCR).

To build the image with OCR support:

```bash
docker build --build-arg AI_SERVICE_INSTALL_OCR=true -t knowledge-ai-service .
```

To run PaddleOCR locally on CPU without Docker, you may need to install specific packages:

```bash
python -m pip install paddlepaddle paddleocr
```
(On macOS, PaddleOCR might require additional system dependencies like `libomp` via Homebrew).

## Provider Configuration

The service runs offline by default with mock providers. To call real model
services, copy `.env.example` to `.env` and switch the provider names:

```bash
EMBEDDING_PROVIDER=openai-compatible
EMBEDDING_MODEL_NAME=text-embedding-v3
EMBEDDING_DIMENSION=1024
AI_EMBEDDING_ENDPOINT=https://api.example.com/v1
EMBEDDING_API_KEY=change-me

LLM_PROVIDER=openai-compatible
LLM_MODEL_NAME=qwen-plus
AI_LLM_ENDPOINT=https://api.example.com/v1
LLM_API_KEY=change-me
LLM_TEMPERATURE=0
LLM_MAX_TOKENS=2048

RERANK_PROVIDER=private-http
RERANK_MODEL_NAME=bge-reranker
AI_RERANK_ENDPOINT=http://localhost:8002/api/v1/rerank
RERANK_API_KEY=change-me
```

Provider protocol:

- Embedding: `POST {AI_EMBEDDING_ENDPOINT}/embeddings` with `model`, `input`, and optional `dimensions`.
- LLM: `POST {AI_LLM_ENDPOINT}/chat/completions` with `model`, `messages`, `temperature`, and `max_completion_tokens`.
- Rerank: `POST {AI_RERANK_ENDPOINT}` with `query`, `documents`, `model`, and `top_k`.

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
