"""Oracle connection pool — dual pool (main + PACS), config from Redis.

Key design:
- Pools are lazily initialized and cached
- Config is read from Redis hash 'system' (same as old system)
- Oracle operations run in thread pool via asyncio.to_thread to avoid blocking the async event loop
"""
from __future__ import annotations

import asyncio
import oracledb
from loguru import logger

from app.database import get_redis

_pool: oracledb.Pool | None = None
_pool_pacs: oracledb.Pool | None = None
_cached_cfg: dict = {}


async def _get_config() -> dict:
    system = await get_redis().hgetall("system")
    if not system:
        raise RuntimeError("system config not found — configure via /api/v1/system/save first")
    return system


async def _get_pool() -> oracledb.Pool:
    global _pool, _cached_cfg
    cfg = await _get_config()
    if _pool and all(cfg.get(k) == _cached_cfg.get(k) for k in ("username", "password", "url")):
        return _pool
    if _pool:
        try:
            _pool.close(force=True)
        except Exception as e:
            logger.warning(f"Error closing old Oracle pool: {e}")
    _pool = oracledb.create_pool(
        user=cfg.get("username", ""),
        password=cfg.get("password", ""),
        dsn=cfg.get("url", ""),
        min=5, max=20, increment=5,
    )
    _cached_cfg.update(cfg)
    logger.info("Oracle main pool created")
    return _pool


async def _get_pool_pacs() -> oracledb.Pool:
    global _pool_pacs, _cached_cfg
    cfg = await _get_config()
    if _pool_pacs and all(cfg.get(k) == _cached_cfg.get(k) for k in ("pacsusername", "pacspassword", "url")):
        return _pool_pacs
    if _pool_pacs:
        try:
            _pool_pacs.close(force=True)
        except Exception as e:
            logger.warning(f"Error closing old PACS pool: {e}")
    _pool_pacs = oracledb.create_pool(
        user=cfg.get("pacsusername", ""),
        password=cfg.get("pacspassword", ""),
        dsn=cfg.get("url", ""),
        min=2, max=10, increment=2,
    )
    _cached_cfg.update(cfg)
    logger.info("Oracle PACS pool created")
    return _pool_pacs


def _sync_query(pool: oracledb.Pool, sql: str) -> list[list]:
    """Synchronous Oracle query — called via asyncio.to_thread."""
    with pool.acquire() as conn:
        cursor = conn.cursor()
        cursor.execute(sql)
        rows = cursor.fetchall()
        return [list(row) for row in rows]


async def query(sql: str) -> list[list]:
    """Execute SQL against main Oracle pool (non-blocking via thread pool)."""
    pool = await _get_pool()
    return await asyncio.to_thread(_sync_query, pool, sql)


async def query_pacs(sql: str) -> list[list]:
    """Execute SQL against PACS Oracle pool (non-blocking via thread pool)."""
    pool = await _get_pool_pacs()
    return await asyncio.to_thread(_sync_query, pool, sql)
