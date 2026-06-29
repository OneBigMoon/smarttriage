from __future__ import annotations
"""POST /api/v1/boxes/*"""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db, get_redis
from app.models import Box, Organization, Datasource, Template
from app.services.socketio_handler import emit_to_box, _send_config


def _to_int(val, field_name: str) -> int:
    """Safely convert value to int, raise 400 on failure."""
    try:
        return int(val)
    except (TypeError, ValueError):
        raise HTTPException(400, f"{field_name} 格式错误")

router = APIRouter(prefix="/api/v1/boxes", tags=["boxes"])


def _fmt_box(box: Box, org_name: str | None, ds_name: str | None, tmpl_name: str | None = None) -> dict:
    return {
        "id": box.id, "no": box.no, "name": box.name,
        "org": {"id": box.org, "name": org_name} if org_name else None,
        "ip": box.ip, "model": box.model, "appversion": box.appversion,
        "style": box.style,
        "datasource": {"id": box.datasource, "name": ds_name} if ds_name else None,
        "template": {"id": box.template, "name": tmpl_name} if tmpl_name else None,
        "status": box.status, "rotation": box.rotation,
    }


async def _resolve_org_children(db: AsyncSession, org_id: int) -> list[int]:
    """Get all org IDs under org_id (by idpath prefix)."""
    result = await db.execute(select(Organization).where(Organization.id == org_id))
    org = result.scalars().first()
    if not org or not org.idpath:
        return [org_id]
    escaped = org.idpath.replace(".", r"\.")
    result = await db.execute(
        select(Organization.id).where(Organization.idpath.op("~")(f"^{escaped}"))
    )
    return [row[0] for row in result.all()]


@router.post("/query")
async def query_boxes(body: dict, db: AsyncSession = Depends(get_db)):
    stmt = select(Box)
    if body.get("no"):
        stmt = stmt.where(Box.no.ilike(f"%{body['no']}%"))
    if body.get("name"):
        stmt = stmt.where(Box.name.ilike(f"%{body['name']}%"))
    if body.get("org") is not None and body["org"] != "all":
        org_ids = await _resolve_org_children(db, _to_int(body["org"], "分组ID"))
        stmt = stmt.where(Box.org.in_(org_ids))
    if body.get("status") and body["status"] != "all":
        stmt = stmt.where(Box.status == body["status"])
    stmt = stmt.order_by(Box.no)
    result = await db.execute(stmt)
    boxes = result.scalars().all()

    # Resolve names
    org_ids = {b.org for b in boxes if b.org is not None}
    org_names: dict = {}
    if org_ids:
        r = await db.execute(select(Organization).where(Organization.id.in_(org_ids)))
        for o in r.scalars().all():
            org_names[o.id] = o.name

    ds_ids = {b.datasource for b in boxes if b.datasource}
    ds_names: dict = {}
    if ds_ids:
        # datasource is stored as string PK
        r = await db.execute(select(Datasource).where(Datasource.id.in_([int(x) for x in ds_ids if x.isdigit()])))
        for d in r.scalars().all():
            ds_names[str(d.id)] = d.name

    tmpl_ids = {b.template for b in boxes if b.template}
    tmpl_names: dict = {}
    if tmpl_ids:
        r = await db.execute(select(Template).where(Template.id.in_([int(x) for x in tmpl_ids if x.isdigit()])))
        for t in r.scalars().all():
            tmpl_names[str(t.id)] = t.name

    return {"errcode": 0, "result": [
        _fmt_box(b, org_names.get(b.org), ds_names.get(b.datasource), tmpl_names.get(b.template))
        for b in boxes
    ]}


@router.post("/save")
async def save_box(body: dict, db: AsyncSession = Depends(get_db)):
    box_id = body.get("id")
    if not box_id:
        raise HTTPException(400, "id required")
    result = await db.execute(select(Box).where(Box.id == _to_int(box_id, "终端ID")))
    box = result.scalars().first()
    if not box:
        raise HTTPException(404, "终端不存在")

    # Validate datasource exists if changing
    if body.get("datasource"):
        ds_result = await db.execute(select(Datasource).where(Datasource.id == _to_int(body["datasource"], "数据源ID")))
        if not ds_result.scalars().first():
            raise HTTPException(400, "数据源不存在")

    # Validate template exists if changing
    if body.get("template"):
        tmpl_result = await db.execute(select(Template).where(Template.id == _to_int(body["template"], "模板ID")))
        if not tmpl_result.scalars().first():
            raise HTTPException(400, "模板不存在")

    for k in ("name", "style", "datasource", "template", "powerontime", "powerofftime", "rotation", "horselamp", "title", "winname"):
        if k in body:
            setattr(box, k, body[k] if body[k] else None)
    if "volume" in body:
        box.volume = body["volume"]  # volume can be 0, don't use `or None`
    box.ut = datetime.now(timezone.utc)
    await db.commit()
    if box.status == "正常":
        await _send_config(None, box.no)
    return {"errcode": 0}


