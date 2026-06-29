from __future__ import annotations
"""POST /api/v1/user/login, /user/logout"""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import User
from app.services.auth import create_token

router = APIRouter(prefix="/api/v1/user", tags=["auth"])
pwd_ctx = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")


@router.post("/login")
async def login(body: dict, db: AsyncSession = Depends(get_db)):
    username = body.get("username", "")
    password = body.get("password", "")
    if not username or not password:
        raise HTTPException(status_code=401, detail="invalid params")

    result = await db.execute(select(User).where(User.username == username))
    user = result.scalars().first()
    if not user or not user.hashedPassword or not pwd_ctx.verify(password, user.hashedPassword):
        raise HTTPException(status_code=401, detail="密码错误或用户不存在")

    user.lastlogintime = datetime.now(timezone.utc)
    await db.commit()

    token = create_token({"sub": str(user.id), "username": user.username, "auths": user.auths or []})
    return {
        "errcode": 0,
        "user": {"id": user.id, "username": user.username, "fullname": user.fullname, "auths": user.auths or []},
        "access_token": token,
    }


@router.post("/logout")
async def logout():
    return {"errcode": 0, "errmsg": "ok"}
