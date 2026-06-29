from __future__ import annotations
"""POST /api/v1/orgnizations/*"""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Organization
from app.utils import safe_int

router = APIRouter(prefix="/api/v1/orgnizations", tags=["organizations"])


def _build_tree(orgs: list) -> list:
    """Build tree from flat list. Handles cycles and orphaned nodes."""
    org_map = {}
    for o in orgs:
        org_map[o.id] = {"id": o.id, "name": o.name, "parentid": o.parentid, "children": []}
    # Detect cycles: if parentid chain leads back to self, treat as root
    roots = []
    for o in orgs:
        node = org_map[o.id]
        pid = o.parentid
        # Check for cycle: parent doesn't exist, or parent is self
        if pid is None or pid == o.id or pid not in org_map:
            roots.append(node)
        else:
            org_map[pid]["children"].append(node)
    return roots


@router.post("/query")
async def query(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Organization).order_by(Organization.id))
    orgs = result.scalars().all()
    return {"errcode": 0, "result": _build_tree(orgs)}


@router.post("/save")
async def save(body: dict, db: AsyncSession = Depends(get_db)):
    now = datetime.now(timezone.utc)
    doc_id = body.get("id")
    if doc_id is not None:
        result = await db.execute(select(Organization).where(Organization.id == safe_int(doc_id)))
        o = result.scalars().first()
        if o:
            o.name = body.get("name", o.name)
            o.ut = now
            await db.commit()
    elif "parentid" in body:
        parent_id = body["parentid"]
        parent = None
        if parent_id is not None:
            r = await db.execute(select(Organization).where(Organization.id == safe_int(parent_id)))
            parent = r.scalars().first()
        o = Organization(name=body.get("name", ""),
                         parentid=parent_id, ct=now, ut=now)
        db.add(o)
        await db.commit()
        await db.refresh(o)
        o.idpath = f"{parent.idpath}{o.id}." if parent else f"{o.id}."
        await db.commit()
    return {"errcode": 0}


@router.post("/remove")
async def remove(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Organization).where(Organization.id == safe_int(id)))
    org = result.scalars().first()
    if org:
        escaped = org.idpath.replace(".", r"\.")
        r = await db.execute(select(Organization).where(Organization.idpath.op("~")(f"^{escaped}")))
        for child in r.scalars().all():
            await db.delete(child)
        await db.commit()
    return {"errcode": 0}
