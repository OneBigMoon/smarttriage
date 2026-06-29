"""Database connections: PostgreSQL (SQLAlchemy async) + Redis."""
from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from loguru import logger

from app.config import settings

# ── PostgreSQL ──

# ── 安全保护：防止误连生产库 ──
_db_url = settings.DATABASE_URL
_db_name = _db_url.rsplit("/", 1)[-1] if "/" in _db_url else "unknown"
_production_names = {"smarttriage", "smarttriage_db", "triage", "triage_db"}

if _db_name in _production_names:
    raise RuntimeError(
        f"\n{'='*60}"
        f"\n[安全保护] 数据库名 '{_db_name}' 可能是旧生产库！"
        f"\n新系统请使用不同的数据库，如 'smarttriage_new'"
        f"\n如需强行启动，请将 DATABASE_URL 改为其他数据库名"
        f"\n{'='*60}"
    )

if "localhost" not in _db_url and "127.0.0.1" not in _db_url:
    logger.warning(f"目标数据库 {_db_url[:50]}... 不在本机，请确认操作意图！")

engine = create_async_engine(_db_url, echo=False, pool_size=20, max_overflow=10)
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


async def get_db() -> AsyncSession:
    async with async_session() as session:
        yield session


async def connect_db() -> None:
    from app.models.base import Base
    from app.models import ALL_MODELS  # noqa

    # 启动前打印连接信息，让用户可追溯
    logger.info(f"  数据库: {_db_url[:70]}...")
    logger.info(f"  Redis:  {settings.REDIS_HOST}:{settings.REDIS_PORT}/{settings.REDIS_DB}")
    logger.info(f"  端口:   {settings.PORT}")

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    logger.info("Database ready: PostgreSQL (表已自动创建)")


async def close_db() -> None:
    await engine.dispose()


# ── Redis ──
redis_client = None


async def connect_redis() -> None:
    global redis_client
    import redis.asyncio as aioredis
    redis_client = aioredis.Redis(
        host=settings.REDIS_HOST, port=settings.REDIS_PORT,
        db=settings.REDIS_DB, decode_responses=True,
        socket_connect_timeout=3,
    )
    await redis_client.ping()
    logger.info("Redis connected")


async def close_redis() -> None:
    global redis_client
    if redis_client:
        await redis_client.close()


def get_redis():
    if redis_client is None:
        raise RuntimeError("Redis not connected — cannot proceed")
    return redis_client
