from __future__ import annotations
"""POST /api/v1/datasources/*"""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select, or_
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Datasource
from app.utils import safe_int

router = APIRouter(prefix="/api/v1/datasources", tags=["datasources"])

TYPES_WITH_DEPT = {"primarytriage", "leveldepart"}


def _fmt_ds(ds: Datasource) -> dict:
    info = {"id": ds.id, "name": ds.name, "type": ds.type,
            "departmentid": ds.departmentid or [],
            "screenid": ds.screenid,
            "screensplitid": ds.screensplitid or [],
            "queue": ds.queue or [],
            "consultingroomname": ds.consultingroomname or [],
            "windowid": ds.windowid or [],
            "pharmacydeptno": ds.pharmacydeptno,
            "pharmacywinno": ds.pharmacywinno or [],
            "morningcleartime": ds.morningcleartime,
            "afternooncleartime": ds.afternooncleartime}
    if ds.pharmacydeptno and ds.pharmacywinno:
        info["pharmacy"] = f"{ds.pharmacydeptno}_{','.join(str(x) for x in ds.pharmacywinno)}"
    return info


@router.post("/query")
async def query(body: dict, db: AsyncSession = Depends(get_db)):
    stmt = select(Datasource)
    if body.get("name"):
        stmt = stmt.where(Datasource.name.ilike(f"%{body['name']}%"))
    stmt = stmt.order_by(Datasource.type)
    result = await db.execute(stmt)
    return {"errcode": 0, "result": [_fmt_ds(d) for d in result.scalars().all()]}


@router.post("/remove")
async def remove(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Datasource).where(Datasource.id == safe_int(id)))
    ds = result.scalars().first()
    if ds:
        await db.delete(ds)
        await db.commit()
    return {"errcode": 0}


@router.post("/save")
async def save(body: dict, db: AsyncSession = Depends(get_db)):
    now = datetime.now(timezone.utc)

    # 统一解析数组字段：兼容前端发来的数组或字符串
    def _parse_int_list(val) -> list[int]:
        if isinstance(val, list):
            return [int(x) for x in val if str(x).lstrip("-").isdigit()]
        if isinstance(val, str) and val.strip():
            return [int(x.strip()) for x in val.split(",") if x.strip().lstrip("-").isdigit()]
        return []

    def _parse_str_list(val) -> list[str]:
        if isinstance(val, list):
            return [str(x) for x in val if x is not None]
        if isinstance(val, str) and val.strip():
            return [x.strip() for x in val.split(",") if x.strip()]
        return []

    departmentid = _parse_int_list(body.get("departmentid"))
    screensplitid = _parse_int_list(body.get("screensplitid"))
    windowid = _parse_int_list(body.get("windowid"))
    pharmacywinno = _parse_int_list(body.get("pharmacywinno"))
    queue = _parse_str_list(body.get("queue"))
    consultingroomname = _parse_str_list(body.get("consultingroomname")) or None
    ds_type = body.get("type", "")

    # Uniqueness checks — departmentid/JSONB 字段用 Python 列表比对
    if ds_type in TYPES_WITH_DEPT and departmentid:
        r = await db.execute(select(Datasource).where(Datasource.type == ds_type))
        for exist in r.scalars().all():
            if exist.id != safe_int(body.get("id", 0)) and (exist.departmentid or []) == departmentid:
                raise HTTPException(400, "该科室数据源已存在")
    elif ds_type == "secondarytriage" and body.get("screenid"):
        r = await db.execute(select(Datasource).where(Datasource.type == "secondarytriage", Datasource.screenid == body["screenid"]))
        dup = r.scalars().first()
        if dup and dup.id != safe_int(body.get("id", 0)):
            raise HTTPException(400, "该屏ID相关数据源已存在")

    doc_id = body.get("id")
    if doc_id:
        result = await db.execute(select(Datasource).where(Datasource.id == safe_int(doc_id)))
        ds = result.scalars().first()
        if ds:
            for k, v in [("name", body.get("name")), ("type", ds_type), ("departmentid", departmentid),
                         ("screenid", body.get("screenid")), ("screensplitid", screensplitid),
                         ("queue", queue), ("consultingroomname", consultingroomname),
                         ("windowid", windowid), ("pharmacydeptno", body.get("pharmacydeptno")),
                         ("pharmacywinno", pharmacywinno), ("morningcleartime", body.get("morningcleartime")),
                         ("afternooncleartime", body.get("afternooncleartime"))]:
                setattr(ds, k, v)
            ds.ut = now
            await db.commit()
    else:
        ds = Datasource(
            name=body.get("name", ""), type=ds_type, departmentid=departmentid,
            screenid=body.get("screenid"), screensplitid=screensplitid,
            queue=queue, consultingroomname=consultingroomname,
            windowid=windowid, pharmacydeptno=body.get("pharmacydeptno"),
            pharmacywinno=pharmacywinno, morningcleartime=body.get("morningcleartime"),
            afternooncleartime=body.get("afternooncleartime"), ct=now, ut=now,
        )
        db.add(ds)
        await db.commit()
    return {"errcode": 0}
