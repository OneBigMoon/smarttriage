from __future__ import annotations
"""POST /api/v1/templates/* — Template CRUD + assignment to boxes."""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Template, Box
from app.utils import safe_int

router = APIRouter(prefix="/api/v1/templates", tags=["templates"])


def _fmt(t: Template) -> dict:
    return {
        "id": t.id, "name": t.name, "key": t.key, "kind": t.kind,
        "html": t.html, "css": t.css, "js": t.js,
        "logo": t.logo, "version": t.version,
        "params": t.params, "manifest": t.manifest,
    }


@router.post("/query")
async def query(body: dict = {}, db: AsyncSession = Depends(get_db)):
    stmt = select(Template)
    if body.get("name"):
        stmt = stmt.where(Template.name.ilike(f"%{body['name']}%"))
    if body.get("kind"):
        stmt = stmt.where(Template.kind == body["kind"])
    result = await db.execute(stmt.order_by(Template.ut.desc()))
    return {"errcode": 0, "result": [_fmt(t) for t in result.scalars().all()]}


@router.post("/save")
async def save(body: dict, db: AsyncSession = Depends(get_db)):
    now = datetime.now(timezone.utc)
    doc_id = body.get("id")
    if doc_id:
        result = await db.execute(select(Template).where(Template.id == safe_int(doc_id)))
        t = result.scalars().first()
        if not t:
            raise HTTPException(404, "template not found")
        for k in ("name", "key", "kind", "html", "css", "js", "logo", "version", "params", "manifest"):
            if k in body:
                setattr(t, k, body[k])
        t.ut = now
        await db.commit()
    else:
        if body.get("key"):
            r = await db.execute(select(Template).where(Template.key == body["key"]))
            if r.scalars().first():
                raise HTTPException(400, "template key already exists")
        t = Template(
            name=body.get("name", ""), key=body.get("key", ""),
            kind=body.get("kind", "web"),
            html=body.get("html", ""), css=body.get("css", ""), js=body.get("js", ""),
            logo=body.get("logo"), version=body.get("version", "1.0.0"),
            params=body.get("params"), manifest=body.get("manifest"),
            ct=now, ut=now,
        )
        db.add(t)
        await db.commit()
    return {"errcode": 0}


@router.post("/remove")
async def remove(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Template).where(Template.id == safe_int(id)))
    t = result.scalars().first()
    if t:
        await db.delete(t)
        await db.commit()
    return {"errcode": 0}


@router.post("/assign")
async def assign(box_id: str, template_id: str, db: AsyncSession = Depends(get_db)):
    from app.services.socketio_handler import _send_config
    result = await db.execute(select(Box).where(Box.id == safe_int(box_id)))
    box = result.scalars().first()
    if not box:
        raise HTTPException(404, "box not found")
    box.template = template_id if template_id else None
    box.ut = datetime.now(timezone.utc)
    await db.commit()
    if box.status == "正常":
        await _send_config(None, box.no)
    return {"errcode": 0}


@router.post("/get-for-box")
async def get_for_box(body: dict, db: AsyncSession = Depends(get_db)):
    box_id = body.get("box_id")
    if not box_id:
        raise HTTPException(400, "box_id required")
    result = await db.execute(select(Box).where(Box.id == safe_int(box_id)))
    box = result.scalars().first()
    if not box or not box.template:
        return {"errcode": 0, "result": None}
    r = await db.execute(select(Template).where(Template.id == int(box.template)))
    t = r.scalars().first()
    return {"errcode": 0, "result": _fmt(t) if t else None}


@router.get("/serve/{template_id}")
async def serve(template_id: str, db: AsyncSession = Depends(get_db)):
    from fastapi.responses import HTMLResponse
    result = await db.execute(select(Template).where(Template.id == safe_int(template_id)))
    t = result.scalars().first()
    if not t:
        raise HTTPException(404, "template not found")
    if t.kind != "web":
        raise HTTPException(400, "not a web template")
    css_tag = f"<style>{t.css}</style>" if t.css else ""
    js_tag = f"<script>{t.js}</script>" if t.js else ""
    html = f"<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>{css_tag}</head><body>{t.html}{js_tag}</body></html>"
    return HTMLResponse(content=html)
