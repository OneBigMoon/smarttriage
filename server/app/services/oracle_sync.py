"""Oracle data sync scheduler — uses SQLAlchemy async sessions."""
from __future__ import annotations

import json
from datetime import datetime, timezone

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from loguru import logger
from sqlalchemy import select

from app.database import get_redis, async_session
from app.models import Box, Datasource, SystemConfig
from app.services import oracle_pool, data_transform as dt
from app.services.socketio_handler import emit_to_box
from app.utils import md5

scheduler = AsyncIOScheduler()


async def _emit_to_datasource_boxes(ds_id: int, ds_type: str, data: dict) -> None:
    async with async_session() as session:
        r = await session.execute(select(Box).where(Box.datasource == str(ds_id)))
        for box in r.scalars().all():
            if box.status == "正常":
                await emit_to_box(box.no, {"type": "DATA", "content": data})


async def _get_system() -> SystemConfig | None:
    async with async_session() as session:
        r = await session.execute(select(SystemConfig).limit(1))
        sys = r.scalars().first()
        # Also sync to Redis for other consumers
        if sys:
            from app.database import get_redis
            data = {k: str(v) for k, v in sys.__dict__.items()
                    if not k.startswith("_") and v is not None}
            await get_redis().hset("system", mapping=data)
        return sys


# ── 一级分诊 ──
async def sync_primary() -> None:
    try:
        system = await _get_system()
        if not system or not system.primarytablename:
            return
        sql = f"SELECT XSPID0, ZSMC00, YSXM00, HZXHMC, BRXM00, GZBRXH, JHSJ00, DSXSXX, JHYYXX, HZBRXH FROM {system.primarytablename} ORDER BY JHSJ00 DESC"
        rows = await oracle_pool.query(sql)
        dept_map: dict = {}
        for row in rows:
            dept_map.setdefault(row[0], []).append(row)

        async with async_session() as session:
            r = await session.execute(select(Datasource).where(Datasource.type.in_(["primarytriage", "leveldepart"])))
            datasources = r.scalars().all()

        r = get_redis()
        for ds in datasources:
            key = ",".join(str(x) for x in (ds.departmentid or []))
            cache = []
            for dept_id in (ds.departmentid or []):
                if dept_id in dept_map:
                    if ds.type != "leveldepart":
                        cache.extend(dept_map[dept_id])
                    else:
                        cache.append(dept_map[dept_id][0])
            cache.sort(key=lambda row: row[6] if len(row) > 6 else "", reverse=True)
            cache_json = json.dumps(cache, ensure_ascii=False, default=str)
            cache_md5 = md5(cache_json)
            last_md5 = await r.hget(f"primary:{key}", "md5")
            if cache_md5 != last_md5:
                await r.hset(f"primary:{key}", mapping={"md5": cache_md5, "data": cache_json})
                content = await dt.transform_primary(cache_json)
                await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
    except Exception as e:
        logger.error(f"sync_primary error: {e}")


