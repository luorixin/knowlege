from pathlib import Path

from app.schemas.document_parse import DocumentParseRequest
from app.services.parser.router import ParserRouter, StructuredDocumentParser

FIXTURES_DIR = Path(__file__).parent / "fixtures" / "documents"


def _create_parser() -> StructuredDocumentParser:
    # Use mock providers for OCR and Caption so tests are fast and deterministic
    return StructuredDocumentParser(router=ParserRouter(enable_ocr=True))


def test_digital_pdf_quality():
    file_path = FIXTURES_DIR / "digital.pdf"
    if not file_path.exists():
        return
    parser = _create_parser()
    request = DocumentParseRequest(doc_id="test-doc-1", version_id="v1", file_path=str(file_path), file_type="pdf")
    result = parser.parse(request)
    
    assert result.status == "SUCCESS"
    assert len(result.pages) >= 1
    assert result.metadata["parser"] == "PdfNativeParser"
    assert result.blocks[0].page_no == 1
    assert result.blocks[0].section_title == "test-doc-1"
    assert any(b.block_type == "paragraph" for b in result.blocks)


def test_scanned_pdf_quality():
    file_path = FIXTURES_DIR / "scanned.pdf"
    if not file_path.exists():
        return
    parser = _create_parser()
    request = DocumentParseRequest(doc_id="test-doc-2", version_id="v1", file_path=str(file_path), file_type="pdf")
    result = parser.parse(request)
    
    assert result.status == "SUCCESS"
    assert result.metadata["parser"] in ["OCRParser", "PdfMixedParser"]
    assert "page_modes" in result.metadata
    assert "scanned_image" in result.metadata["page_modes"]


def test_docx_quality():
    file_path = FIXTURES_DIR / "headings_tables.docx"
    if not file_path.exists():
        return
    parser = _create_parser()
    request = DocumentParseRequest(doc_id="test-doc-3", version_id="v1", file_path=str(file_path), file_type="docx")
    result = parser.parse(request)
    
    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "DocxParser"
    
    block_types = {b.block_type for b in result.blocks}
    assert "title" in block_types
    assert "paragraph" in block_types
    assert "table" in block_types
    
    # Check table markdown
    table_block = next(b for b in result.blocks if b.block_type == "table")
    assert "| Header 1 | Header 2 |" in table_block.markdown
    assert "| Row 1 Col 1 | Row 1 Col 2 |" in table_block.markdown


def test_pptx_quality():
    file_path = FIXTURES_DIR / "tables_charts.pptx"
    if not file_path.exists():
        return
    parser = _create_parser()
    request = DocumentParseRequest(doc_id="test-doc-4", version_id="v1", file_path=str(file_path), file_type="pptx")
    result = parser.parse(request)
    
    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "PptParser"
    
    # slide 1 has titles
    # slide 2 has a table
    table_blocks = [b for b in result.blocks if b.block_type == "table"]
    assert len(table_blocks) >= 1
    
    assert any("Presentation Title" in b.section_title for b in result.blocks if b.section_title)


def test_xlsx_quality():
    file_path = FIXTURES_DIR / "multi_sheet.xlsx"
    if not file_path.exists():
        return
    parser = _create_parser()
    request = DocumentParseRequest(doc_id="test-doc-5", version_id="v1", file_path=str(file_path), file_type="xlsx")
    result = parser.parse(request)
    
    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "ExcelParser"
    assert len(result.pages) == 2
    assert result.pages[0].section_title.lower() == "sheet1"
    assert result.pages[1].section_title.lower() == "sheet2"
    
    # Check formulas and merged cells
    assert "formulas" in result.pages[0].metadata
    assert "merged_cells" in result.pages[0].metadata


def test_image_quality():
    file_path = FIXTURES_DIR / "flowchart.png"
    if not file_path.exists():
        return
    parser = _create_parser()
    request = DocumentParseRequest(doc_id="test-doc-6", version_id="v1", file_path=str(file_path), file_type="png")
    result = parser.parse(request)
    
    assert result.status == "SUCCESS"
    assert result.metadata["parser"] == "ImageParser"
    assert len(result.blocks) == 1
    assert result.blocks[0].block_type == "figure"
    assert result.blocks[0].image_uri == "doc:test-doc-6/version:v1/image:1"
