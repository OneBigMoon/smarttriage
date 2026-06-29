"""终端面向的 HTTP API — 兼容旧版 (triage/) 和新版 (android/) 安卓终端。

所有终端 HTTP 请求统一走这个模块。管理端 API 在对应 router 里。

旧安卓直接调用的遗留路径（如 /api/v1/upgrade、/api/v1/upload）作为别名保留，
在对应 router 里调用此模块的同名函数，确保行为一致。
"""
from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Style, Datasource

router = APIRouter(prefix="/api/v1/terminal", tags=["terminal"])


# ── 对外：获取终端可用样式 ──
# 旧安卓: POST /api/v1/terminal/querystyles  → 返回 [{key, name}]
# 管理端: POST /api/v1/styles/query           → 返回 [{id, key, name}]

async def get_terminal_styles(db: AsyncSession) -> list[dict]:
    """核心逻辑：查询所有样式，给终端用（只需 key + name）。"""
    result = await db.execute(select(Style))
    styles = result.scalars().all()
    if not styles:
        from app.routers.simple_queries import DEFAULT_STYLES
        return DEFAULT_STYLES
    return [{"key": s.key, "name": s.name} for s in styles]


@router.post("/querystyles")
async def query_terminal_styles(db: AsyncSession = Depends(get_db)):
    """终端获取可用样式列表 — 公开（无 token）。"""
    return {"errcode": 0, "result": await get_terminal_styles(db)}


# ── 对外：获取终端可用数据源 ──
# 旧安卓: POST /api/v1/terminal/querydatasources → 返回 [{_id, name}]
# 管理端: POST /api/v1/datasources/query         → 返回 [{id, name, type, ...}]

async def get_terminal_datasources(db: AsyncSession) -> list[dict]:
    """核心逻辑：查询所有数据源，给终端用（需要 _id + name）。

    旧安卓 Android 端反序列化时硬编码了 _id 字段名（MongoDB 遗留），
    同时保留 id 字段给新版可能使用。
    """
    result = await db.execute(select(Datasource).order_by(Datasource.id))
    return [{"_id": str(ds.id), "id": ds.id, "name": ds.name} for ds in result.scalars().all()]


@router.post("/querydatasources")
async def query_terminal_datasources(db: AsyncSession = Depends(get_db)):
    """终端获取数据源列表 — 公开（无 token）。"""
    return {"errcode": 0, "result": await get_terminal_datasources(db)}


# ── 屏幕预览（终端画面复用） ──
# 此接口管理端和终端都用：管理端在网页上预览，终端可扫码查看自身画面
# 已实现于 routers/boxes.py: GET /api/v1/boxes/preview/{box_id}