# ── 二级分诊 ──
async def sync_secondary() -> None:
    try:
        system = await _get_system()
        if not system or not system.secondarytablename or not system.secondarycounttablename:
            return
        count_rows = await oracle_pool.query(f"SELECT JZYSBH, JZRS00 FROM {system.secondarycounttablename}")
        r = get_redis()
        for row in count_rows:
            await r.set(f"secondary_count:{row[0]}", row[1])

        sql = f"SELECT ZSMC00, XSPID0, JZYSXM, YSZYZC, ZYTC00, PBSJ00, PHOTO0, HZXHMC, BRXM00, BRXM01, HZZT00, JZYSBH, YSJJ00, JZSJ00 FROM {system.secondarytablename}"
        rows = await oracle_pool.query(sql)
        screen_map: dict = {}
        for row in rows:
            screen_map.setdefault(row[1], []).append(row)

        async with async_session() as session:
            for screen_id, screen_rows in screen_map.items():
                cache_json = json.dumps(screen_rows, ensure_ascii=False, default=str)
                cache_md5 = md5(cache_json)
                last_md5 = await r.hget(f"secondary:{screen_id}", "md5")
                if cache_md5 != last_md5:
                    await r.hset(f"secondary:{screen_id}", mapping={"md5": cache_md5, "data": cache_json})
                    ds_r = await session.execute(select(Datasource).where(Datasource.type == "secondarytriage", Datasource.screenid == screen_id))
                    for ds in ds_r.scalars().all():
                        content = await dt.transform_secondary(cache_json)
                        await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
                    ds_r2 = await session.execute(select(Datasource).where(Datasource.type == "secondarytriagesplit", Datasource.screensplitid.contains(screen_id)))
                    for ds in ds_r2.scalars().all():
                        split_caches = []
                        for sid in (ds.screensplitid or []):
                            c = await r.hget(f"secondary:{sid}", "data")
                            split_caches.append(c)
                        content = await dt.transform_secondary_split(split_caches)
                        await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
    except Exception as e:
        logger.error(f"sync_secondary error: {e}")


# ── 超声二级 ──
async def sync_ultrasonic() -> None:
    try:
        system = await _get_system()
        if not system or not system.pacssecondarytablename:
            return
        time_period = "上午" if datetime.now().hour < 13 else "下午"
        sql = (f"SELECT 队列名称, 排队号, NAME, 状态, 叫号时间, 呼叫诊室 FROM "
               f"(SELECT 队列名称, 排队号, NAME, 状态, 叫号时间, 呼叫诊室, "
               f"ROW_NUMBER() OVER(PARTITION BY 队列名称 ORDER BY 叫号时间 DESC) NO "
               f"FROM {system.pacssecondarytablename} WHERE 状态!='0' AND 叫号时间 IS NOT NULL) WHERE NO=1")
        recent = await oracle_pool.query_pacs(sql)
        pacs_map: dict = {}
        for row in recent:
            pacs_map[row[0]] = [row]
        sql2 = (f"SELECT 队列名称, 排队号, NAME, 状态, 呼叫诊室 FROM {system.pacssecondarytablename} "
                f"WHERE 状态='0' AND 队列时段='{time_period}' ORDER BY 队列名称, 排队号")
        waiting = await oracle_pool.query_pacs(sql2)
        for row in waiting:
            pacs_map.setdefault(row[0], []).append(row)

        r = get_redis()
        for queue_name, queue_rows in pacs_map.items():
            cache_json = json.dumps(queue_rows, ensure_ascii=False, default=str)
            cache_md5 = md5(cache_json)
            await r.hset(f"pacssecondary:{queue_name}", mapping={"md5": cache_md5, "data": cache_json})

        async with async_session() as session:
            ds_r = await session.execute(select(Datasource).where(Datasource.type == "secondarytriageultrasonic"))
            for ds in ds_r.scalars().all():
                contents = {"content": [], "consultingroomname": ds.consultingroomname or []}
                for q in (ds.queue or []):
                    c = await r.hget(f"pacssecondary:{q}", "data")
                    contents["content"].append(c)
                content = await dt.transform_pacs_secondary(contents)
                await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
    except Exception as e:
        logger.error(f"sync_ultrasonic error: {e}")


