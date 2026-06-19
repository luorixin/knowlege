import logging
import posixpath
from pathlib import Path
from typing import Optional
from xml.etree import ElementTree
import zipfile

import cv2
import numpy as np

logger = logging.getLogger(__name__)


def extract_image_from_uri(image_uri: str) -> Optional[np.ndarray]:
    """
    Extract an image array from a URI.
    Supports local image files and PDF page fragments (e.g., file.pdf#page-1).
    """
    try:
        if "#page-" in image_uri:
            return _extract_pdf_page(image_uri)
        if "#slide-" in image_uri:
            return _extract_ppt_slide_image(image_uri)

        # Handle standard local image file.
        path = Path(image_uri)
        if not path.exists():
            logger.error(f"Image not found: {image_uri}")
            return None

        img_array = cv2.imread(str(path))
        return img_array

    except Exception as exc:
        logger.exception(f"Failed to extract image from {image_uri}")
        return None


def _extract_pdf_page(image_uri: str) -> Optional[np.ndarray]:
    import fitz  # PyMuPDF

    file_path, page_fragment = image_uri.split("#page-")
    page_no = int(page_fragment) - 1

    doc = fitz.open(file_path)
    if page_no < 0 or page_no >= len(doc):
        logger.error(f"Page number {page_no + 1} out of bounds for {file_path}")
        return None

    page = doc.load_page(page_no)
    pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
    img_array = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, pix.n)

    if pix.n == 4:
        return cv2.cvtColor(img_array, cv2.COLOR_RGBA2BGR)
    if pix.n == 3:
        return cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)
    if pix.n == 1:
        return cv2.cvtColor(img_array, cv2.COLOR_GRAY2BGR)
    return img_array


def _extract_ppt_slide_image(image_uri: str) -> Optional[np.ndarray]:
    file_path, slide_fragment = image_uri.split("#slide-")
    slide_no = int(slide_fragment)
    slide_path = f"ppt/slides/slide{slide_no}.xml"
    rels_path = f"ppt/slides/_rels/slide{slide_no}.xml.rels"

    with zipfile.ZipFile(file_path) as archive:
        try:
            rels_root = ElementTree.fromstring(archive.read(rels_path))
        except KeyError:
            logger.warning(f"PPT slide relationship file not found: {rels_path}")
            return None

        for relationship in rels_root.iter():
            if _local_name(relationship.tag) != "Relationship":
                continue
            rel_type = relationship.attrib.get("Type", "")
            target = relationship.attrib.get("Target", "")
            if "image" not in rel_type or not target:
                continue
            image_path = posixpath.normpath(posixpath.join(posixpath.dirname(slide_path), target))
            try:
                image_bytes = archive.read(image_path)
            except KeyError:
                logger.warning(f"PPT embedded image not found: {image_path}")
                continue
            return cv2.imdecode(np.frombuffer(image_bytes, dtype=np.uint8), cv2.IMREAD_COLOR)

    logger.warning(f"No embedded image found for PPT slide URI: {image_uri}")
    return None


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1] if "}" in tag else tag
