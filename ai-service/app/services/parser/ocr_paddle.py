import logging
from typing import Any

from app.services.parser.base import OcrProvider, OcrResult
from app.services.parser.errors import OcrProviderError
from app.services.parser.image_utils import extract_image_from_uri

logger = logging.getLogger(__name__)


class PaddleOcrProvider(OcrProvider):
    def __init__(self):
        try:
            from paddleocr import PaddleOCR
            # Initialize PaddleOCR
            # use_angle_cls=True helps with rotated text
            # lang='ch' supports Chinese and English
            self.ocr = PaddleOCR(use_angle_cls=True, lang="ch")
            logger.info("PaddleOCR initialized successfully.")
        except ImportError as exc:
            logger.error("PaddleOCR is not installed. Please install paddleocr and paddlepaddle.")
            raise RuntimeError("PaddleOCR dependencies missing") from exc
        except Exception as exc:
            logger.error(f"Failed to initialize PaddleOCR: {exc}")
            raise RuntimeError("PaddleOCR initialization failed") from exc

    def recognize_page(self, image_uri: str, page_no: int, metadata: dict[str, Any] | None = None) -> OcrResult:
        """
        Recognize text from a page image or image-like PDF page using PaddleOCR.
        """
        logger.info(f"Running PaddleOCR on {image_uri}")
        img_array = extract_image_from_uri(image_uri)

        if img_array is None:
            # Rasterisation / file read failed. Raise so the OCRParser records
            # a page-level error instead of silently treating the page as an
            # empty-but-successful parse (which would pollute the vector DB).
            raise OcrProviderError(
                "IMAGE_EXTRACT_FAILED",
                f"Failed to extract image array for {image_uri}",
            )

        try:
            # Run OCR
            result = self.ocr.ocr(img_array)
            
            if not result or not result[0]:
                return OcrResult(text="", confidence=0.0, provider="paddle")

            lines = []
            confidences = []
            # In paddleocr>=3.0 / paddlex, result is a list of dicts.
            # Older versions return a list of lists of bounding boxes and texts.
            for res in result:
                if not res:
                    continue
                # PaddleX style dict format
                if isinstance(res, dict):
                    if 'rec_texts' in res:
                        texts = res['rec_texts']
                        if isinstance(texts, list):
                            lines.extend([str(t) for t in texts if t])
                    scores = res.get('rec_scores')
                    if isinstance(scores, list):
                        confidences.extend(float(score) for score in scores if score is not None)
                # Older PaddleOCR list format
                elif isinstance(res, list):
                    for line in res:
                        if isinstance(line, (list, tuple)) and len(line) >= 2:
                            text_info = line[1]
                            if isinstance(text_info, (list, tuple)) and len(text_info) > 0:
                                lines.append(str(text_info[0]))
                                if len(text_info) > 1:
                                    confidences.append(float(text_info[1]))
                            else:
                                lines.append(str(text_info))
            
            confidence = sum(confidences) / len(confidences) if confidences else None
            return OcrResult(
                text="\n".join(lines),
                confidence=confidence,
                provider="paddle",
                language="ch",
                metadata={"line_count": len(lines)},
            )
            
        except OcrProviderError:
            raise
        except Exception as exc:
            # OCR execution failed (model error, corrupt page render, etc.).
            # Surface it: returning "" would make this page look like a
            # successfully parsed empty page and silently enter the corpus.
            logger.exception(f"PaddleOCR failed for {image_uri}")
            raise OcrProviderError(
                "OCR_RECOGNIZE_FAILED",
                f"PaddleOCR recognition failed for {image_uri}: {exc}",
            ) from exc