# ── 检验 ──
async def sync_drawblood() -> None:
    try:
        system = await _get_system()
        if not system or not system.drawbloodtablename:
            return
        sql = f"SELECT CYCKBH, CKMC00, JHBZ00, DLH000, DJSJ00, BRXM00, DSXSXX, JHYYXX FROM {system.drawbloodtablename} WHERE JHBZ00='1' OR JHBZ00='7' ORDER BY CYCKBH, JHBZ00 DESC"
        rows = await oracle_pool.query(sql)
        win_map = {row[0]: row for row in rows}

        async with async_session() as session:
            ds_r = await session.execute(select(Datasource).where(Datasource.type == "drawbloodtriage"))
            datasources = ds_r.scalars().all()

        r = get_redis()
        for ds in datasources:
            key = ",".join(str(x) for x in (ds.windowid or []))
            cache = sorted([win_map[w] for w in (ds.windowid or []) if w in win_map], key=lambda x: x[0])
            cache_json = json.dumps(cache, ensure_ascii=False, default=str)
            cache_md5 = md5(cache_json)
            last_md5 = await r.hget(f"drawblood:{key}", "md5")
            if cache_md5 != last_md5:
                await r.hset(f"drawblood:{key}", mapping={"md5": cache_md5, "data": cache_json})
                content = await dt.transform_drawblood(cache_json)
                await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
    except Exception as e:
        logger.error(f"sync_drawblood error: {e}")


# ── 药房一级 ──
async def sync_pharmacy_primary() -> None:
    try:
        system = await _get_system()
        if not system or not system.pharmacytablename:
            return
        sql = f"SELECT yfbmbh, fyckbh, fydlxh, brxm00, jhcs00, jhsj00, brxm01 FROM {system.pharmacytablename} ORDER BY yfbmbh, fyckbh, jhsj00 DESC"
        rows = await oracle_pool.query(sql)
        win_map: dict = {}
        for row in rows:
            win_map.setdefault(f"{row[0]}_{row[1]}", []).append(row)

        async with async_session() as session:
            ds_r = await session.execute(select(Datasource).where(Datasource.type == "primarypharmacytriage"))
            datasources = ds_r.scalars().all()

        r = get_redis()
        for ds in datasources:
            key = f"{ds.pharmacydeptno}_{','.join(str(x) for x in (ds.pharmacywinno or []))}"
            cache = []
            for w in (ds.pharmacywinno or []):
                cache.extend(win_map.get(f"{ds.pharmacydeptno}_{w}", []))
            cache.sort(key=lambda x: x[5] if len(x) > 5 else "", reverse=True)
            cache_json = json.dumps(cache, ensure_ascii=False, default=str)
            cache_md5 = md5(cache_json)
            last_md5 = await r.hget(f"pharmacyprimary:{key}", "md5")
            if cache_md5 != last_md5:
                await r.hset(f"pharmacyprimary:{key}", mapping={"md5": cache_md5, "data": cache_json})
                content = await dt.transform_pharmacy(cache_json)
                await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
    except Exception as e:
        logger.error(f"sync_pharmacy_primary error: {e}")


# ── 药房二级 ──
async def sync_pharmacy_secondary() -> None:
    try:
        system = await _get_system()
        if not system or not system.pharmacytablename:
            return
        sql = (f"SELECT yfbmbh, fyckbh, fydlxh, brxm00, brxm01, jhcs00, jhsj00, zt0000, CKMC00 "
               f"FROM {system.pharmacytablename} WHERE zt0000 IN ('0','1','2') ORDER BY yfbmbh, fyckbh, zt0000, jhsj00 DESC")
        rows = await oracle_pool.query(sql)
        win_map: dict = {}
        for row in rows:
            win_map.setdefault(f"{row[0]}_{row[1]}", []).append(row)

        async with async_session() as session:
            ds_r = await session.execute(select(Datasource).where(Datasource.type == "secondarypharmacytriage"))
            datasources = ds_r.scalars().all()

        r = get_redis()
        for ds in datasources:
            key = f"{ds.pharmacydeptno}_{','.join(str(x) for x in (ds.pharmacywinno or []))}"
            cache = []
            for w in (ds.pharmacywinno or []):
                cache.extend(win_map.get(f"{ds.pharmacydeptno}_{w}", []))
            cache_json = json.dumps(cache, ensure_ascii=False, default=str)
            cache_md5 = md5(cache_json)
            last_md5 = await r.hget(f"pharmacysecondary:{key}", "md5")
            if cache_md5 != last_md5:
                await r.hset(f"pharmacysecondary:{key}", mapping={"md5": cache_md5, "data": cache_json})
                content = await dt.transform_pharmacy_secondary(cache_json, len(ds.pharmacywinno or []) > 1)
                await _emit_to_datasource_boxes(ds.id, ds.type, {"type": "DATA", "content": content})
    except Exception as e:
        logger.error(f"sync_pharmacy_secondary error: {e}")


