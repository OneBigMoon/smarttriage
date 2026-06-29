"""Upgrade (升级包) model."""
from __future__ import annotations

from sqlalchemy import BigInteger, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class Upgrade(Base, TimestampMixin):
    __tablename__ = "upgrades"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    model: Mapped[str] = mapped_column(String(64), default="")
    appVersion: Mapped[str] = mapped_column(String(32), default="")
    sortAppVersion: Mapped[int] = mapped_column(BigInteger, default=0)
    originname: Mapped[str] = mapped_column(String(256), default="")
    name: Mapped[str] = mapped_column(String(256), default="")
    path: Mapped[str] = mapped_column(String(512), default="")
    md5: Mapped[str] = mapped_column(String(64), default="")
