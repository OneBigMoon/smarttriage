"""Box (终端) model."""
from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import Integer, String, DateTime, Index
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class Box(Base, TimestampMixin):
    __tablename__ = "boxes"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    no: Mapped[str] = mapped_column(String(32), unique=True, index=True, default="")
    name: Mapped[str] = mapped_column(String(128), default="")
    org: Mapped[Optional[int]] = mapped_column(Integer, default=None)  # FK → organization.id
    ip: Mapped[str] = mapped_column(String(64), default="")
    mac: Mapped[str] = mapped_column(String(64), default="")
    model: Mapped[str] = mapped_column(String(64), default="")        # product flavor
    appversion: Mapped[str] = mapped_column(String(32), default="")
    style: Mapped[Optional[str]] = mapped_column(String(64), default=None)
    datasource: Mapped[Optional[str]] = mapped_column(String(64), default=None)  # FK → datasource.id (str)
    template: Mapped[Optional[str]] = mapped_column(String(64), default=None)     # FK → template.id (str)
    status: Mapped[str] = mapped_column(String(16), default="正常")   # 正常|关机|断开
    user: Mapped[Optional[str]] = mapped_column(String(64), default=None)
    powerontime: Mapped[Optional[str]] = mapped_column(String(16), default=None)  # HH:MM:SS
    powerofftime: Mapped[Optional[str]] = mapped_column(String(16), default=None)
    volume: Mapped[Optional[int]] = mapped_column(Integer, default=None)           # 0-9
    horselamp: Mapped[Optional[str]] = mapped_column(String(512), default=None)
    title: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    winname: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    rotation: Mapped[Optional[str]] = mapped_column(String(16), default=None)     # auto|0|90|180|270
    dataenabled: Mapped[int] = mapped_column(Integer, default=0)                   # 0=off, 1=on
    ht: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), default=None)  # heartbeat

    __table_args__ = (
        Index("ix_boxes_org", "org"),
        Index("ix_boxes_status", "status"),
    )
