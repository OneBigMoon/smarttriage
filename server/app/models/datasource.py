"""Datasource (数据源) model."""
from __future__ import annotations

from typing import Optional

from sqlalchemy import Integer, String, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class Datasource(Base, TimestampMixin):
    __tablename__ = "datasources"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(128), default="")
    type: Mapped[str] = mapped_column(String(64), default="", index=True)
    # Array fields stored as JSONB — small arrays, read-heavy
    departmentid: Mapped[Optional[list]] = mapped_column(JSON, default=list)
    screenid: Mapped[Optional[int]] = mapped_column(Integer, default=None)
    screensplitid: Mapped[Optional[list]] = mapped_column(JSON, default=list)
    queue: Mapped[Optional[list]] = mapped_column(JSON, default=list)             # PACS queue names
    consultingroomname: Mapped[Optional[list]] = mapped_column(JSON, default=None)
    windowid: Mapped[Optional[list]] = mapped_column(JSON, default=list)          # drawblood window IDs
    pharmacydeptno: Mapped[Optional[int]] = mapped_column(Integer, default=None)
    pharmacywinno: Mapped[Optional[list]] = mapped_column(JSON, default=list)
    morningcleartime: Mapped[Optional[str]] = mapped_column(String(16), default=None)
    afternooncleartime: Mapped[Optional[str]] = mapped_column(String(16), default=None)
