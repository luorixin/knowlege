from __future__ import annotations

import zipfile
from pathlib import Path
from typing import Any

from fastapi.testclient import TestClient

from app.main import create_app
from app.schemas.document_parse import DocumentParseRequest
from app.services.parser.errors import OcrProviderError
from app.services.parser.base import OcrResult, VLMCaptionResult
from app.services.parser.structured import OCRParser, ParserRouter, StructuredDocumentParser


def test_parse_txt_document(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.txt"
    file_path.write_text("First line\nSecond line", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-1",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "txt",
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["doc_id"] == "doc-1"
    assert payload["version_id"] == "v1"
    assert payload["pages"] == [
        {
            "page_no": 1,
            "section_title": "sample.txt",
            "content_type": "text",
            "content": "First line\nSecond line",
            "metadata": {"file_type": "txt"},
        }
    ]


def test_parse_document_accepts_pipeline_style_aliases(tmp_path: Path) -> None:
    file_path = tmp_path / "legacy-payload.txt"
    file_path.write_text("Legacy payload fields", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "document_id": "doc-legacy",
            "storage_uri": str(file_path),
            "file_type": "txt",
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["doc_id"] == "doc-legacy"
    assert payload["version_id"] == "1"
    assert payload["pages"][0]["content"] == "Legacy payload fields"


def test_parse_markdown_uses_first_heading_as_section_title(tmp_path: Path) -> None:
    file_path = tmp_path / "proposal.md"
    file_path.write_text("# Proposal Overview\n\nBody text", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-md",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "md",
        },
    )

    assert response.status_code == 200
    page = response.json()["pages"][0]
    assert page["section_title"] == "Proposal Overview"
    assert page["content"] == "# Proposal Overview\n\nBody text"


def test_parse_pdf_extracts_page_text(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.pdf"
    file_path.write_bytes(_minimal_pdf_bytes("Hello PDF"))
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-pdf",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "pdf",
        },
    )

    assert response.status_code == 200
    pages = response.json()["pages"]
    assert len(pages) == 1
    assert pages[0]["page_no"] == 1
    assert pages[0]["content_type"] == "text"
    assert "Hello PDF" in pages[0]["content"]
    payload = response.json()
    assert payload["status"] == "SUCCESS"
    assert payload["metadata"]["parser"] == "PdfNativeParser"
    assert payload["blocks"][0]["block_type"] == "paragraph"
    assert payload["blocks"][0]["page_no"] == 1
    assert payload["blocks"][0]["source_uri"] == str(file_path)
    assert "Hello PDF" in payload["blocks"][0]["markdown"]


def test_parse_image_like_pdf_uses_mock_ocr_parser(tmp_path: Path) -> None:
    file_path = tmp_path / "scan.pdf"
    file_path.write_bytes(_minimal_pdf_bytes(""))
    parser = StructuredDocumentParser(router=ParserRouter(enable_ocr=True))

    result = parser.parse(DocumentParseRequest(
        doc_id="doc-scan-pdf",
        version_id="v1",
        file_path=str(file_path),
        file_type="pdf",
    ))

    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "OCRParser"
    assert result.pages[0].metadata["parser"] == "OCRParser"
    assert result.blocks[0].block_type == "paragraph"
    assert result.blocks[0].confidence < 1
    assert "Mock OCR text" in result.blocks[0].content


def test_mixed_pdf_uses_native_text_and_ocr_per_page(tmp_path: Path) -> None:
    file_path = tmp_path / "mixed.pdf"
    file_path.write_bytes(_minimal_multipage_pdf_bytes(["Native page text", ""]))
    parser = StructuredDocumentParser(router=ParserRouter(ocr_provider=_StructuredOcrProvider()))

    result = parser.parse(DocumentParseRequest(
        doc_id="doc-mixed",
        version_id="v1",
        file_path=str(file_path),
        file_type="pdf",
    ))

    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "PdfMixedParser"
    assert [page.metadata["page_parse_mode"] for page in result.pages] == ["native_text", "scanned_image"]
    assert result.pages[0].content == "Native page text"
    assert result.pages[1].content == "OCR text page 2"
    assert result.blocks[1].bbox == [1.0, 2.0, 3.0, 4.0]
    assert result.blocks[1].confidence == 0.88
    assert result.blocks[1].metadata["language"] == "zh"


def test_scanned_pdf_without_ocr_records_page_error(tmp_path: Path) -> None:
    file_path = tmp_path / "scan-disabled.pdf"
    file_path.write_bytes(_minimal_pdf_bytes(""))
    parser = StructuredDocumentParser(router=ParserRouter(ocr_provider=_StructuredOcrProvider(), enable_ocr=False))

    result = parser.parse(DocumentParseRequest(
        doc_id="doc-scan-disabled",
        version_id="v1",
        file_path=str(file_path),
        file_type="pdf",
    ))

    assert result.status == "FAILED"
    assert result.errors[0].code == "OCR_DISABLED"
    assert result.errors[0].metadata["page_parse_mode"] == "scanned_image"
    assert result.pages == []
    assert result.blocks == []


def test_parse_docx_extracts_paragraphs(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.docx"
    _write_zip(
        file_path,
        {
            "word/document.xml": """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>Docx Title</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Docx body</w:t></w:r></w:p>
                  </w:body>
                </w:document>
            """,
        },
    )
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-docx",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "docx",
        },
    )

    assert response.status_code == 200
    page = response.json()["pages"][0]
    assert page["section_title"] == "Docx Title"
    assert page["content"] == "Docx Title\nDocx body"