@router.post("/move")
async def move_box(id: str, orgId: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if box:
        box.org = orgId
        box.ut = datetime.now(timezone.utc)
        await db.commit()
    return {"errcode": 0}


@router.post("/remove")
async def remove_box(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if box:
        await db.delete(box)
        await db.commit()
    return {"errcode": 0}


@router.post("/power")
async def power_box(id: str, cmd: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if not box:
        return {"errcode": 0}
    now = datetime.now(timezone.utc)
    if cmd == "on" and box.status == "关机":
        box.status = "正常"; box.ut = now; await db.commit()
        await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "on"}})
    elif cmd == "restart" and box.status == "正常":
        box.status = "断开"; box.ut = now; await db.commit()
        await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "restart"}})
    elif cmd == "off" and box.status == "正常":
        box.status = "关机"; box.ut = now; await db.commit()
        await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "off"}})
    return {"errcode": 0}


@router.post("/group/power")
async def group_power(id: int, cmd: str, db: AsyncSession = Depends(get_db)):
    org_ids = await _resolve_org_children(db, id)
    result = await db.execute(select(Box).where(Box.org.in_(org_ids)))
    boxes = result.scalars().all()
    now = datetime.now(timezone.utc)
    for box in boxes:
        if cmd == "on" and box.status == "关机":
            box.status = "正常"; box.ut = now; await db.commit()
            await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "on"}})
        elif cmd == "restart" and box.status == "正常":
            box.status = "断开"; box.ut = now; await db.commit()
            await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "restart"}})
        elif cmd == "off" and box.status == "正常":
            box.status = "关机"; box.ut = now; await db.commit()
            await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "off"}})
    return {"errcode": 0}


@router.post("/group/save")
async def group_save(id: int, body: dict, db: AsyncSession = Depends(get_db)):
    org_ids = await _resolve_org_children(db, id)
    result = await db.execute(select(Box).where(Box.org.in_(org_ids)))
    boxes = result.scalars().all()
    now = datetime.now(timezone.utc)
    for box in boxes:
        for k in ("powerontime", "powerofftime", "volume", "horselamp"):
            if k in body:
                setattr(box, k, body[k])
        box.ut = now
        await db.commit()
        if box.status == "正常":
            await _send_config(None, box.no)
    return {"errcode": 0}


@router.post("/dataenabled/toggle")
async def toggle_data_enabled(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if not box:
        raise HTTPException(404, "not found")
    box.dataenabled = 0 if box.dataenabled == 1 else 1
    box.ut = datetime.now(timezone.utc)
    await db.commit()
    return {"errcode": 0, "result": {"id": id, "dataenabled": box.dataenabled}}


@router.post("/dataenabled/enable-all")
async def enable_all_data(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Box))
    boxes = result.scalars().all()
    now = datetime.now(timezone.utc)
    for box in boxes:
        box.dataenabled = 1
        box.ut = now
    await db.commit()
    return {"errcode": 0}


@router.post("/upload-log")
async def upload_log(id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if box:
        await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "uploadlog"}})
    return {"errcode": 0}


@router.post("/check-log")
async def check_log(id: str, db: AsyncSession = Depends(get_db)):
    from app.models import LogRecord
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if not box:
        raise HTTPException(404, "not found")
    r = await db.execute(select(LogRecord).where(LogRecord.no == box.no).limit(1))
    log = r.scalars().first()
    return {"errcode": 0} if log else {"errcode": 10000, "errmsg": "未查询到该设备的日志"}


@router.get("/download-log")
async def download_log(id: str, db: AsyncSession = Depends(get_db)):
    import os
    from fastapi.responses import FileResponse
    from app.models import LogRecord
    result = await db.execute(select(Box).where(Box.id == _to_int(id, "终端ID")))
    box = result.scalars().first()
    if not box:
        raise HTTPException(404, "not found")
    r = await db.execute(select(LogRecord).where(LogRecord.no == box.no).limit(1))
    log = r.scalars().first()
    if not log:
        raise HTTPException(404, "log not found")
    fp = os.path.join("uploads", log.name)
    if not os.path.exists(fp):
        raise HTTPException(404, "file not found")
    return FileResponse(fp, filename=log.originname)


# ── Terminal Preview ──

def _build_preview_html(html: str, css: str | None, js: str | None, data_json: str) -> str:
    """Build the same full HTML page that Android WebView renders, with data pre-injected.

    This is an exact replica of MainActivity.loadTemplate() + pushData().
    """
    css_tag = f"<style>{css}</style>" if css else ""
    data_script = f"""<script>
window.__PREVIEW_DATA__ = {data_json};
function __dispatchData() {{
    if (typeof window.updateData === 'function') {{
        window.updateData(window.__PREVIEW_DATA__);
    }}
}}
if (document.readyState === 'loading') {{
    document.addEventListener('DOMContentLoaded', __dispatchData);
}} else {{
    __dispatchData();
}}
</script>"""
    # JS before data_script — match Android loadTemplate() order:
    #   1. CSS in <head>
    #   2. template HTML in <body>
    #   3. template JS in <body> (defines window.updateData)
    #   4. data injection after JS (mimics Android evaluateJavascript)
    js_tag = f"<script>{js}</script>" if js else ""
    return (
        "<!DOCTYPE html><html><head>"
        "<meta charset=\"utf-8\">"
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">"
        f"{css_tag}"
        "</head><body>"
        f"{html}"
        f"{js_tag}"
        f"{data_script}"
        "</body></html>"
    )


