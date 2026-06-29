from __future__ import annotations
"""Smart Triage — FastAPI + Socket.IO entry point."""
import os
from contextlib import asynccontextmanager

import socketio
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from loguru import logger
from passlib.context import CryptContext
from sqlalchemy import select

from app.config import settings
from app.database import connect_db, close_db, connect_redis, close_redis, async_session
from app.dependencies import SKIP_AUTH_PATHS, SKIP_AUTH_PREFIXES
from app.models import User
from app.services.auth import decode_token
from app.services.oracle_sync import start_scheduler, stop_scheduler
from app.services.socketio_handler import sio

pwd_ctx = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Smart Triage server...")
    await connect_db()
    await connect_redis()
    # Default admin
    async with async_session() as db:
        result = await db.execute(select(User).where(User.username == "admin"))
        if not result.scalars().first():
            from datetime import datetime, timezone
            u = User(username="admin", hashedPassword=pwd_ctx.hash("admin123"),
                     fullname="管理员", auths=["AUTH_ROOT"],
                     ct=datetime.now(timezone.utc), ut=datetime.now(timezone.utc))
            db.add(u)
            await db.commit()
            logger.info("Default admin created (admin/admin123)")
    start_scheduler()
    yield
    stop_scheduler()
    await close_redis()
    await close_db()


app = FastAPI(title="Smart Triage", version="3.0.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=settings.CORS_ORIGINS, allow_credentials=True,
                   allow_methods=["*"], allow_headers=["*"])


# ── Global auth middleware ──
@app.middleware("http")
async def auth_middleware(request: Request, call_next):
    """Protect all /api/* routes except login/health/docs + public prefixes."""
    path = request.url.path
    if path.startswith("/api/") and path not in SKIP_AUTH_PATHS and not any(path.startswith(p) for p in SKIP_AUTH_PREFIXES):
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return JSONResponse(status_code=401, content={"detail": "未登录"})
        token = auth_header[7:]
        from app.services.auth import decode_token
        payload = decode_token(token)
        if payload is None:
            return JSONResponse(status_code=401, content={"detail": "token无效或已过期"})
        # Attach user info to request state
        request.state.user = payload
    response = await call_next(request)
    return response


# ── API routers ──
from app.routers import auth, boxes, datasources, organizations, simple_queries, system, upgrades, uploads, templates, apk, discovery, terminal
for r in (auth, boxes, datasources, organizations, simple_queries, system, upgrades, uploads, templates, apk, discovery, terminal):
    app.include_router(r.router)


# ── 打包模式：前端静态文件服务 ──

_www_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "web", "dist")
if os.path.isdir(_www_dir):
    # 生产模式（打包后）—— 由后端直接托管前端静态文件
    from fastapi.responses import FileResponse
    import mimetypes
    mimetypes.init()

    _assets = os.path.join(_www_dir, "assets")
    if os.path.isdir(_assets):
        app.mount("/assets", StaticFiles(directory=_assets), name="assets")

    @app.get("/")
    async def serve_index():
        return FileResponse(os.path.join(_www_dir, "index.html"))

    @app.exception_handler(404)
    async def spa_fallback(request: Request, exc):
        """SPA 路由：所有非 API 路径返回 index.html"""
        if not request.url.path.startswith("/api/") and not request.url.path.startswith("/socket.io"):
            return FileResponse(os.path.join(_www_dir, "index.html"))
        return JSONResponse(status_code=404, content={"detail": "Not found"})

    _port_env = os.environ.get("PORT") or str(settings.PORT)
    _open_url = f"http://localhost:{_port_env}/"
    logger.info(f"生产模式启动: {_open_url}")


@app.get("/api/health")
async def health():
    return {"status": "ok", "version": "3.0.0"}


@app.get("/api/v1/system/health")
async def system_health():
    """系统健康检查 — 各组件连接状态"""
    import socket
    from app.database import get_redis, engine
    from sqlalchemy import text

    checks = {}

    # PostgreSQL
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        checks["postgres"] = {"status": "ok", "label": "PostgreSQL"}
    except Exception as e:
        checks["postgres"] = {"status": "error", "label": "PostgreSQL", "msg": str(e)[:50]}

    # Redis
    try:
        await get_redis().ping()
        checks["redis"] = {"status": "ok", "label": "Redis"}
    except Exception as e:
        checks["redis"] = {"status": "error", "label": "Redis", "msg": str(e)[:50]}

    # Oracle (with timeout)
    try:
        import asyncio
        from app.services import oracle_pool
        await asyncio.wait_for(oracle_pool.query("SELECT 1 FROM DUAL"), timeout=5.0)
        checks["oracle"] = {"status": "ok", "label": "Oracle"}
    except asyncio.TimeoutError:
        checks["oracle"] = {"status": "error", "label": "Oracle", "msg": "连接超时"}
    except Exception as e:
        checks["oracle"] = {"status": "error", "label": "Oracle", "msg": str(e)[:50]}

    # Socket.IO (check if Redis adapter is working)
    try:
        from app.services.socketio_handler import sio
        checks["socketio"] = {"status": "ok", "label": "Socket.IO"}
    except Exception as e:
        checks["socketio"] = {"status": "error", "label": "Socket.IO", "msg": str(e)[:50]}

    return {"errcode": 0, "result": checks}


# ── Mount Socket.IO ──
socket_app = socketio.ASGIApp(sio, other_asgi_app=app)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:socket_app", host=settings.HOST, port=settings.PORT, reload=True)
