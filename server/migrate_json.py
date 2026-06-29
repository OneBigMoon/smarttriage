"""Import MongoDB JSON dump into PostgreSQL.

Usage:
  1. Place JSON files in the same directory as this script (or set DATA_DIR)
  2. Run: python migrate_json.py

Reads:
  - smarttriage.users.json
  - smarttriage.orgnizations.json
  - smarttriage.datasources.json
  - smarttriage.systems.json
  - smarttriage.styles.json
  - smarttriage.datasourcetypes.json
  - smarttriage.logs.json
  - smarttriage.upgrades.json
  - smarttriage.boxes.json (extracted from log)
"""
from __future__ import annotations

import json
import os
import re
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import select
from passlib.context import CryptContext

from app.database import engine, async_session
from app.models.base import Base
from app.models import ALL_MODELS
from app.models import (
    Box, Datasource, User, Organization, SystemConfig,
    Upgrade, LogRecord, Template, Style, DatasourceType,
)

pwd_ctx = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")

DATA_DIR = os.getenv("DATA_DIR", "/Users/x/Documents/排队叫号/新建文件夹")


def parse_mongo_date(val: Any) -> datetime | None:
    """Parse MongoDB date format: {'$date': '2024-01-01T00:00:00.000Z'}"""
    if isinstance(val, dict) and "$date" in val:
        try:
            return datetime.fromisoformat(val["$date"].replace("Z", "+00:00"))
        except Exception:
            return None
    if isinstance(val, str):
        try:
            return datetime.fromisoformat(val.replace("Z", "+00:00"))
        except Exception:
            return None
    if isinstance(val, datetime):
        return val
    return None


def load_json(filename: str) -> list[dict]:
    path = os.path.join(DATA_DIR, filename)
    if not os.path.exists(path):
        print(f"  ⚠ {filename} not found, skipping")
        return []
    with open(path, "r", encoding="utf-8") as f:
        content = f.read().strip()
        # Handle JSON Lines format (one JSON per line)
        if content.startswith("["):
            return json.loads(content)
        else:
            return [json.loads(line) for line in content.split("\n") if line.strip()]


def get_nested(obj: dict, key: str, default=None):
    """Get value handling MongoDB $oid format."""
    val = obj.get(key, default)
    if isinstance(val, dict) and "$oid" in val:
        return val["$oid"]
    return val


