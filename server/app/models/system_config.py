"""SystemConfig (系统配置) model — Oracle connection parameters."""
from __future__ import annotations

from typing import Optional

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class SystemConfig(Base):
    __tablename__ = "system_config"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    url: Mapped[Optional[str]] = mapped_column(String(256), default=None)
    primarytablename: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    secondarytablename: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    secondarycounttablename: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    drawbloodtablename: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    pharmacytablename: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    username: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    password: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    pacssecondarytablename: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    pacsusername: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    pacspassword: Mapped[Optional[str]] = mapped_column(String(128), default=None)
