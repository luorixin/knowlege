class DocumentParseError(Exception):
    def __init__(self, code: str, message: str, status_code: int = 400) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


class OcrProviderError(Exception):
    """Raised by an OCR provider when recognition genuinely fails.

    Unlike ``DocumentParseError``, this is a low-level capability error and
    carries no HTTP status code: the ``OCRParser`` decides how to translate it
    into a page-level ``PageParseError``. ``code`` lets callers distinguish
    failure modes (e.g. ``IMAGE_EXTRACT_FAILED`` vs ``OCR_RECOGNIZE_FAILED``).
    """

    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
