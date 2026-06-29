"""Socket.IO event handler — uses SQLAlchemy async sessions."""
from __future__ import annotations

from datetime import datetime, timezone
from urllib.parse import parse_qs

import socketio
from loguru import logger
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_redis, async_session
from app.models import Box, Datasource, Template
from app.utils import pick_not_none
from app.services import data_transform as dt

sio = socketio.AsyncServer(async_mode="asgi", cors_allowed_origins="*")


def _is_data_enabled(box: Box) -> bool:
    return box.dataenabled == 1


async def _safe(fn, *args, **kwargs):
    """Wrap any awaitable with error handling."""
    try:
        return await fn(*args, **kwargs)
    except Exception as e:
        logger.error(f"Error in {fn.__name__}: {e}")
        return None


# ── DB helpers (each takes a session) ──

async def _find_box(session: AsyncSession, **kwargs) -> Box | None:
    stmt = select(Box)
    for k, v in kwargs.items():
        stmt = stmt.where(getattr(Box, k) == v)
    r = await session.execute(stmt)
    return r.scalars().first()


async def _find_ds(session: AsyncSession, ds_id: int | str) -> Datasource | None:
    r = await session.execute(select(Datasource).where(Datasource.id == int(ds_id)))
    return r.scalars().first()


async def _find_tmpl(session: AsyncSession, tmpl_id: int | str) -> Template | None:
    r = await session.execute(select(Template).where(Template.id == int(tmpl_id)))
    return r.scalars().first()


# ── Socket.IO events ──

@sio.event
async def connect(sid: str, environ: dict, auth: dict | None = None) -> None:
    try:
        query = parse_qs(environ.get("QUERY_STRING", ""))
        params = {k: v[0] for k, v in query.items()}
        logger.info(f"Socket.IO connect: {params}")
        now = datetime.now(timezone.utc)

        if not params.get("ip"):
            await sio.emit("apiv1_error", {"type": "ERROR", "content": {"msg": "connect invalid params"}}, room=sid)
            return

        async with async_session() as session:
            if params.get("no"):
                box = await _find_box(session, no=params["no"])
                if box and box.status != "断开":
                    await sio.emit("apiv1_message", {"type": "ERROR", "content": {"code": "11001", "msg": "boxno is repeated"}}, room=sid)
                    await sio.disconnect(sid)
                    return
                elif box:
                    box.ip = params.get("ip", ""); box.mac = params.get("mac", "")
                    box.model = params.get("model", ""); box.appversion = params.get("appversion", "")
                    box.ut = now; box.ht = now; box.status = "正常"
                    await session.commit()
                    await sio.enter_room(sid, params["no"])
                else:
                    box = Box(no=params["no"], name=params["no"], org=-1,
                              ip=params.get("ip", ""), mac=params.get("mac", ""),
                              model=params.get("model", ""), appversion=params.get("appversion", ""),
                              status="正常", ht=now, ct=now, ut=now)
                    session.add(box); await session.commit(); await session.refresh(box)
                    await sio.enter_room(sid, params["no"])
            else:
                r = await session.execute(select(Box).order_by(Box.no.desc()).limit(1))
                last = r.scalars().first()
                try:
                    box_no = f"BOX{(int(last.no.replace('BOX', '')) + 1):03d}" if last else "BOX001"
                except (ValueError, AttributeError):
                    box_no = "BOX001"
                box = Box(no=box_no, name=box_no, org=-1,
                          ip=params.get("ip", ""), mac=params.get("mac", ""),
                          model=params.get("model", ""), appversion=params.get("appversion", ""),
                          status="正常", ht=now, ct=now, ut=now)
                session.add(box); await session.commit()
                params["no"] = box_no
                await sio.enter_room(sid, box_no)

        await _safe(get_redis().hset, "sid_to_box", sid, params["no"])
        await _send_config(sid)
        await _send_initial_data(sid)
    except Exception as e:
        logger.error(f"Socket.IO connect error: {e}")


