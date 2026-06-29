"""Template (模板) model.

Two kinds:
  "native" — Android uses built-in Fragment (legacy)
  "web"    — Android loads HTML/CSS/JS in WebView
"""
from __future__ import annotations

from typing import Optional

from sqlalchemy import String, Text, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class Template(Base, TimestampMixin):
    __tablename__ = "templates"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(128), default="")
    key: Mapped[str] = mapped_column(String(128), unique=True, default="")
    kind: Mapped[str] = mapped_column(String(16), default="web")      # native|web
    html: Mapped[str] = mapped_column(Text, default="")
    css: Mapped[str] = mapped_column(Text, default="")
    js: Mapped[str] = mapped_column(Text, default="")
    logo: Mapped[Optional[str]] = mapped_column(String(512), default=None)
    version: Mapped[str] = mapped_column(String(32), default="1.0.0")
    params: Mapped[Optional[dict]] = mapped_column(JSON, default=None)
    manifest: Mapped[Optional[dict]] = mapped_column(JSON, default=None)
