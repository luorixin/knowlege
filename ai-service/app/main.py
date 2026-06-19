import logging

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api import parse
from app.api.v1.router import api_router

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    app = FastAPI(
        title="Knowledge AI Service",
        version="0.1.0",
        description="Document processing and model abstraction service for the enterprise knowledge agent.",
    )
    app.include_router(parse.router, prefix="/api")
    app.include_router(parse.router, prefix="/api/v1")
    app.include_router(api_router, prefix="/api/v1")

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request, exc):
        body_preview = ""
        if request.url.path == "/api/v1/parse/document":
            body = await request.body()
            body_preview = body.decode("utf-8", errors="replace")[:1000]
        logger.warning(
            "Request validation failed path=%s errors=%s body=%s",
            request.url.path,
            exc.errors(),
            body_preview,
        )
        return JSONResponse(status_code=422, content={"detail": exc.errors()})

    return app


app = create_app()