def test_docx_preserves_heading_list_and_table_blocks(tmp_path: Path) -> None:
    file_path = tmp_path / "structured.docx"
    _write_zip(
        file_path,
        {
            "word/document.xml": """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>项目背景</w:t></w:r></w:p>
                    <w:p><w:r><w:t>背景正文</w:t></w:r></w:p>
                    <w:p><w:pPr><w:numPr><w:ilvl w:val="0"/></w:numPr></w:pPr><w:r><w:t>列表项</w:t></w:r></w:p>
                    <w:tbl>
                      <w:tr><w:tc><w:p><w:r><w:t>字段</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>值</w:t></w:r></w:p></w:tc></w:tr>
                      <w:tr><w:tc><w:p><w:r><w:t>行业</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>金融</w:t></w:r></w:p></w:tc></w:tr>
                    </w:tbl>
                  </w:body>
                </w:document>
            """,
        },
    )
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={"doc_id": "doc-docx-structured", "version_id": "v1", "file_path": str(file_path), "file_type": "docx"},
    )

    assert response.status_code == 200
    blocks = response.json()["blocks"]
    assert [block["block_type"] for block in blocks] == ["title", "paragraph", "list", "table"]
    assert blocks[1]["section_title"] == "项目背景"
    assert blocks[3]["metadata"]["headers"] == ["字段", "值"]
    assert "| 字段 | 值 |" in blocks[3]["markdown"]


def test_parse_pptx_extracts_slide_text(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.pptx"
    _write_zip(
        file_path,
        {
            "ppt/slides/slide1.xml": """
                <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                  <p:cSld><p:spTree>
                    <p:sp><p:txBody><a:p><a:r><a:t>Slide Title</a:t></a:r></a:p></p:txBody></p:sp>
                    <p:sp><p:txBody><a:p><a:r><a:t>Slide bullet</a:t></a:r></a:p></p:txBody></p:sp>
                  </p:spTree></p:cSld>
                </p:sld>
            """,
        },
    )
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-pptx",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "pptx",
        },
    )

    assert response.status_code == 200
    page = response.json()["pages"][0]
    assert page["page_no"] == 1
    assert page["section_title"] == "Slide Title"
    assert page["content"] == "Slide Title\nSlide bullet"
    payload = response.json()
    assert payload["metadata"]["parser"] == "PptParser"
    assert payload["blocks"][0]["metadata"]["slide_text"] == "Slide Title\nSlide bullet"
    assert payload["blocks"][0]["metadata"]["slide_summary"] == "Slide Title"
    assert payload["blocks"][0]["image_uri"].endswith("#slide-1")


