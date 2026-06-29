"""POST /api/v1/discovery/* — Auto-discover data from Oracle."""
from __future__ import annotations

from fastapi import APIRouter

from app.services import oracle_discover

router = APIRouter(prefix="/api/v1/discovery", tags=["discovery"])


@router.post("/departments")
async def discover_departments():
    """从Oracle获取所有可用科室列表"""
    result = await oracle_discover.discover_departments()
    return {"errcode": 0, "result": result}


@router.post("/screens")
async def discover_screens():
    """从Oracle获取所有可用分诊屏列表"""
    result = await oracle_discover.discover_screens()
    return {"errcode": 0, "result": result}


@router.post("/queues")
async def discover_queues():
    """从Oracle获取所有可用超声队列"""
    result = await oracle_discover.discover_queues()
    return {"errcode": 0, "result": result}


@router.post("/windows")
async def discover_windows():
    """从Oracle获取所有可用检验窗口"""
    result = await oracle_discover.discover_windows()
    return {"errcode": 0, "result": result}


@router.post("/pharmacy-depts")
async def discover_pharmacy_depts():
    """从Oracle获取所有药房部门"""
    result = await oracle_discover.discover_pharmacy_depts()
    return {"errcode": 0, "result": result}


@router.post("/pharmacy-windows")
async def discover_pharmacy_windows(body: dict):
    """从Oracle获取指定药房部门的窗口"""
    dept_no = body.get("dept_no", 0)
    from app.utils import safe_int
    result = await oracle_discover.discover_pharmacy_windows(safe_int(dept_no))
    return {"errcode": 0, "result": result}
