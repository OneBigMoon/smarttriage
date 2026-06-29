# -*- mode: python ; coding: utf-8 -*-
"""
PyInstaller 打包配置 —— SmartTriage 服务端。

使用方法（在 Windows 上）：
  1. 安装 Python 3.10+、Node.js 18+
  2. cd server
  3. npm run build              # 编译前端到 ../web/dist/
  4. pip install -r requirements.txt
  5. pip install pyinstaller
  6. pyinstaller build.spec

输出：dist/SmartTriage.exe
"""

import os
import sys
from pathlib import Path

# 项目根目录（build.spec 所在目录的上级）
_PROJ = Path(r"../").resolve()
# 前端构建产物
_WWW = _PROJ / "web" / "dist"
# 上传目录（运行时创建）
_UPLOADS = Path(r"./uploads")

block_cipher = None

# ── 收集前端静态文件 ──
# 将 web/dist/* 作为 app 的 data 文件打进 exe 同级目录的 web/dist/ 下
_www_files = []
if _WWW.is_dir():
    for f in _WWW.rglob("*"):
        if f.is_file():
            rel = f.relative_to(_WWW.parent)  # web/dist/*
            _www_files.append((str(f), str(rel.parent)))
    print(f"[build] web/dist/ 已就绪 — {len(_www_files)} 个文件")
else:
    print("WARNING: web/dist/ 不存在，前端静态文件不会被包含在 EXE 中。")
    print("WARNING: 在生产环境，后端将无法提供前端页面。")
    print("WARNING: 请先执行 npm run build 构建前端。")
    # 不 exit，让 PyInstaller 继续打包后端
    # 这样 CI 中即使前端构建有问题，也能先拿到后端的 exe

# 打包 settings.env 模板（首次运行自动同目录生成，这里仅作备选）
_env_tmpl = _PROJ / "server" / "settings.env"
if _env_tmpl.is_file():
    _www_files.append((str(_env_tmpl), "."))

a = Analysis(
    ["run.py"],
    pathex=[],
    binaries=[],
    datas=_www_files,
    hiddenimports=[
        # uvicorn 及其依赖
        "uvicorn",
        "uvicorn.logging",
        "uvicorn.loops",
        "uvicorn.loops.auto",
        "uvicorn.protocols",
        "uvicorn.protocols.http",
        "uvicorn.protocols.http.auto",
        "uvicorn.protocols.websockets",
        "uvicorn.protocols.websockets.auto",
        "uvicorn.middleware",
        "uvicorn.middleware.asgi",
        # ASGI 服务器
        "uvicorn.workers",
        # SQLAlchemy 引擎
        "sqlalchemy",
        "sqlalchemy.ext.asyncio",
        "asyncpg",
        # Redis
        "redis.asyncio",
        # Socket.IO
        "socketio",
        "engineio",
        "engineio.async_drivers.asgi",
        # JOSE
        "jose",
        "jose.constants",
        "jose.backends",
        # Passlib
        "passlib.handlers.pbkdf2",
        # 日志
        "loguru",
        # APScheduler
        "apscheduler.triggers.interval",
        # 应用模块
        "app",
        "app.config",
        "app.database",
        "app.dependencies",
        "app.utils",
        "app.models",
        "app.models.base",
        "app.models.box",
        "app.models.datasource",
        "app.models.datasource_type",
        "app.models.log_record",
        "app.models.organization",
        "app.models.style",
        "app.models.system_config",
        "app.models.template",
        "app.models.upgrade",
        "app.models.user",
        "app.routers",
        "app.routers.auth",
        "app.routers.boxes",
        "app.routers.datasources",
        "app.routers.discovery",
        "app.routers.organizations",
        "app.routers.simple_queries",
        "app.routers.system",
        "app.routers.templates",
        "app.routers.terminal",
        "app.routers.upgrades",
        "app.routers.uploads",
        "app.routers.apk",
        "app.services",
        "app.services.auth",
        "app.services.data_transform",
        "app.services.oracle_discover",
        "app.services.oracle_pool",
        "app.services.oracle_sync",
        "app.services.socketio_handler",
        # 系统托盘
        "pystray",
        "PIL",
        "PIL.Image",
        "PIL.ImageDraw",
        "PIL.ImageFont",
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        "tkinter",
        "matplotlib",
        "numpy",
        "pandas",
        "scipy",
        "PIL",
        "cv2",
        "tensorflow",
        "torch",
        "notebook",
        "jupyter",
        "ipython",
        "setuptools._distutils",
        "unittest",
        "test",
    ],
    # 使用 --windowed 模式，无控制台窗口
    win_no_window=True if sys.platform == "win32" else False,
    noarchive=False,
)

pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name="SmartTriage",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    # 单文件模式
    console=False,         # 无控制台窗口（后台运行）
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,
)