def test_pptx_empty_slide_uses_structured_vlm_caption_and_table_block(tmp_path: Path) -> None:
    file_path = tmp_path / "caption-table.pptx"
    _write_zip(
        file_path,
        {
            "ppt/slides/slide1.xml": """
                <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                  <p:cSld><p:spTree>
                    <p:graphicFrame><a:graphic><a:graphicData>
                      <a:tbl>
                        <a:tr><a:tc><a:txBody><a:p><a:r><a:t>阶段</a:t></a:r></a:p></a:txBody></a:tc><a:tc><a:txBody><a:p><a:r><a:t>交付物</a:t></a:r></a:p></a:txBody></a:tc></a:tr>
                        <a:tr><a:tc><a:txBody><a:p><a:r><a:t>一期</a:t></a:r></a:p></a:txBody></a:tc><a:tc><a:txBody><a:p><a:r><a:t>方案</a:t></a:r></a:p></a:txBody></a:tc></a:tr>
                      </a:tbl>
                    </a:graphicData></a:graphic></p:graphicFrame>
                  </p:spTree></p:cSld>
                </p:sld>
            """,
            "ppt/slides/slide2.xml": """
                <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                  <p:cSld><p:spTree></p:spTree></p:cSld>
                </p:sld>
            """,
        },
    )
    parser = StructuredDocumentParser(router=ParserRouter(caption_provider=_StructuredCaptionProvider()))

    result = parser.parse(DocumentParseRequest(
        doc_id="doc-ppt-vlm",
        version_id="v1",
        file_path=str(file_path),
        file_type="pptx",
    ))

    table_blocks = [block for block in result.blocks if block.block_type == "table"]
    figure_blocks = [block for block in result.blocks if block.block_type == "figure"]
    assert table_blocks[0].metadata["headers"] == ["阶段", "交付物"]
    assert "| 阶段 | 交付物 |" in table_blocks[0].markdown
    assert figure_blocks[1].content == "VLM caption for slide"
    assert figure_blocks[1].metadata["caption_provider"] == "test-vlm"
    assert figure_blocks[1].confidence == 0.91


def test_parse_xlsx_extracts_rows_as_table_text(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.xlsx"
    _write_zip(
        file_path,
        {
            "xl/sharedStrings.xml": """
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <si><t>Name</t></si>
                  <si><t>Value</t></si>
                  <si><t>Revenue</t></si>
                </sst>
            """,
            "xl/worksheets/sheet1.xml": """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c>
                      <c r="B1" t="s"><v>1</v></c>
                    </row>
                    <row r="2">
                      <c r="A2" t="s"><v>2</v></c>
                      <c r="B2"><v>1200</v></c>
                    </row>
                  </sheetData>
                </worksheet>
            """,
        },
    )
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-xlsx",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "xlsx",
        },
    )

    assert response.status_code == 200
    page = response.json()["pages"][0]
    assert page["content_type"] == "table"
    assert page["content"] == "Name\tValue\nRevenue\t1200"
    assert page["metadata"]["sheet_name"] == "sheet1"
    payload = response.json()
    assert payload["metadata"]["parser"] == "ExcelParser"
    assert payload["blocks"][0]["block_type"] == "table"
    assert "| Name | Value |" in payload["blocks"][0]["markdown"]
    assert payload["blocks"][0]["metadata"]["headers"] == ["Name", "Value"]
    assert payload["blocks"][0]["metadata"]["merged_cells"] == []


def test_xlsx_uses_workbook_sheet_names_formulas_and_splits_large_tables(tmp_path: Path) -> None:
    file_path = tmp_path / "workbook.xlsx"
    row_xml = "\n".join(
        f'<row r="{i}"><c r="A{i}" t="s"><v>{0 if i == 1 else 2}</v></c><c r="B{i}"><f>SUM(A1:A1)</f><v>{i}</v></c></row>'
        for i in range(1, 8)
    )
    _write_zip(
        file_path,
        {
            "xl/workbook.xml": """
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="收入明细" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
            """,
            "xl/_rels/workbook.xml.rels": """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Target="worksheets/sheet1.xml"/>
                </Relationships>
            """,
            "xl/sharedStrings.xml": """
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <si><t>科目</t></si>
                  <si><t>金额</t></si>
                  <si><t>收入</t></si>
                </sst>
            """,
            "xl/worksheets/sheet1.xml": f"""
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <dimension ref="A1:B7"/>
                  <sheetData>{row_xml}</sheetData>
                  <mergeCells><mergeCell ref="A1:B1"/></mergeCells>
                </worksheet>
            """,
        },
    )
    parser = StructuredDocumentParser(router=ParserRouter())

    result = parser.parse(DocumentParseRequest(doc_id="doc-xlsx-large", version_id="v1", file_path=str(file_path), file_type="xlsx"))

    assert result.pages[0].section_title == "收入明细"
    assert len(result.blocks) > 1
    assert result.blocks[0].metadata["sheet_name"] == "收入明细"
    assert result.blocks[0].metadata["formulas"][0]["formula"] == "SUM(A1:A1)"
    assert result.blocks[0].metadata["merged_cells"] == ["A1:B1"]


