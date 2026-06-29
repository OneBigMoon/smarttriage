from __future__ import annotations
"""POST /api/v1/upgrade/*, GET /api/v1/upgrade/download"""
import os

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Upgrade, Box
from app.services.socketio_handler import emit_to_box
from app.utils import version_to_sort, safe_int

router = APIRouter(prefix="/api/v1/upgrade", tags=["upgrades"])


@router.post("/query")
async def query(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Upgrade).order_by(Upgrade.ut.desc()))
    return {"errcode": 0, "result": result.scalars().all()}


@router.post("/remove")
async def remove(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Upgrade).where(Upgrade.id == safe_int(id)))
    up = result.scalars().first()
    if up:
        await db.delete(up)
        await db.commit()
    return {"errcode": 0}


@router.post("")
async def get_upgrade(body: dict, db: AsyncSession = Depends(get_db)):
    model = body.get("model", "")
    appversion = body.get("appversion", "")
    if not model or not appversion:
        raise HTTPException(400, "参数错误")
    sort_ver = version_to_sort(appversion)
    result = await db.execute(
        select(Upgrade).where(Upgrade.model == model, Upgrade.sortAppVersion > sort_ver)
        .order_by(Upgrade.sortAppVersion.desc()).limit(1)
    )
    up = result.scalars().first()
    return {"errcode": 0, "name": up.name if up else None, "originname": up.originname if up else None,
            "md5": up.md5 if up else None, "appversion": up.appVersion if up else None}


@router.get("/download")
async def download(name: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Upgrade).where(Upgrade.name == name))
    up = result.scalars().first()
    if not up:
        raise HTTPException(404, "not found")
    fp = os.path.join("uploads", up.path, up.name)
    if not os.path.exists(fp):
        raise HTTPException(404, "file not found")
    return FileResponse(fp, filename=up.originname)
