from __future__ import annotations
"""POST /api/v1/upload"""
import os
import uuid

from fastapi import APIRouter, Depends, UploadFile, File, Form
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import LogRecord, Upgrade, Box
from app.services.socketio_handler import emit_to_box
from app.utils import version_to_sort

router = APIRouter(prefix="/api/v1/upload", tags=["uploads"])


@router.post("")
async def upload(type: str = "", no: str = "", model: str = "", appVersion: str = "",
                 file: UploadFile = File(...), db: AsyncSession = Depends(get_db)):
    os.makedirs("uploads", exist_ok=True)
    ext = os.path.splitext(file.filename or "")[1]
    filename = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join("uploads", filename)
    content = await file.read()
    with open(file_path, "wb") as f:
        f.write(content)

    if type == "logfile":
        log = LogRecord(no=no, originname=file.filename or "", name=filename, path=file_path)
        db.add(log)
        await db.commit()
    elif type == "upgrade":
        from app.utils import md5 as calc_md5
        up = Upgrade(
            model=model, appVersion=appVersion, sortAppVersion=version_to_sort(appVersion),
            originname=file.filename or "", name=filename, path=os.path.join("uploads", filename),
            md5=calc_md5(content),
        )
        db.add(up)
        await db.commit()
        result = await db.execute(select(Box).where(Box.status == "正常"))
        for box in result.scalars().all():
            await emit_to_box(box.no, {"type": "COMMAND", "content": {"cmd": "upgrade"}})

    return {"errcode": 0}
