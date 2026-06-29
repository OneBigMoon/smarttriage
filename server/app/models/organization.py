"""Organization (分组) model.

Old system uses integer _id: -1 (root "未分组"), 0 (root "默认组"), 1, 2, ...
idpath format: "1.2.3." (trailing dot) for tree queries.
"""
from __future__ import annotations
from typing import Optional

from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class Organization(Base, TimestampMixin):
    __tablename__ = "organizations"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(128), default="")
    parentid: Mapped[Optional[int]] = mapped_column(Integer, default=None)
    idpath: Mapped[str] = mapped_column(String(256), default="", index=True)
    user: Mapped[Optional[str]] = mapped_column(String(64), default=None)
