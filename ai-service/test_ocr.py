import os
import sys

# Set configuration via environment variables
os.environ["OCR_PROVIDER"] = "paddle"

from app.schemas.document_parse import DocumentParseRequest
from app.api.parse import parser

def test_ocr_parsing():
    print("Testing Real OCR Integration (PaddleOCR)...")
    
    # Use one of the existing screenshots from the project to test image OCR
    test_image_path = os.path.abspath("../sticth/stitch_enterprise_ai_knowledge_hub/screen.png")
    
    if not os.path.exists(test_image_path):
        print(f"Test image not found at {test_image_path}")
        return

    from app.services.parser.ocr_paddle import PaddleOcrProvider
    ocr_provider = PaddleOcrProvider()
    
    print("\nExtracting text from image...")
    text = ocr_provider.recognize_page(test_image_path, 1)
    
    print("\n--- OCR Extraction Successful ---")
    print(text)

if __name__ == "__main__":
    test_ocr_parsing()
