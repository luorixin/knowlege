import os

# Set configuration via environment variables
os.environ["VLM_PROVIDER"] = "openai"
# For this script to work, you must have AI_LLM_ENDPOINT and either LLM_API_KEY, OPENAI_API_KEY, or QWEN_API_KEY exported in your environment.

from app.schemas.document_parse import DocumentParseRequest
from app.api.parse import parser

def test_vlm_parsing():
    print("Testing Real VLM Integration (OpenAI Vision)...")
    
    # Use one of the existing screenshots from the project to test image VLM
    test_image_path = os.path.abspath("../sticth/stitch_enterprise_ai_knowledge_hub/screen.png")
    
    if not os.path.exists(test_image_path):
        print(f"Test image not found at {test_image_path}")
        return

    req = DocumentParseRequest(
        doc_id="test_doc_vlm_1",
        version_id="test_version_vlm_1",
        file_path=test_image_path,
        file_type="png"  # Using png file_type will route to ImageParser which uses VLMCaptionProvider
    )
    
    try:
        response = parser.parse(req)
        print("\n--- VLM Parse Successful ---")
        print(f"Status: {response.status}")
        print(f"Parsed Pages: {len(response.pages)}")
        
        if response.pages:
            print("\nExtracted VLM Caption:")
            print("-" * 60)
            print(response.pages[0].content)
            print("-" * 60)
            
    except Exception as e:
        print(f"Failed to parse document with VLM: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_vlm_parsing()