async def _get_preview_data(redis, ds: Datasource) -> dict | None:
    """Fetch and transform the latest cached data for a datasource, same pipeline as Socket.IO."""
    from app.services import data_transform as dt

    ds_type = ds.type
    if not ds_type:
        return None

    try:
        if ds_type in ("primarytriage", "leveldepart"):
            dept_ids = ds.departmentid or []
            if not dept_ids:
                return None
            key = ",".join(str(x) for x in dept_ids)
            cache = await redis.hget(f"primary:{key}", "data")
            return await dt.transform_primary(cache) if cache else None

        elif ds_type == "secondarytriage":
            screen_id = ds.screenid
            if not screen_id:
                return None
            cache = await redis.hget(f"secondary:{screen_id}", "data")
            return await dt.transform_secondary(cache) if cache else None

        elif ds_type == "secondarytriageultrasonic":
            queues = ds.queue or []
            if not queues:
                return None
            contents = [await redis.hget(f"pacssecondary:{q}", "data") for q in queues]
            if not any(contents):
                return None
            consultingroomname = ds.consultingroomname or []
            return await dt.transform_pacs_secondary({"content": contents, "consultingroomname": consultingroomname})

        elif ds_type == "drawbloodtriage":
            win_ids = ds.windowid or []
            if not win_ids:
                return None
            key = ",".join(str(x) for x in win_ids)
            cache = await redis.hget(f"drawblood:{key}", "data")
            return await dt.transform_drawblood(cache) if cache else None

        elif ds_type == "primarypharmacytriage":
            dept_no = ds.pharmacydeptno
            win_ids = ds.pharmacywinno or []
            if not dept_no or not win_ids:
                return None
            key = f"{dept_no}_{','.join(str(x) for x in win_ids)}"
            cache = await redis.hget(f"pharmacyprimary:{key}", "data")
            return await dt.transform_pharmacy(cache) if cache else None

        elif ds_type == "secondarypharmacytriage":
            dept_no = ds.pharmacydeptno
            win_ids = ds.pharmacywinno or []
            if not dept_no or not win_ids:
                return None
            key = f"{dept_no}_{','.join(str(x) for x in win_ids)}"
            cache = await redis.hget(f"pharmacysecondary:{key}", "data")
            return await dt.transform_pharmacy_secondary(cache, len(win_ids) > 1) if cache else None

    except Exception as e:
        import logging
        logging.getLogger("preview").warning(f"preview data error: {e}")
    return None


DEFAULT_PREVIEW_HTML = """<!DOCTYPE html>
<html><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
body{margin:0;padding:0;background:#0a0a1a;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;}
.waiting{text-align:center;font-size:20px;color:#666;}
.waiting .hint{font-size:14px;color:#444;margin-top:8px;}
</style>
</head><body>
<div class="waiting">
  <div>⏳ 暂无模板</div>
  <div class="hint">请先为终端配置显示模板</div>
</div>
</body></html>"""


@router.get("/preview/{box_id}")
async def preview_box(box_id: str, db: AsyncSession = Depends(get_db)):
    """返回终端在当前配置下的完整渲染 HTML，与 Android WebView 显示一致。"""
    from fastapi.responses import HTMLResponse
    import json

    # 1. 查终端
    result = await db.execute(select(Box).where(Box.id == _to_int(box_id, "终端ID")))
    box = result.scalars().first()
    if not box:
        return HTMLResponse(content="<h1>终端不存在</h1>", status_code=404)

    # 2. 查模板
    if not box.template:
        return HTMLResponse(content=DEFAULT_PREVIEW_HTML)

    tmpl_result = await db.execute(select(Template).where(Template.id == int(box.template)))
    tmpl = tmpl_result.scalars().first()
    if not tmpl or tmpl.kind != "web":
        return HTMLResponse(content=DEFAULT_PREVIEW_HTML)

    # 3. 获取实时数据（复用 Socket.IO 推送用的数据管道）
    data_json = "{}"
    if box.datasource and box.dataenabled == 1:
        ds_result = await db.execute(select(Datasource).where(Datasource.id == int(box.datasource)))
        ds = ds_result.scalars().first()
        if ds:
            try:
                red = get_redis()
                data = await _get_preview_data(red, ds)
                if data:
                    data_json = json.dumps(data, ensure_ascii=False, default=str)
            except Exception:
                # Redis down or no cached data — show template without data
                pass

    # 4. 组装为完整的 HTML 页面（与 Android WebView 渲染结果一致）
    html = _build_preview_html(tmpl.html, tmpl.css, tmpl.js, data_json)
    return HTMLResponse(content=html)