def test_parse_image_uses_image_parser_with_mock_caption(tmp_path: Path) -> None:
    file_path = tmp_path / "diagram.png"
    file_path.write_bytes(b"\x89PNG\r\n\x1a\nmock-image")
    parser = StructuredDocumentParser(router=ParserRouter())

    result = parser.parse(DocumentParseRequest(
        doc_id="doc-image",
        version_id="v1",
        file_path=str(file_path),
        file_type="png",
    ))

    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "ImageParser"
    assert result.pages[0].content_type == "image"
    assert result.blocks[0].block_type == "figure"
    assert result.blocks[0].image_uri == str(file_path)
    assert "Mock image caption" in result.blocks[0].content


def test_image_parser_records_structured_vlm_metadata_and_failure() -> None:
    parser = StructuredDocumentParser(router=ParserRouter(caption_provider=_FailingCaptionProvider()))
    file_path = Path(__file__)

    result = parser.parse(DocumentParseRequest(doc_id="doc-image-fail", version_id="v1", file_path=str(file_path), file_type="png"))

    assert result.status == "PARTIAL_SUCCESS"
    assert result.blocks[0].metadata["caption_error_code"] == "VLM_FAILED"
    assert result.errors[0].code == "VLM_FAILED"


def test_parse_pptx_records_page_error_and_returns_partial_success(tmp_path: Path) -> None:
    file_path = tmp_path / "partial.pptx"
    _write_zip(
        file_path,
        {
            "ppt/slides/slide1.xml": """
                <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                  <p:cSld><p:spTree>
                    <p:sp><p:txBody><a:p><a:r><a:t>Good Slide</a:t></a:r></a:p></p:txBody></p:sp>
                  </p:spTree></p:cSld>
                </p:sld>
            """,
            "ppt/slides/slide2.xml": "<p:sld><broken>",
        },
    )
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-partial-ppt",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "pptx",
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "PARTIAL_SUCCESS"
    assert len(payload["pages"]) == 1
    assert payload["pages"][0]["content"] == "Good Slide"
    assert payload["errors"][0]["page_no"] == 2
    assert payload["errors"][0]["code"] == "PAGE_PARSE_FAILED"


def test_parse_returns_clear_error_for_missing_file(tmp_path: Path) -> None:
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-missing",
            "version_id": "v1",
            "file_path": str(tmp_path / "missing.txt"),
            "file_type": "txt",
        },
    )

    assert response.status_code == 404
    assert response.json()["detail"]["code"] == "FILE_NOT_FOUND"


def test_parse_rejects_unsupported_file_type(tmp_path: Path) -> None:
    file_path = tmp_path / "sample.csv"
    file_path.write_text("a,b", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-csv",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "csv",
        },
    )

    assert response.status_code == 400
    assert response.json()["detail"]["code"] == "UNSUPPORTED_FILE_TYPE"


def test_parse_empty_document(tmp_path: Path) -> None:
    file_path = tmp_path / "empty.txt"
    file_path.write_text("", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-empty",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "txt",
        },
    )

    assert response.status_code == 200
    pages = response.json()["pages"]
    assert len(pages) == 1
    assert pages[0]["content"] == ""


