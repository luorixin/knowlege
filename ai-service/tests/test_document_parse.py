from __future__ import annotations

import zipfile
from pathlib import Path

from fastapi.testclient import TestClient

from app.main import create_app


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


def _write_zip(path: Path, files: dict[str, str]) -> None:
    with zipfile.ZipFile(path, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content.strip())


def _minimal_pdf_bytes(text: str) -> bytes:
    stream = f"BT /F1 24 Tf 72 720 Td ({text}) Tj ET".encode("ascii")
    objects = [
        b"1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
        b"2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
        b"3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
        b"/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n",
        b"4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n",
        b"5 0 obj << /Length " + str(len(stream)).encode("ascii") + b" >> stream\n"
        + stream
        + b"\nendstream endobj\n",
    ]
    output = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for obj in objects:
        offsets.append(len(output))
        output.extend(obj)
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