@sio.event
async def disconnect(sid: str) -> None:
    try:
        box_no = await _safe(get_redis().hget, "sid_to_box", sid)
        if box_no:
            async with async_session() as session:
                box = await _find_box(session, no=box_no)
                if box:
                    box.status = "断开"; box.ut = datetime.now(timezone.utc)
                    await session.commit()
            await _safe(get_redis().hdel, "sid_to_box", sid)
    except Exception as e:
        logger.error(f"Socket.IO disconnect error: {e}")


@sio.on("apiv1_heartbeat")
async def handle_heartbeat(sid: str, data: dict) -> None:
    try:
        box_no = await _safe(get_redis().hget, "sid_to_box", sid)
        if not box_no:
            return
        now = datetime.now(timezone.utc)
        async with async_session() as session:
            box = await _find_box(session, no=box_no)
            if not box:
                return
            if box.status == "断开":
                await sio.emit("apiv1_message", {"type": "ERROR", "content": {"code": "11002", "msg": "box is disconnected"}}, room=sid)
                await sio.disconnect(sid)
            else:
                ip = data.get("content", {}).get("ip", "") if isinstance(data, dict) else ""
                box.ip = ip; box.ut = now; box.ht = now
                await session.commit()
    except Exception as e:
        logger.error(f"Socket.IO heartbeat error: {e}")


@sio.on("apiv1_message")
async def handle_message(sid: str, data: dict) -> None:
    try:
        box_no = await _safe(get_redis().hget, "sid_to_box", sid)
        if not box_no:
            return
        now = datetime.now(timezone.utc)
        msg_type = data.get("type", "")
        content = data.get("content", {})

        async with async_session() as session:
            if msg_type == "CONFIG":
                update = pick_not_none(content, ["style", "datasource", "volume", "powerontime", "powerofftime", "rotation"])
                if update:
                    box = await _find_box(session, no=box_no)
                    if box:
                        for k, v in update.items():
                            setattr(box, k, v)
                        box.ut = now; await session.commit()
                await _send_config(sid)
            elif msg_type == "COMMAND":
                cmd = content.get("cmd", "")
                box = await _find_box(session, no=box_no)
                if box:
                    if cmd == "on": box.status = "正常"
                    elif cmd == "off": box.status = "关机"
                    box.ut = now; await session.commit()
    except Exception as e:
        logger.error(f"Socket.IO message error: {e}")


# ── Internal helpers ──

async def _send_config(sid: str, box_no: str | None = None) -> None:
    try:
        if not box_no:
            box_no = await _safe(get_redis().hget, "sid_to_box", sid)
        if not box_no:
            return
        async with async_session() as session:
            box = await _find_box(session, no=box_no)
            if not box:
                return
            content = pick_not_none(box.__dict__, [
                "no", "name", "style", "datasource", "volume",
                "powerontime", "powerofftime", "rotation", "horselamp", "title", "winname",
            ])
            content.pop("_sa_instance_state", None)
            content["timestamp"] = int(datetime.now(timezone.utc).timestamp() * 1000)
            if box.datasource:
                ds = await _find_ds(session, box.datasource)
                if ds:
                    parts = [t for t in (ds.morningcleartime, ds.afternooncleartime) if t]
                    if parts:
                        content["offtime"] = ",".join(parts)
            if box.template:
                tmpl = await _find_tmpl(session, box.template)
                if tmpl:
                    content["template"] = {"key": tmpl.key, "kind": tmpl.kind, "version": tmpl.version,
                                           "logo": tmpl.logo, "params": tmpl.params, "manifest": tmpl.manifest,
                                           "package": tmpl.params.get("package", "") if isinstance(tmpl.params, dict) else ""}
                    # 旧安卓 (triage/) 在顶层的 Params 字段读取模板参数（提示文字、电话、二维码等）
                    if tmpl.params and isinstance(tmpl.params, dict):
                        content["Params"] = tmpl.params
                    if tmpl.kind == "web":
                        content["template"]["url"] = f"/api/v1/templates/serve/{tmpl.id}"
                        content["template"]["html"] = tmpl.html
                        content["template"]["css"] = tmpl.css
                        content["template"]["js"] = tmpl.js
        await sio.emit("apiv1_message", {"type": "CONFIG", "content": content}, room=box_no)
    except Exception as e:
        logger.error(f"send_config error: {e}")


