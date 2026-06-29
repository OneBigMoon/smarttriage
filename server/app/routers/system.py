from __future__ import annotations
"""POST /api/v1/system, /system/save"""
from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db, get_redis
from app.models import SystemConfig

router = APIRouter(prefix="/api/v1/system", tags=["system"])


@router.post("")
async def get_system(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(SystemConfig).limit(1))
    sys = result.scalars().first()
    if not sys:
        return {"errcode": 0, "result": {}}
    return {"errcode": 0, "result": {
        "url": sys.url, "primarytablename": sys.primarytablename,
        "secondarytablename": sys.secondarytablename, "secondarycounttablename": sys.secondarycounttablename,
        "drawbloodtablename": sys.drawbloodtablename, "pharmacytablename": sys.pharmacytablename,
        "username": sys.username, "password": sys.password,
        "pacssecondarytablename": sys.pacssecondarytablename,
        "pacsusername": sys.pacsusername, "pacspassword": sys.pacspassword,
    }}


@router.post("/save")
async def save_system(body: dict, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(SystemConfig).limit(1))
    sys = result.scalars().first()
    data = {k: v for k, v in body.items() if v is not None}
    if sys:
        for k, v in data.items():
            if hasattr(sys, k):
                setattr(sys, k, v)
    else:
        sys = SystemConfig(**data)
        db.add(sys)
    await db.commit()
    # Sync to Redis for Oracle connection pool
    await get_redis().hset("system", mapping={k: str(v) for k, v in data.items() if v is not None})
    return {"errcode": 0}
