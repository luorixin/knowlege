import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree

from app.schemas.document_parse import (
    DocumentParseRequest,
    DocumentParseResult,
    PageParseError,
    ParsedBlock,
    ParsedPage,
)
from app.services.parser.normalization import (
    EXCEL_TABLE_BLOCK_MAX_ROWS,
    _block,
    _merged_cells,
    _natural_sort_key,
    _page_error,
    _read_shared_strings,
    _read_sheet_formulas,
    _read_sheet_rows,
    _read_workbook_sheet_titles,
    _result,
    _sheet_dimension,
    _split_table_rows,
    _table_markdown,
)


class ExcelParser:
    def parse(self, request: DocumentParseRequest, file_path: Path, file_type: str) -> DocumentParseResult:
        pages: list[ParsedPage] = []
        blocks: list[ParsedBlock] = []
        errors: list[PageParseError] = []
        with zipfile.ZipFile(file_path) as archive:
            shared_strings = _read_shared_strings(archive)
            sheet_titles = _read_workbook_sheet_titles(archive)
            sheet_names = sorted(
                (name for name in archive.namelist() if re.fullmatch(r"xl/worksheets/sheet\d+\.xml", name)),
                key=_natural_sort_key,
            )
            for page_no, sheet_name in enumerate(sheet_names, start=1):
                try:
                    xml_content = archive.read(sheet_name)
                    rows = _read_sheet_rows(xml_content, shared_strings)
                    formulas = _read_sheet_formulas(xml_content)
                    content = "\n".join("\t".join(row).rstrip() for row in rows if any(row))
                    root = ElementTree.fromstring(xml_content)
                    table_region = _sheet_dimension(root)
                    headers = next((row for row in rows if any(row)), [])
                    merged_cells = _merged_cells(root)
                    sheet_title = sheet_titles.get(sheet_name, Path(sheet_name).stem)
                    metadata = {
                        "file_type": "xlsx",
                        "parser": "ExcelParser",
                        "sheet_name": sheet_title,
                        "table_region": table_region,
                        "headers": headers,
                        "merged_cells": merged_cells,
                        "formulas": formulas,
                        "row_count": len(rows),
                    }
                    pages.append(
                        ParsedPage(
                            page_no=page_no,
                            section_title=sheet_title,
                            content_type="table",
                            content=content,
                            metadata=metadata,
                        )
                    )
                    for table_index, table_rows in enumerate(_split_table_rows(rows, EXCEL_TABLE_BLOCK_MAX_ROWS), start=1):
                        if not any(any(cell for cell in row) for row in table_rows):
                            continue
                        block_metadata = {
                            **metadata,
                            "table_block_index": table_index,
                            "table_block_count": len(_split_table_rows(rows, EXCEL_TABLE_BLOCK_MAX_ROWS)),
                        }
                        blocks.append(
                            _block(
                                request,
                                file_path,
                                page_no=page_no,
                                block_type="table",
                                content="\n".join("\t".join(row).rstrip() for row in table_rows if any(row)),
                                section_title=sheet_title,
                                markdown=_table_markdown(table_rows),
                                html=None,
                                confidence=1.0,
                                metadata=block_metadata,
                                block_no=len(blocks) + 1,
                            )
                        )
                except Exception as exc:
                    errors.append(_page_error(page_no, exc, {"sheet_path": sheet_name}))
        return _result(request, "ExcelParser", pages, blocks, errors)
