"""Shared FastAPI dependencies — auth, DB session, etc."""
from __future__ import annotations

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.services.auth import decode_token

_bearer = HTTPBearer(auto_error=False)

# Paths that don't require auth
SKIP_AUTH_PATHS = {
    "/api/v1/user/login",
    "/api/v1/user/logout",
    "/api/health",
    "/api/v1/system/health",
    "/docs",
    "/openapi.json",
    "/redoc",
}

# Path prefixes that don't require auth (dynamic paths like /api/v1/templates/serve/123)
# 旧版安卓终端无 token 认证，必须公开这些路径
SKIP_AUTH_PREFIXES = {
    "/api/v1/templates/serve/",
    "/api/v1/boxes/download-log",
    "/api/v1/terminal/",
    "/api/v1/upgrade/download",    # old Android: GET download
}

# Exact paths that bypass auth (for old Android terminal backward compatibility)
SKIP_AUTH_PATHS.update({
    "/api/v1/upgrade",              # old Android: POST check version
    "/api/v1/upload",               # old Android: POST upload log
})


async def get_current_user(
    cred: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> dict:
    """JWT auth dependency — validates token and returns payload."""
    if cred is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="未登录",
            headers={"WWW-Authenticate": "Bearer"},
        )
    payload = decode_token(cred.credentials)
    if payload is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="token无效或已过期",
        )
    return payload


async def get_db_session() -> AsyncSession:
    """Yields DB session — alias for clarity in route signatures."""
    async for session in get_db():
        yield session
