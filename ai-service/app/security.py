"""Only the Spring backend may call this service.

Spring sends a secret in the X-Internal-Key header. The browser never talks to
this service directly, so students can never reach it without going through
Spring's login and permission checks.
"""
import secrets

from fastapi import HTTPException, Security
from fastapi.security import APIKeyHeader

from app.config import settings

_header = APIKeyHeader(name="X-Internal-Key", auto_error=False)


def require_internal_key(key: str | None = Security(_header)) -> None:
    # compare_digest avoids leaking the key through response timing.
    if not key or not secrets.compare_digest(key, settings.internal_api_key):
        raise HTTPException(status_code=401, detail="invalid_internal_key")
