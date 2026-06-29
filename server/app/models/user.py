"""User (用户) model."""
from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import String, JSON, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class User(Base, TimestampMixin):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    salt: Mapped[str] = mapped_column(String(64), default="")
    hashedPassword: Mapped[str] = mapped_column(String(256), default="")
    auths: Mapped[Optional[list]] = mapped_column(JSON, default=list)    # ["AUTH_ROOT"]
    fullname: Mapped[str] = mapped_column(String(128), default="")
    profession: Mapped[Optional[str]] = mapped_column(String(64), default=None)
    praises: Mapped[Optional[int]] = mapped_column(default=None)
    lastlogintime: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), default=None)
    photoname: Mapped[Optional[str]] = mapped_column(String(128), default=None)
    photouri: Mapped[Optional[str]] = mapped_column(String(256), default=None)