def test_parse_corrupt_document(tmp_path: Path) -> None:
    file_path = tmp_path / "corrupt.docx"
    file_path.write_text("not a zip file", encoding="utf-8")
    client = TestClient(create_app())

    response = client.post(
        "/api/parse/document",
        json={
            "doc_id": "doc-corrupt",
            "version_id": "v1",
            "file_path": str(file_path),
            "file_type": "docx",
        },
    )

    assert response.status_code == 400
    assert response.json()["detail"]["code"] == "PARSE_FAILED"


class _RaisingOcrProvider:
    """Test double that raises OcrProviderError to simulate OCR failure."""

    def __init__(self, code: str, message: str) -> None:
        self.code = code
        self.message = message

    def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> str:
        raise OcrProviderError(self.code, self.message)


class _StructuredOcrProvider:
    def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> OcrResult:
        return OcrResult(
            text=f"OCR text page {page_no}",
            bbox=[1.0, 2.0, 3.0, 4.0],
            confidence=0.88,
            language="zh",
            provider="test-ocr",
            elapsed_ms=12,
        )


class _StructuredCaptionProvider:
    def caption(self, image_uri: str, metadata: dict[str, Any] | None = None) -> VLMCaptionResult:
        return VLMCaptionResult(
            caption="VLM caption for slide",
            provider="test-vlm",
            model_name="vlm-test",
            confidence=0.91,
            elapsed_ms=34,
        )


class _FailingCaptionProvider:
    def caption(self, image_uri: str, metadata: dict[str, Any] | None = None) -> VLMCaptionResult:
        return VLMCaptionResult(
            caption="",
            provider="test-vlm",
            error_code="VLM_FAILED",
            error_message="caption unavailable",
        )


def _ocr_request(file_path: Path) -> DocumentParseRequest:
    return DocumentParseRequest(
        doc_id="doc-ocr",
        version_id="v1",
        file_path=str(file_path),
        file_type="pdf",
    )


def test_ocr_recognition_failure_surfaces_as_partial_success(tmp_path: Path) -> None:
    """OCR_RECOGNIZE_FAILED must surface as a page error, never a silent empty page.

    The minimal PDF has a single page; when OCR fails on it the page is dropped,
    so the overall status is FAILED with a single error whose code is the
    provider's own (not a generic PAGE_PARSE_FAILED)."""
    file_path = tmp_path / "scan.pdf"
    file_path.write_bytes(_minimal_pdf_bytes(""))
    provider = _RaisingOcrProvider("OCR_RECOGNIZE_FAILED", "model crashed")

    result = OCRParser(provider).parse(_ocr_request(file_path), file_path, "pdf")  # type: ignore[arg-type]

    # The failed page is skipped and the error is recorded with the provider's
    # own code, so callers can tell it apart from a generic PAGE_PARSE_FAILED.
    assert len(result.errors) == 1
    assert result.errors[0].page_no == 1
    assert result.errors[0].code == "OCR_RECOGNIZE_FAILED"
    assert "model crashed" in result.errors[0].message
    assert result.errors[0].metadata["ocr_error_code"] == "OCR_RECOGNIZE_FAILED"
    assert result.pages == []
    # No block produced for the failed page — nothing fake enters the corpus.
    assert result.blocks == []
    # Single-page document whose only page failed to OCR.
    assert result.status == "FAILED"


def test_ocr_image_extract_failure_surfaces_as_error(tmp_path: Path) -> None:
    """IMAGE_EXTRACT_FAILED (rasterisation/missing file) is surfaced, not swallowed."""
    file_path = tmp_path / "scan.pdf"
    file_path.write_bytes(_minimal_pdf_bytes(""))
    provider = _RaisingOcrProvider("IMAGE_EXTRACT_FAILED", "could not render page")

    result = OCRParser(provider).parse(_ocr_request(file_path), file_path, "pdf")  # type: ignore[arg-type]

    assert result.status == "FAILED"
    assert result.errors[0].code == "IMAGE_EXTRACT_FAILED"
    assert result.errors[0].metadata["ocr_error_code"] == "IMAGE_EXTRACT_FAILED"
    assert result.pages == []
    assert result.blocks == []