async def migrate():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    # Check if already migrated
    async with async_session() as db:
        result = await db.execute(select(Box).limit(1))
        if result.scalars().first():
            print("⚠ Database already has data. Delete the DB first to re-migrate.")
            return

    now = datetime.now(timezone.utc)

    # ── Load all JSON files ──
    print("Loading JSON files...")
    users_data = load_json("smarttriage.users.json")
    orgs_data = load_json("smarttriage.orgnizations.json")
    ds_data = load_json("smarttriage.datasources.json")
    sys_data = load_json("smarttriage.systems.json")
    styles_data = load_json("smarttriage.styles.json")
    dtypes_data = load_json("smarttriage.datasourcetypes.json")
    logs_data = load_json("smarttriage.logs.json")
    ups_data = load_json("smarttriage.upgrades.json")
    boxes_data = load_json("smarttriage.boxes.json")

    print(f"  Users: {len(users_data)}, Orgs: {len(orgs_data)}, Datasources: {len(ds_data)}")
    print(f"  Boxes: {len(boxes_data)}, Systems: {len(sys_data)}, Upgrades: {len(ups_data)}")
    print(f"  Logs: {len(logs_data)}, Styles: {len(styles_data)}, DTypes: {len(dtypes_data)}")

    async with async_session() as db:
        # ── Users ──
        for u in users_data:
            db.add(User(
                username=u.get("username", ""),
                salt=u.get("salt", ""),
                hashedPassword=u.get("hashedPassword", ""),
                auths=u.get("auths", []),
                fullname=u.get("fullname", ""),
                profession=u.get("profession"),
                praises=u.get("praises"),
                lastlogintime=parse_mongo_date(u.get("lastlogintime")),
                photoname=u.get("photoname"),
                photouri=u.get("photouri"),
                ct=parse_mongo_date(u.get("ct")) or now,
                ut=parse_mongo_date(u.get("ut")) or now,
            ))
        print(f"✓ Users: {len(users_data)}")

        # ── Organizations ──
        for o in orgs_data:
            oid = o.get("_id")
            if isinstance(oid, dict):
                oid = oid.get("$oid", 0)
            db.add(Organization(
                id=int(oid),
                name=o.get("name", ""),
                parentid=o.get("parentid"),
                idpath=o.get("idpath", ""),
                user=o.get("user"),
                ct=parse_mongo_date(o.get("ct")) or now,
                ut=parse_mongo_date(o.get("ut")) or now,
            ))
        print(f"✓ Organizations: {len(orgs_data)}")

        # ── Datasources ──
        ds_id_map = {}  # old_mongo_id -> new_sql_id
        for d in ds_data:
            old_id = get_nested(d, "_id")
            new_ds = Datasource(
                name=d.get("name", ""),
                type=d.get("type", ""),
                departmentid=d.get("departmentid", []),
                screenid=d.get("screenid"),
                screensplitid=d.get("screensplitid", []),
                queue=d.get("queue", []),
                consultingroomname=d.get("consultingroomname"),
                windowid=d.get("windowid", []),
                pharmacydeptno=d.get("pharmacydeptno"),
                pharmacywinno=d.get("pharmacywinno", []),
                morningcleartime=d.get("morningcleartime"),
                afternooncleartime=d.get("afternooncleartime"),
                ct=parse_mongo_date(d.get("ct")) or now,
                ut=parse_mongo_date(d.get("ut")) or now,
            )
            db.add(new_ds)
            await db.flush()
            ds_id_map[old_id] = str(new_ds.id)
        print(f"✓ Datasources: {len(ds_data)} (mapped {len(ds_id_map)} IDs)")

        # ── Boxes ──
        for b in boxes_data:
            old_ds_id = b.get("datasource", "")
            new_ds_id = ds_id_map.get(old_ds_id, old_ds_id if old_ds_id else None)

            db.add(Box(
                no=b.get("no", ""),
                name=b.get("name", ""),
                org=b.get("org"),
                ip=b.get("ip", ""),
                mac=b.get("mac", ""),
                model=b.get("model", ""),
                appversion=b.get("appversion", ""),
                style=b.get("style"),
                datasource=new_ds_id,
                status=b.get("status", "正常"),
                powerontime=b.get("powerontime"),
                powerofftime=b.get("powerofftime"),
                volume=b.get("volume"),
                horselamp=b.get("horselamp"),
                title=b.get("title"),
                winname=b.get("winname"),
                rotation=b.get("rotation"),
                dataenabled=b.get("dataenabled", 0),
                ht=parse_mongo_date(b.get("ht")),
                ct=parse_mongo_date(b.get("ct")) or now,
                ut=parse_mongo_date(b.get("ut")) or now,
            ))
        print(f"✓ Boxes: {len(boxes_data)}")

        # ── SystemConfig ──
        for s in sys_data:
            db.add(SystemConfig(
                url=s.get("url"),
                primarytablename=s.get("primarytablename"),
                secondarytablename=s.get("secondarytablename"),
                secondarycounttablename=s.get("secondarycounttablename"),
                drawbloodtablename=s.get("drawbloodtablename"),
                pharmacytablename=s.get("pharmacytablename"),
                username=s.get("username"),
                password=s.get("password"),
                pacssecondarytablename=s.get("pacssecondarytablename"),
                pacsusername=s.get("pacsusername"),
                pacspassword=s.get("pacspassword"),
            ))
        print(f"✓ SystemConfig: {len(sys_data)}")

        # ── Upgrades ──
        for u in ups_data:
            db.add(Upgrade(
                model=u.get("model", ""),
                appVersion=u.get("appVersion", ""),
                sortAppVersion=u.get("sortAppVersion", 0),
                originname=u.get("originname", ""),
                name=u.get("name", ""),
                path=u.get("path", ""),
                md5=u.get("md5", ""),
                ct=parse_mongo_date(u.get("ct")) or now,
                ut=parse_mongo_date(u.get("ut")) or now,
            ))
        print(f"✓ Upgrades: {len(ups_data)}")

        # ── LogRecords ──
        for l in logs_data:
            db.add(LogRecord(
                no=l.get("no", ""),
                originname=l.get("originname", ""),
                name=l.get("name", ""),
                path=l.get("path", ""),
                ct=parse_mongo_date(l.get("ct")) or now,
                ut=parse_mongo_date(l.get("ut")) or now,
            ))
        print(f"✓ LogRecords: {len(logs_data)}")

        # ── Styles ──
        for s in styles_data:
            db.add(Style(name=s.get("name", ""), key=s.get("key", "")))
        if not styles_data:
            for name, key in [("一级分诊", "primarytriage"), ("二级分诊", "secondarytriage"),
                               ("二级分诊(分屏)", "secondarytriagesplit"), ("二级超声分诊", "secondarytriageultrasonic"),
                               ("检验分诊", "drawbloodtriage"), ("药房一级分诊", "primarypharmacytriage"),
                               ("药房二级分诊", "secondarypharmacytriage"), ("层级科室", "leveldepart")]:
                db.add(Style(name=name, key=key))
        print(f"✓ Styles: {len(styles_data)}")

        # ── DatasourceTypes ──
        for t in dtypes_data:
            db.add(DatasourceType(name=t.get("name", ""), key=t.get("key", "")))
        if not dtypes_data:
            for name, key in [("一级分诊", "primarytriage"), ("二级分诊", "secondarytriage"),
                               ("二级分诊(分屏)", "secondarytriagesplit"), ("二级超声分诊", "secondarytriageultrasonic"),
                               ("检验分诊", "drawbloodtriage"), ("药房一级分诊", "primarypharmacytriage"),
                               ("药房二级分诊", "secondarypharmacytriage"), ("层级科室", "leveldepart")]:
                db.add(DatasourceType(name=name, key=key))
        print(f"✓ DatasourceTypes: {len(dtypes_data)}")

        # ── Templates (defaults) ──
        db.add(Template(name="一级分诊-默认", key="primarytriage_default", kind="web", version="1.0.0",
                         html='<div id="app"><div class="queue"><div class="header">排队信息</div>{{#each queues}}<div class="item"><span class="callno">{{callno}}</span><span class="name">{{patientname}}</span><span class="dept">{{officename}}</span></div>{{/each}}</div></div>',
                         css='*{margin:0;padding:0;box-sizing:border-box}body{background:#0a0a1a;color:#fff;font-family:sans-serif;min-height:100vh}.queue .header{background:#1a1a2e;padding:16px 24px;font-size:24px;color:#ffd700}.queue .item{display:flex;justify-content:space-between;padding:14px 24px;border-bottom:1px solid #222}.callno{color:#ffd700;font-size:28px;font-weight:bold;min-width:80px}.name{font-size:20px}.dept{color:#aaa;font-size:16px}',
                         ct=now, ut=now))
        db.add(Template(name="二级分诊-默认", key="secondarytriage_default", kind="web", version="1.0.0",
                         html='<div id="app"><div class="header"><div class="clinic">{{clinicname}}</div><div class="doctor">{{doctorname}} {{doctortitle}}</div></div><div class="patients">{{#each patients}}<div class="item"><span class="ticket">{{ticket}}</span><span class="name">{{brxmfull}}</span></div>{{/each}}</div><div class="count">等候: {{patientcount}}人</div></div>',
                         css='*{margin:0;padding:0;box-sizing:border-box}body{background:#0a0a1a;color:#fff;font-family:sans-serif}.header{background:#1a1a2e;padding:30px;text-align:center}.clinic{font-size:36px;color:#ffd700;margin-bottom:8px}.doctor{font-size:20px;color:#aaa}.patients{padding:20px}.item{display:flex;justify-content:space-between;padding:14px 20px;border-bottom:1px solid #222;font-size:20px}.ticket{color:#ffd700;font-weight:bold;width:80px}.count{padding:16px;text-align:center;color:#aaa}',
                         ct=now, ut=now))
        db.add(Template(name="药房叫号-默认", key="pharmacy_default", kind="web", version="1.0.0",
                         html='<div id="app"><div class="header"><span class="window">窗口 {{winno}}</span><span class="dept">{{deptname}}</span></div><div class="called"><div class="ticket">{{calledTicket}}</div><div class="name">{{calledName}}</div></div></div>',
                         css='*{margin:0;padding:0;box-sizing:border-box}body{background:#0a0a1a;color:#fff;font-family:sans-serif}.header{background:#1a1a2e;padding:20px;text-align:center}.window{font-size:36px;color:#ffd700;margin-right:20px}.called{background:#1a3a1a;padding:40px;text-align:center;margin:20px;border-radius:12px}.ticket{font-size:72px;color:#ffd700;font-weight:bold}.name{font-size:32px;margin-top:10px}',
                         ct=now, ut=now))
        print("✓ Templates: 3 defaults")

        await db.commit()

    print(f"\n✅ Migration complete! {len(boxes_data)} boxes, {len(orgs_data)} orgs, {len(ds_data)} datasources imported.")


if __name__ == "__main__":
    import asyncio
    asyncio.run(migrate())
