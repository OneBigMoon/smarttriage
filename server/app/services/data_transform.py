"""Data transform functions — exact port of old lib/utils/index.js transform*."""
from __future__ import annotations

import json
from typing import Any

from app.database import get_redis


def _parse(cache: Any) -> list:
    if isinstance(cache, str):
        return json.loads(cache)
    return cache or []


# ── 一级分诊 ──
async def transform_primary(cache: Any) -> dict:
    rows = _parse(cache)
    queues = []
    for row in rows:
        queues.append({
            "departmentid": row[0] if len(row) > 0 else None,
            "officename": row[1] if len(row) > 1 else "",
            "doctorname": row[2] if len(row) > 2 else "",
            "callno": row[3] if len(row) > 3 else "",
            "patientname": row[4] if len(row) > 4 else "",
            "passno": row[5] if len(row) > 5 else "",
            "calltime": row[6] if len(row) > 6 else "",
            "callmessage": row[7] if len(row) > 7 else "",
            "callvoice": row[8] if len(row) > 8 else "",
            "waitno": row[9] if len(row) > 9 else "",
        })
    return {"queues": queues}


# ── 二级分诊 ──
async def transform_secondary(cache: Any) -> dict:
    rows = _parse(cache)
    result: dict[str, Any] = {"patients": []}
    for row in rows:
        if not result.get("clinicname") and row[0]:
            result["clinicname"] = row[0]
        if not result.get("screenid") and row[1]:
            result["screenid"] = row[1]
        if not result.get("doctorname") and row[2]:
            result["doctorname"] = row[2]
        if not result.get("doctortitle") and row[3]:
            result["doctortitle"] = row[3]
        if not result.get("doctorintro") and row[4]:
            result["doctorintro"] = row[4]
        if not result.get("doctorschedule") and row[5]:
            result["doctorschedule"] = row[5]
        if not result.get("doctorphoto"):
            result["doctorphoto"] = row[6] if len(row) > 6 and row[6] else None
        if not result.get("doctorno") and row[11] if len(row) > 11 else None:
            result["doctorno"] = row[11]
        if not result.get("doctorfeature") and row[12] if len(row) > 12 else None:
            result["doctorfeature"] = row[12]
        result["patients"].append({
            "ticket": row[7] if len(row) > 7 else "",
            "brxm": row[8] if len(row) > 8 else "",
            "brxmfull": row[9] if len(row) > 9 else "",
            "status": row[10] if len(row) > 10 else "",
            "calltime": row[13] if len(row) > 13 else "",
            "callmessage": f"请 {row[7] if len(row) > 7 else ''}号 {row[8] if len(row) > 8 else ''} 到 {row[0]} 就诊",
            "callvoice": f"请{row[7] if len(row) > 7 else ''}号{row[9] if len(row) > 9 else ''}到{row[0]}就诊",
        })
    if result.get("doctorno"):
        count = await get_redis().get(f"secondary_count:{result['doctorno']}")
        result["patientcount"] = count
    return result


# ── 二级分诊(分屏) ──
async def transform_secondary_split(caches: list) -> dict:
    result: dict[str, Any] = {"queues": []}
    for cache in caches:
        if not cache:
            continue
        rows = _parse(cache)
        queue: dict[str, Any] = {"patients": []}
        for row in rows:
            if not queue.get("clinicname") and row[0]:
                queue["clinicname"] = row[0]
            if not queue.get("screenid") and row[1]:
                queue["screenid"] = row[1]
            if not queue.get("doctorname") and row[2]:
                queue["doctorname"] = row[2]
            if not queue.get("doctortitle") and row[3]:
                queue["doctortitle"] = row[3]
            if not queue.get("doctorintro") and row[4]:
                queue["doctorintro"] = row[4]
            if not queue.get("doctorschedule") and row[5]:
                queue["doctorschedule"] = row[5]
            if not queue.get("doctorphoto"):
                queue["doctorphoto"] = row[6] if len(row) > 6 and row[6] else None
            if not queue.get("doctorno") and len(row) > 11 and row[11]:
                queue["doctorno"] = row[11]
            if not queue.get("doctorfeature") and len(row) > 12 and row[12]:
                queue["doctorfeature"] = row[12]
            if not queue.get("offtime") and len(row) > 13 and row[13]:
                queue["offtime"] = row[13]
            queue["patients"].append({
                "ticket": row[7] if len(row) > 7 else "",
                "brxm": row[8] if len(row) > 8 else "",
                "brxmfull": row[9] if len(row) > 9 else "",
                "status": row[10] if len(row) > 10 else "",
            })
        if queue.get("doctorno"):
            count = await get_redis().get(f"secondary_count:{queue['doctorno']}")
            queue["patientcount"] = count
        result["queues"].append(queue)
    return result


