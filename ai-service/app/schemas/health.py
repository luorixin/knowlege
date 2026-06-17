from pydantic import BaseModel


class HealthStatus(BaseModel):
    service: str
    status: str
    capabilities: list[str]
