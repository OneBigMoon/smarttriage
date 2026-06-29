from __future__ import annotations
"""Simple query-only routers: styles, datasourcetypes."""
from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Style, DatasourceType

router = APIRouter(prefix="/api/v1", tags=["simple"])

DEFAULT_STYLES = [
    {"key": "primarytriage", "name": "一级分诊"}, {"key": "secondarytriage", "name": "二级分诊"},
    {"key": "secondarytriagesplit", "name": "二级分诊(分屏)"}, {"key": "secondarytriageultrasonic", "name": "二级超声分诊"},
    {"key": "drawbloodtriage", "name": "检验分诊"}, {"key": "primarypharmacytriage", "name": "药房一级分诊"},
    {"key": "secondarypharmacytriage", "name": "药房二级分诊"}, {"key": "leveldepart", "name": "层级科室"},
]


@router.post("/styles/query")
async def query_styles(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Style))
    styles = result.scalars().all()
    if not styles:
        return {"errcode": 0, "result": DEFAULT_STYLES}
    return {"errcode": 0, "result": [{"id": s.id, "name": s.name, "key": s.key} for s in styles]}


@router.post("/datasourcetypes/query")
async def query_types(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(DatasourceType))
    types = result.scalars().all()
    if not types:
        return {"errcode": 0, "result": DEFAULT_STYLES}
    return {"errcode": 0, "result": [{"id": t.id, "name": t.name, "key": t.key} for t in types]}