# ── 超声二级 ──
async def transform_pacs_secondary(contents: dict) -> dict:
    caches = contents.get("content", [])
    consultingroomname = contents.get("consultingroomname", [])
    result: dict[str, Any] = {"queues": []}
    for i, cache in enumerate(caches):
        if not cache:
            continue
        rows = _parse(cache)
        queue: dict[str, Any] = {"patients": []}
        for row in rows:
            if not queue.get("queuename"):
                if len(row) > 5 and row[5]:
                    queue["queuename"] = row[5]
                elif consultingroomname and i < len(consultingroomname) and consultingroomname[i]:
                    queue["queuename"] = consultingroomname[i]
                else:
                    queue["queuename"] = ""
            queue["patients"].append({
                "ticket": row[1] if len(row) > 1 else "",
                "brxmfull": row[2] if len(row) > 2 else "",
                "status": row[3] if len(row) > 3 else "",
            })
        result["queues"].append(queue)
    return result


# ── 检验 ──
async def transform_drawblood(cache: Any) -> dict:
    rows = _parse(cache)
    return {"patiens": [{
        "no": row[3] if len(row) > 3 else "",
        "name": row[5] if len(row) > 5 else "",
        "winno": row[0] if len(row) > 0 else "",
        "winname": row[1] if len(row) > 1 else "",
        "callvoice": row[7] if len(row) > 7 else "",
    } for row in rows]}


# ── 药房一级 ──
async def transform_pharmacy(cache: Any) -> dict:
    rows = _parse(cache)
    result: dict[str, Any] = {"patiens": []}
    for row in rows:
        if not result.get("deptno"):
            result["deptno"] = row[0]
        if not result.get("winno"):
            result["winno"] = row[1]
        result["patiens"].append({
            "no": row[2] if len(row) > 2 else "",
            "name": row[6] if len(row) > 6 else "",
            "namefull": row[3] if len(row) > 3 else "",
            "calltimes": row[4] if len(row) > 4 else "",
            "winno": row[1] if len(row) > 1 else "",
            "calltime": row[5] if len(row) > 5 else "",
        })
    return result


# ── 药房二级 ──
async def transform_pharmacy_secondary(cache: Any, is_multi_win: bool = False) -> dict:
    rows = _parse(cache)
    result: dict[str, Any] = {"waitPatients": [], "callPatients": [], "isMultiWin": is_multi_win}
    for row in rows:
        if not result.get("deptno"):
            result["deptno"] = row[0]
        if not result.get("winno"):
            result["winno"] = row[1]
            result["winname"] = row[8] if len(row) > 8 else ""
        import re
        name_clean = re.sub(r"[0-9]", "", str(row[4] if len(row) > 4 else ""))
        namefull_clean = re.sub(r"[0-9]", "", str(row[3] if len(row) > 3 else ""))
        if len(row) > 7 and row[7] == "2" and row[6]:
            result["callPatients"].append({
                "no": row[2] if len(row) > 2 else "",
                "name": name_clean,
                "namefull": namefull_clean,
                "calltimes": row[4] if len(row) > 4 else "",
                "winno": row[1],
                "winname": row[8] if len(row) > 8 else "",
                "calltime": row[6],
            })
        else:
            result["waitPatients"].append({
                "no": row[2] if len(row) > 2 else "",
                "name": name_clean,
                "namefull": namefull_clean,
                "winno": row[1],
                "winname": row[8] if len(row) > 8 else "",
            })
    result["callPatients"] = sorted(result["callPatients"], key=lambda x: x.get("calltime", ""), reverse=True)
    return result
