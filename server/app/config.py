"""Application configuration via environment variables (loaded from settings.env)."""
from __future__ import annotations

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    HOST: str = "0.0.0.0"
    PORT: int = 7017                     # 与旧系统并行，默认用不同端口

    # Database — PostgreSQL
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/smarttriage_new"

    # Redis
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 1                    # 不同 DB 编号，避免与旧系统缓存冲突

    # JWT
    JWT_SECRET: str = ""
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_HOURS: int = 12

    CORS_ORIGINS: list[str] = ["*"]
    UPLOAD_DIR: str = "uploads"

    # pydantic 从 os.environ 读取（字段名即环境变量名）
    # 开发模式可使用 .env 文件（放到 server/ 目录下）
    model_config = {"env_prefix": "", "env_file": ".env"}


settings = Settings()