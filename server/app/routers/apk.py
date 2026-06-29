from __future__ import annotations
"""POST /api/v1/apk/* — APK management + QR code config generation."""
import json
import os
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Box

router = APIRouter(prefix="/api/v1/apk", tags=["apk"])

APK_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "apk")
os.makedirs(APK_DIR, exist_ok=True)


@router.post("/upload")
async def upload_apk(file: UploadFile = File(...), version: str = Form(""),
                     notes: str = Form(""), flavor: str = Form("default")):
    if not file.filename or not file.filename.endswith(".apk"):
        raise HTTPException(400, "only .apk files accepted")
    filename = f"triage-{version}-{flavor}-{uuid.uuid4().hex[:8]}.apk"
    file_path = os.path.join(APK_DIR, filename)
    content = await file.read()
    with open(file_path, "wb") as f:
        f.write(content)
    index = _load_index()
    entry = {"id": uuid.uuid4().hex, "filename": filename, "original": file.filename,
             "version": version, "flavor": flavor, "notes": notes, "size": len(content),
             "uploaded_at": datetime.now(timezone.utc).isoformat()}
    index.append(entry)
    _save_index(index)
    return {"errcode": 0, "result": entry}


@router.post("/list")
async def list_apks():
    return {"errcode": 0, "result": _load_index()}


@router.post("/remove")
async def remove_apk(id: str):
    index = _load_index()
    entry = next((e for e in index if e["id"] == id), None)
    if not entry:
        raise HTTPException(404, "not found")
    fp = os.path.join(APK_DIR, entry["filename"])
    if os.path.exists(fp):
        os.remove(fp)
    _save_index([e for e in index if e["id"] != id])
    return {"errcode": 0}


@router.get("/download/{apk_id}")
async def download_apk(apk_id: str):
    index = _load_index()
    entry = next((e for e in index if e["id"] == apk_id), None)
    if not entry:
        raise HTTPException(404, "not found")
    fp = os.path.join(APK_DIR, entry["filename"])
    if not os.path.exists(fp):
        raise HTTPException(404, "file not found")
    return FileResponse(fp, filename=entry["original"], media_type="application/vnd.android.package-archive")


@router.get("/latest")
async def latest_apk(flavor: str = "default"):
    index = _load_index()
    flavor_apks = [e for e in index if e.get("flavor") == flavor or flavor == "all"]
    if not flavor_apks:
        return {"errcode": 0, "result": None}
    latest = sorted(flavor_apks, key=lambda e: e.get("version", ""), reverse=True)[0]
    return {"errcode": 0, "result": latest}


@router.post("/batch-qr")
async def batch_qr(body: dict, db: AsyncSession = Depends(get_db)):
    server_ip = body.get("server_ip", "")
    server_port = body.get("server_port", "7016")
    count = body.get("count", 10)
    prefix = body.get("prefix", "BOX")
    if not server_ip:
        raise HTTPException(400, "server_ip required")
    host = f"{server_ip}:{server_port}" if server_port else server_ip
    result = await db.execute(select(Box).order_by(Box.no.desc()).limit(1))
    last = result.scalars().first()
    max_num = 0
    if last:
        try:
            max_num = int(last.no.replace(prefix, ""))
        except ValueError:
            pass
    return {"errcode": 0, "result": [
        {"server": host, "no": f"{prefix}{max_num + i + 1:03d}", "version": "3.0.0"}
        for i in range(count)
    ]}


def _index_path():
    return os.path.join(APK_DIR, "index.json")

def _load_index() -> list:
    try:
        with open(_index_path(), "r") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []

def _save_index(index: list):
    with open(_index_path(), "w") as f:
        json.dump(index, f, indent=2, ensure_ascii=False)
