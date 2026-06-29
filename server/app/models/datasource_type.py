"""DatasourceType (数据源类型) model."""
from __future__ import annotations

from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class DatasourceType(Base):
    __tablename__ = "datasource_types"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(64), default="")
    key: Mapped[str] = mapped_column(String(64), default="")
