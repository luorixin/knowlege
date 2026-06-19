import base64
import logging
from typing import Any

import cv2
import httpx
from fastapi import HTTPException

from app.config import get_settings
from app.services.parser.base import VLMCaptionProvider, VLMCaptionResult
from app.services.parser.image_utils import extract_image_from_uri

logger = logging.getLogger(__name__)


class OpenAIVLMCaptionProvider(VLMCaptionProvider):
    def __init__(self):
        settings = get_settings()
        self.api_key = settings.llm_api_key or settings.openai_api_key or settings.qwen_api_key
        self.endpoint = (settings.ai_vlm_endpoint or settings.ai_llm_endpoint).rstrip("/")
        self.model_name = settings.vlm_model_name
        self.temperature = settings.vlm_temperature
        self.timeout = settings.ai_request_timeout

        if not self.endpoint:
            logger.error("OpenAI VLM provider initialized but endpoint is missing (ai_vlm_endpoint or ai_llm_endpoint). VLM requests will likely fail.")
        if not self.api_key:
            logger.error("OpenAI VLM provider initialized but API key is missing. VLM requests will likely fail.")
        if not self.model_name:
            logger.error("OpenAI VLM provider initialized but model_name is missing. VLM requests will likely fail.")

    def caption(self, image_uri: str, metadata: dict[str, Any] | None = None) -> VLMCaptionResult:
        """
        Extracts the image from the given URI, converts it to base64,
        and requests a caption from an OpenAI-compatible Vision API.
        """
        logger.info(f"Generating VLM caption for {image_uri} using {self.model_name}")
        
        # 1. Extract image array using existing utility
        img_array = extract_image_from_uri(image_uri)
        if img_array is None:
            logger.warning(f"Failed to extract image for VLM: {image_uri}")
            return VLMCaptionResult(
                caption="",
                provider="openai-compatible",
                model_name=self.model_name,
                error_code="IMAGE_EXTRACT_FAILED",
                error_message=f"Failed to extract image for VLM: {image_uri}",
            )

        # 2. Convert numpy array to base64 PNG
        success, encoded_image = cv2.imencode(".png", img_array)
        if not success:
            logger.error(f"Failed to encode image to PNG: {image_uri}")
            return VLMCaptionResult(
                caption="",
                provider="openai-compatible",
                model_name=self.model_name,
                error_code="IMAGE_ENCODE_FAILED",
                error_message=f"Failed to encode image to PNG: {image_uri}",
            )
            
        base64_image = base64.b64encode(encoded_image.tobytes()).decode("utf-8")
        
        # 3. Construct prompt based on file type
        file_type = (metadata or {}).get("file_type", "")
        if file_type in ("pptx", "ppt"):
            prompt = "这是一张幻灯片(PPT)截图。请作为专业的业务知识库分析员，用中文详细总结这张幻灯片讲述的核心内容、图表信息、以及关键要点。不要遗漏任何图表中的重要数据指标。"
        else:
            prompt = "这是一张文档截图或图表。请作为专业的业务知识库分析员，用中文简明扼要地总结图中的核心内容。如果包含流程图、架构图或数据图表，请清晰描述其结构和关键数据。"

        # 4. Construct OpenAI Vision payload
        url = f"{self.endpoint}/chat/completions"
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        payload = {
            "model": self.model_name,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/png;base64,{base64_image}"
                            }
                        }
                    ]
                }
            ],
            "temperature": self.temperature,
            "max_tokens": 1000
        }

        # 5. Call API
        try:
            with httpx.Client(timeout=float(self.timeout), trust_env=False) as client:
                response = client.post(url, headers=headers, json=payload)
                response.raise_for_status()
                data = response.json()
                
                choices = data.get("choices", [])
                if choices:
                    usage = data.get("usage") or {}
                    return VLMCaptionResult(
                        caption=choices[0].get("message", {}).get("content", ""),
                        provider="openai-compatible",
                        model_name=self.model_name,
                        token_count=usage.get("total_tokens"),
                    )
                return VLMCaptionResult(caption="", provider="openai-compatible", model_name=self.model_name)
        except httpx.HTTPError as exc:
            logger.error(f"HTTPError in OpenAI VLM provider: {exc}")
            return VLMCaptionResult(
                caption="",
                provider="openai-compatible",
                model_name=self.model_name,
                error_code="VLM_HTTP_FAILED",
                error_message=str(exc),
            )
        except Exception as exc:
            logger.exception("Unexpected error in OpenAI VLM provider")
            return VLMCaptionResult(
                caption="",
                provider="openai-compatible",
                model_name=self.model_name,
                error_code="VLM_FAILED",
                error_message=str(exc),
            )
