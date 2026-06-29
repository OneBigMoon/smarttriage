"""LogRecord (日志) model."""
from __future__ import annotations

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class LogRecord(Base, TimestampMixin):
    __tablename__ = "log_records"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    no: Mapped[str] = mapped_column(String(32), default="", index=True)
    originname: Mapped[str] = mapped_column(String(256), default="")
    name: Mapped[str] = mapped_column(String(256), default="")
    path: Mapped[str] = mapped_column(String(512), default="")
