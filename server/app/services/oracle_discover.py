"""Oracle data discovery — auto-discover available data from HIS.

When Oracle is connected, query for available departments, screens, queues etc.
Admin picks from dropdown lists instead of manually entering IDs.
"""
from __future__ import annotations

from loguru import logger
from app.services import oracle_pool


async def discover_departments() -> list[dict]:
    """Query Oracle for all available departments (from primary triage view)."""
    try:
        system = await _get_system_config()
        if not system or not system.get("primarytablename"):
            return []
        sql = f"SELECT DISTINCT XSPID0, ZSMC00 FROM {system['primarytablename']} ORDER BY XSPID0"
        rows = await oracle_pool.query(sql)
        return [{"id": row[0], "name": row[1]} for row in rows]
    except Exception as e:
        logger.error(f"discover_departments error: {e}")
        return []


async def discover_screens() -> list[dict]:
    """Query Oracle for all available screens (from secondary triage view)."""
    try:
        system = await _get_system_config()
        if not system or not system.get("secondarytablename"):
            return []
        sql = f"SELECT DISTINCT XSPID0, ZSMC00 FROM {system['secondarytablename']} ORDER BY XSPID0"
        rows = await oracle_pool.query(sql)
        return [{"id": row[0], "name": row[1]} for row in rows]
    except Exception as e:
        logger.error(f"discover_screens error: {e}")
        return []


async def discover_queues() -> list[dict]:
    """Query Oracle for all available ultrasonic queues (from PACS view)."""
    try:
        system = await _get_system_config()
        if not system or not system.get("pacssecondarytablename"):
            return []
        sql = f"SELECT DISTINCT 队列名称 FROM {system['pacssecondarytablename']} ORDER BY 队列名称"
        rows = await oracle_pool.query_pacs(sql)
        return [{"name": row[0]} for row in rows]
    except Exception as e:
        logger.error(f"discover_queues error: {e}")
        return []


async def discover_windows() -> list[dict]:
    """Query Oracle for all available drawblood windows."""
    try:
        system = await _get_system_config()
        if not system or not system.get("drawbloodtablename"):
            return []
        sql = f"SELECT DISTINCT CYCKBH, CKMC00 FROM {system['drawbloodtablename']} ORDER BY CYCKBH"
        rows = await oracle_pool.query(sql)
        return [{"id": row[0], "name": row[1]} for row in rows]
    except Exception as e:
        logger.error(f"discover_windows error: {e}")
        return []


async def discover_pharmacy_depts() -> list[dict]:
    """Query Oracle for all available pharmacy departments."""
    try:
        system = await _get_system_config()
        if not system or not system.get("pharmacytablename"):
            return []
        sql = f"SELECT DISTINCT yfbmbh FROM {system['pharmacytablename']} ORDER BY yfbmbh"
        rows = await oracle_pool.query(sql)
        return [{"id": row[0]} for row in rows]
    except Exception as e:
        logger.error(f"discover_pharmacy_depts error: {e}")
        return []


async def discover_pharmacy_windows(dept_no: int) -> list[dict]:
    """Query Oracle for pharmacy windows of a specific department."""
    try:
        system = await _get_system_config()
        if not system or not system.get("pharmacytablename"):
            return []
        sql = f"SELECT DISTINCT fyckbh FROM {system['pharmacytablename']} WHERE yfbmbh = {dept_no} ORDER BY fyckbh"
        rows = await oracle_pool.query(sql)
        return [{"id": row[0]} for row in rows]
    except Exception as e:
        logger.error(f"discover_pharmacy_windows error: {e}")
        return []


async def _get_system_config() -> dict | None:
    from app.database import get_redis
    return await get_redis().hgetall("system")