async def _send_initial_data(sid: str, box_no: str | None = None) -> None:
    try:
        if not box_no:
            box_no = await _safe(get_redis().hget, "sid_to_box", sid)
        if not box_no:
            return

        # Load box and datasource in one session, extract all needed data before closing
        async with async_session() as session:
            box = await _find_box(session, no=box_no)
            if not box or not box.datasource or not _is_data_enabled(box):
                return
            ds = await _find_ds(session, box.datasource)
            if not ds:
                return
            # Snapshot data before session closes
            ds_type = ds.type
            ds_departmentid = ds.departmentid or []
            ds_screenid = ds.screenid
            ds_queue = ds.queue or []
            ds_consultingroomname = ds.consultingroomname or []
            ds_windowid = ds.windowid or []
            ds_pharmacydeptno = ds.pharmacydeptno
            ds_pharmacywinno = ds.pharmacywinno or []

        redis = get_redis()

        if ds_type in ("primarytriage", "leveldepart"):
            key = ",".join(str(x) for x in ds_departmentid)
            cache = await _safe(redis.hget, f"primary:{key}", "data")
            if cache:
                content = await dt.transform_primary(cache)
                await sio.emit("apiv1_message", {"type": "DATA", "content": content}, room=box_no)
        elif ds_type == "secondarytriage":
            cache = await _safe(redis.hget, f"secondary:{ds_screenid}", "data")
            if cache:
                content = await dt.transform_secondary(cache)
                await sio.emit("apiv1_message", {"type": "DATA", "content": content}, room=box_no)
        elif ds_type == "secondarytriageultrasonic":
            contents = [await _safe(redis.hget, f"pacssecondary:{q}", "data") for q in ds_queue]
            if any(contents):
                content = await dt.transform_pacs_secondary({"content": contents, "consultingroomname": ds_consultingroomname})
                await sio.emit("apiv1_message", {"type": "DATA", "content": content}, room=box_no)
        elif ds_type == "drawbloodtriage":
            key = ",".join(str(x) for x in ds_windowid)
            cache = await _safe(redis.hget, f"drawblood:{key}", "data")
            if cache:
                content = await dt.transform_drawblood(cache)
                await sio.emit("apiv1_message", {"type": "DATA", "content": content}, room=box_no)
        elif ds_type == "primarypharmacytriage":
            key = f"{ds_pharmacydeptno}_{','.join(str(x) for x in ds_pharmacywinno)}"
            cache = await _safe(redis.hget, f"pharmacyprimary:{key}", "data")
            if cache:
                content = await dt.transform_pharmacy(cache)
                await sio.emit("apiv1_message", {"type": "DATA", "content": content}, room=box_no)
        elif ds_type == "secondarypharmacytriage":
            key = f"{ds_pharmacydeptno}_{','.join(str(x) for x in ds_pharmacywinno)}"
            cache = await _safe(redis.hget, f"pharmacysecondary:{key}", "data")
            if cache:
                content = await dt.transform_pharmacy_secondary(cache, len(ds_pharmacywinno) > 1)
                await sio.emit("apiv1_message", {"type": "DATA", "content": content}, room=box_no)
    except Exception as e:
        logger.error(f"send_initial_data error: {e}")


async def emit_to_box(box_no: str, data: dict) -> None:
    try:
        await sio.emit("apiv1_message", data, room=box_no)
    except Exception as e:
        logger.error(f"emit_to_box({box_no}) error: {e}")
