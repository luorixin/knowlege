import zipfile
from pathlib import Path
from xml.etree import ElementTree

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResult,
    ParsedBlock,
    ParsedPage,
)
from app.services.parser.normalization import (
    _block,
    _first_child_by_local_name,
    _first_non_empty_line,
    _is_list_paragraph,
    _local_name,
    _paragraph_heading_level,
    _result,
    _table_markdown,
    _table_rows,
    _texts_in_element,
)


class DocxParser:
    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        with zipfile.ZipFile(file_path) as archive:
            xml_content = archive.read("word/document.xml")
        root = ElementTree.fromstring(xml_content)
        body_element = _first_child_by_local_name(root, "body")
        body = body_element if body_element is not None else root
        headings: list[dict[str, int | str]] = []
        blocks: list[ParsedBlock] = []
        page_lines: list[str] = []
        current_section: str | None = None

        for child in body:
            local_name = _local_name(child.tag)
            if local_name == "p":
                text = "".join(_texts_in_element(child)).strip()
                if not text:
                    continue
                heading_level = _paragraph_heading_level(child)
                if heading_level is not None:
                    current_section = text
                    headings.append({"level": heading_level, "text": text})
                    block_type = "title"
                else:
                    block_type = "list" if _is_list_paragraph(child) else "paragraph"
                page_lines.append(text)
                blocks.append(
                    _block(
                        request,
                        file_path,
                        page_no=1,
                        block_type=block_type,
                        content=text,
                        section_title=current_section or text,
                        confidence=1.0,
                        metadata={"file_type": "docx", "parser": "DocxParser", "heading_level": heading_level},
                        block_no=len(blocks) + 1,
                    )
                )
            elif local_name == "tbl":
                rows = _table_rows(child)
                markdown = _table_markdown(rows)
                content = "\n".join("\t".join(row).rstrip() for row in rows if any(row))
                if not content.strip():
                    continue
                page_lines.append(content)
                blocks.append(
                    _block(
                        request,
                        file_path,
                        page_no=1,
                        block_type="table",
                        content=content,
                        section_title=current_section or request.doc_id,
                        markdown=markdown,
                        confidence=1.0,
                        metadata={"file_type": "docx", "parser": "DocxParser", "headers": rows[0] if rows else []},
                        block_no=len(blocks) + 1,
                    )
                )

        content = "\n".join(page_lines)
        title = _first_non_empty_line(content) or request.doc_id
        page = ParsedPage(
            page_no=1,
            section_title=title,
            content_type="text",
            content=content,
            metadata={"file_type": "docx", "parser": "DocxParser", "block_count": len(blocks), "headings": headings},
        )
        return _result(request, "DocxParser", [page], blocks, [])