def test_parse_image_like_pdf_ocr_error_does_not_mask_unrelated_exception(tmp_path: Path) -> None:
    """A non-OcrProviderError still falls back to the generic PAGE_PARSE_FAILED code."""

    class _GenericRaisingProvider:
        def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> str:
            raise ValueError("unexpected")

    file_path = tmp_path / "scan.pdf"
    file_path.write_bytes(_minimal_pdf_bytes(""))
    provider = _GenericRaisingProvider()
    result = OCRParser(provider).parse(_ocr_request(file_path), file_path, "pdf")  # type: ignore[arg-type]

    assert result.status == "FAILED"
    assert result.errors[0].code == "PAGE_PARSE_FAILED"
    assert "ocr_error_code" not in result.errors[0].metadata


def test_ocr_partial_failure_returns_partial_success_with_some_pages(tmp_path: Path) -> None:
    """Multi-page doc where some pages OCR fine and one fails → PARTIAL_SUCCESS.

    This is the production scenario: a scanned PDF where a single corrupt page
    must not abort the whole document, but must be reported as an error."""

    class _FailOnPage2Provider:
        def __init__(self) -> None:
            self.calls: list[int] = []

        def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> str:
            self.calls.append(page_no)
            if page_no == 2:
                raise OcrProviderError("OCR_RECOGNIZE_FAILED", "page 2 corrupt")
            return f"text from page {page_no}"

    file_path = tmp_path / "scan.pdf"
    file_path.write_bytes(_minimal_multipage_pdf_bytes(["", ""]))
    provider = _FailOnPage2Provider()
    result = OCRParser(provider).parse(_ocr_request(file_path), file_path, "pdf")  # type: ignore[arg-type]

    assert result.status == "PARTIAL_SUCCESS"
    assert [page.page_no for page in result.pages] == [1]
    assert [block.page_no for block in result.blocks] == [1]
    assert len(result.errors) == 1
    assert result.errors[0].page_no == 2
    assert result.errors[0].code == "OCR_RECOGNIZE_FAILED"


def _write_zip(path: Path, files: dict[str, str]) -> None:
    with zipfile.ZipFile(path, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content.strip())


def _minimal_pdf_bytes(text: str) -> bytes:
    return _minimal_multipage_pdf_bytes([text])


def _minimal_multipage_pdf_bytes(texts: list[str]) -> bytes:
    """Build a minimal valid multi-page PDF whose pages carry the given text.

    Empty text yields image-like pages (no extractable layer), so the router
    falls back to OCR — useful for testing the OCR error path."""
    # Object layout (1-based):
    #   1 = Catalog, 2 = Pages (Kids filled once page objects exist),
    #   then per page i: a Page object and its Contents stream,
    #   last object = shared Font.
    page_count = len(texts)
    page_obj_numbers = [3 + 2 * i for i in range(page_count)]
    font_obj_number = 3 + 2 * page_count

    objects: list[bytes] = []
    # obj 1 — Catalog
    objects.append(b"<< /Type /Catalog /Pages 2 0 R >>")
    # obj 2 — Pages (Kids + Count)
    kids = " ".join(f"{n} 0 R" for n in page_obj_numbers)
    objects.append(f"<< /Type /Pages /Kids [{kids}] /Count {page_count} >>".encode("ascii"))
    # per page — Page + Contents stream
    for i, text in enumerate(texts):
        stream_no = page_obj_numbers[i] + 1
        objects.append(
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
            f"/Resources << /Font << /F1 {font_obj_number} 0 R >> >> /Contents {stream_no} 0 R >>".encode("ascii")
        )
        stream = f"BT /F1 24 Tf 72 720 Td ({text}) Tj ET".encode("ascii")
        objects.append(b"<< /Length " + str(len(stream)).encode("ascii") + b" >> stream\n" + stream + b"\nendstream")
    # shared Font
    objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

    output = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for obj_no, body in enumerate(objects, start=1):
        offsets.append(len(output))
        output.extend(f"{obj_no} 0 obj ".encode("ascii") + body + b"\nendobj\n")
    xref_start = len(output)
    output.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
    output.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        output.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    output.extend(
        f"trailer << /Size {len(objects) + 1} /Root 1 0 R >>\n"
        f"startxref\n{xref_start}\n%%EOF\n".encode("ascii")
    )
    return bytes(output)