# ── 清缓存 ──
async def clear_data_cache() -> None:
    try:
        now = datetime.now(timezone.utc)
        r = get_redis()
        async with async_session() as session:
            ds_r = await session.execute(select(Datasource))
            datasources = ds_r.scalars().all()
            for ds in datasources:
                flag = False
                for time_field in ("morningcleartime", "afternooncleartime"):
                    t = getattr(ds, time_field, None)
                    if t:
                        parts = t.split(":")
                        h, m, s = int(parts[0]), int(parts[1]), int(parts[2])
                        if now.hour == h and now.minute == m and 0 <= now.second - s < 300:
                            flag = True
                if not flag:
                    continue
                if ds.type in ("primarytriage", "leveldepart"):
                    await r.delete(f"primary:{','.join(str(x) for x in (ds.departmentid or []))}")
                elif ds.type == "secondarytriage":
                    await r.delete(f"secondary:{ds.screenid}")
                elif ds.type == "secondarytriagesplit":
                    for sid in (ds.screensplitid or []):
                        await r.delete(f"secondary:{sid}")
                elif ds.type == "secondarytriageultrasonic":
                    for q in (ds.queue or []):
                        await r.delete(f"pacssecondary:{q}")
                elif ds.type == "drawbloodtriage":
                    await r.delete(f"drawblood:{','.join(str(x) for x in (ds.windowid or []))}")
                elif ds.type == "primarypharmacytriage":
                    await r.delete(f"pharmacyprimary:{ds.pharmacydeptno}_{','.join(str(x) for x in (ds.pharmacywinno or []))}")
                elif ds.type == "secondarypharmacytriage":
                    await r.delete(f"pharmacysecondary:{ds.pharmacydeptno}_{(ds.pharmacywinno or [0])[0]}")

                box_r = await session.execute(select(Box).where(Box.datasource == str(ds.id)))
                for box in box_r.scalars().all():
                    await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "cleardata"}})
    except Exception as e:
        logger.error(f"clear_data_cache error: {e}")


# ── 断线检查 ──
async def check_disconnect() -> None:
    try:
        threshold = datetime.now(timezone.utc).timestamp() - 30
        async with async_session() as session:
            r = await session.execute(select(Box).where(Box.status != "断开"))
            for box in r.scalars().all():
                if box.ht and box.ht.timestamp() < threshold:
                    box.status = "断开"
                    box.ut = datetime.now(timezone.utc)
            await session.commit()
    except Exception as e:
        logger.error(f"check_disconnect error: {e}")


def start_scheduler() -> None:
    scheduler.add_job(sync_primary, "interval", seconds=3, id="sync_primary", max_instances=1)
    scheduler.add_job(sync_secondary, "interval", seconds=3, id="sync_secondary", max_instances=1)
    scheduler.add_job(sync_ultrasonic, "interval", seconds=3, id="sync_ultrasonic", max_instances=1)
    scheduler.add_job(sync_drawblood, "interval", seconds=3, id="sync_drawblood", max_instances=1)
    scheduler.add_job(sync_pharmacy_primary, "interval", seconds=3, id="sync_pharmacy_primary", max_instances=1)
    scheduler.add_job(sync_pharmacy_secondary, "interval", seconds=3, id="sync_pharmacy_secondary", max_instances=1)
    scheduler.add_job(clear_data_cache, "interval", seconds=120, id="clear_cache", max_instances=1)
    scheduler.add_job(check_disconnect, "interval", seconds=15, id="check_disconnect", max_instances=1)
    scheduler.start()
    logger.info("Scheduler started")


def stop_scheduler() -> None:
    scheduler.shutdown(wait=False)
