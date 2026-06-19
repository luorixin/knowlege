import logging
from pathlib import Path
from typing import Optional

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
            # Handle PDF extraction
            import fitz  # PyMuPDF
            file_path, page_fragment = image_uri.split("#page-")
            page_no = int(page_fragment) - 1  # 0-indexed in fitz
            
            doc = fitz.open(file_path)
            if page_no < 0 or page_no >= len(doc):
                logger.error(f"Page number {page_no + 1} out of bounds for {file_path}")
                return None
            
            page = doc.load_page(page_no)
            # Render at ~150 DPI for OCR (scale=2.0)
            pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
            
            # Convert fitz pixmap to numpy array (OpenCV format BGR)
            img_array = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, pix.n)
            
            if pix.n == 4:
                # Convert RGBA to BGR
                img_array = cv2.cvtColor(img_array, cv2.COLOR_RGBA2BGR)
            elif pix.n == 3:
                # Convert RGB to BGR
                img_array = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)
            elif pix.n == 1:
                # Grayscale to BGR
                img_array = cv2.cvtColor(img_array, cv2.COLOR_GRAY2BGR)
                
            return img_array
            
        else:
            # Handle standard local image file
            path = Path(image_uri)
            if not path.exists():
                logger.error(f"Image not found: {image_uri}")
                return None
            
            img_array = cv2.imread(str(path))
            return img_array
            
    except Exception as exc:
        logger.exception(f"Failed to extract image from {image_uri}")
        return None
