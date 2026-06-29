"""Smart Triage — 生产启动入口（系统托盘模式）。

双击 exe 后：
  1. 右下角任务栏出现图标
  2. 后台启动 FastAPI + Socket.IO 服务
  3. 右键图标菜单：打开管理页面 / 退出
"""
import os
import sys
import webbrowser
import threading
import time
import socket
import subprocess
import signal
import atexit
from pathlib import Path

# ── 加载配置（托盘图标前置） ──

def _load_or_create_env():
    """加载 settings.env，不存在则创建模板并退出提示。"""
    env_path = Path(__file__).parent / "settings.env"

    if not env_path.exists():
        _create_env_template(env_path)
        print(f"[SmartTriage] 已创建配置文件：{env_path}")
        print("[SmartTriage] 请编辑后重新启动。")
        if getattr(sys, "frozen", False):
            import ctypes
            ctypes.windll.user32.MessageBoxW(0,
                f"已创建配置文件：\n{env_path}\n\n请编辑后重新启动程序。",
                "Smart Triage", 0)
        sys.exit(0)

    with open(env_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, val = line.partition("=")
            os.environ.setdefault(key.strip(), val.strip().strip('"').strip("'"))


def _create_env_template(path: Path):
    content = r"""# ═══════════════════════════════════════════════
# Smart Triage — 服务端配置
# ═══════════════════════════════════════════════
# 编辑后保存，重启生效。首次启动自动创建数据库表。

PORT=7017
DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/smarttriage_new
# Redis — 安装包内置了 redis-server.exe，自动启动，无需额外安装
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DB=1
JWT_SECRET=smarttriage_dev_secret_change_me
LOG_LEVEL=info
"""
    path.write_text(content, encoding="utf-8")


# ── 内置 Redis 侧车进程 ──

_redis_proc: subprocess.Popen | None = None

def _start_bundled_redis(port: int = 6379, db: int = 1) -> bool:
    """尝试启动同目录下的 redis-server.exe，作为内置 Redis。

    打包目录结构：
      SmartTriage.exe
      redis/
        redis-server.exe
        redis.conf

    如果 redis-server.exe 不存在，说明机器已装好 Redis，跳过。
    """
    global _redis_proc

    # 找 redis-server.exe
    base = Path(sys.argv[0]).parent if getattr(sys, "frozen", False) else Path(".")
    redis_exe = base / "redis" / "redis-server.exe"
    if not redis_exe.is_file():
        # 没有内置 Redis，让外部 Redis 连接
        return False

    # 检查端口是否已被占用（外部 Redis 已在运行）
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2)
        s.connect(("127.0.0.1", port))
        s.close()
        print(f"[Redis] 端口 {port} 已被占用，使用外部 Redis")
        return True  # 外部 Redis 已运行，不用启动
    except (ConnectionRefusedError, OSError):
        pass

    # 启动内置 Redis
    redis_conf = base / "redis" / "redis.conf"
    try:
        _redis_proc = subprocess.Popen(
            [str(redis_exe), str(redis_conf)] if redis_conf.is_file()
            else [str(redis_exe), "--port", str(port), "--db", str(db)],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0,
        )
        print(f"[Redis] 内置 Redis 已启动 (端口 {port}, DB {db})")
        return True
    except Exception as e:
        print(f"[Redis] 启动失败: {e}")
        return False


def _stop_bundled_redis():
    """退出时清理内置 Redis 进程。"""
    global _redis_proc
    if _redis_proc and _redis_proc.poll() is None:
        _redis_proc.terminate()
        try:
            _redis_proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            _redis_proc.kill()
        print("[Redis] 内置 Redis 已停止")


# ── 系统托盘 ──

def _create_tray_icon(port: int):
    """创建系统托盘图标，阻塞直到用户选择退出。"""
    try:
        from PIL import Image, ImageDraw, ImageFont
    except ImportError:
        # 没有 Pillow 时降级为纯命令行模式
        return _run_console_mode(port)

    try:
        import pystray
    except ImportError:
        return _run_console_mode(port)

    # 生成图标：64x64 蓝色方块 + 白色 T 字
    img = Image.new("RGBA", (64, 64), (59, 130, 246, 255))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("segoeui.ttf", 36)
    except Exception:
        font = ImageFont.load_default()
    bbox = draw.textbbox((0, 0), "T", font=font)
    tx = (64 - (bbox[2] - bbox[0])) // 2
    ty = (64 - (bbox[3] - bbox[1])) // 2
    draw.text((tx, ty), "T", fill="white", font=font)

    port_str = str(port)

    def on_open(icon, item):
        webbrowser.open(f"http://localhost:{port}/")

    def on_show_url(icon, item):
        import ctypes
        ctypes.windll.user32.MessageBoxW(0,
            f"管理地址: http://localhost:{port}/\n\n服务器已在后台运行。",
            "Smart Triage", 0)

    def on_exit(icon, item):
        icon.stop()
        # 强制退出（daemon 线程随进程终止）
        os._exit(0)

    menu = pystray.Menu(
        pystray.MenuItem("打开管理页面", on_open, default=True),
        pystray.Menu.SEPARATOR,
        pystray.MenuItem(f"运行端口: {port}", on_show_url),
        pystray.Menu.SEPARATOR,
        pystray.MenuItem("退出", on_exit),
    )

    icon = pystray.Icon("SmartTriage", img, "Smart Triage 管理平台", menu)
    icon.run()


def _run_console_mode(port: int):
    """降级模式：无托盘时在控制台等待按回车退出。"""
    print(f"\n  Smart Triage 服务已启动")
    print(f"  管理页面: http://localhost:{port}/")
    print(f"  按 Ctrl+C 停止服务\n")
    try:
        threading.Event().wait()
    except KeyboardInterrupt:
        pass


# ── 服务启动 ──

def _run_server(port: int, host: str):
    """在线程中启动 uvicorn 服务。"""
    import uvicorn
    from app.main import socket_app

    uvicorn.run(
        socket_app,
        host=host,
        port=port,
        reload=False,
        log_level=os.environ.get("LOG_LEVEL", "info"),
    )


def _run_service_mode(port: int, host: str):
    """服务模式：无托盘、无浏览器，由 nssm 管理生命周期。"""
    import uvicorn
    from app.main import socket_app
    uvicorn.run(socket_app, host=host, port=port, reload=False,
                log_level=os.environ.get("LOG_LEVEL", "info"))


def _run_interactive_mode(port: int, host: str):
    """交互模式：托盘图标 + 自动打开浏览器。"""
    server_thread = threading.Thread(
        target=_run_server, args=(port, host), daemon=True, name="uvicorn"
    )
    server_thread.start()
    time.sleep(1.5)
    webbrowser.open(f"http://localhost:{port}/")
    _create_tray_icon(port)


def main():
    _load_or_create_env()

    # 确保关键配置有默认值
    os.environ.setdefault("DATABASE_URL",
        "postgresql+asyncpg://postgres:postgres@localhost:5432/smarttriage_new")
    os.environ.setdefault("REDIS_HOST", "localhost")
    os.environ.setdefault("REDIS_PORT", "6379")
    os.environ.setdefault("REDIS_DB", "1")

    is_service = "--service" in sys.argv[1:]
    port = int(os.environ.get("PORT", "7017") if not is_service else "7017")
    host = os.environ.get("HOST", "0.0.0.0")
    os.environ["PORT"] = str(port)

    # ── 端口检测：如果服务已在运行，只打开浏览器后退出 ──
    # 场景：用户安装了 Windows 服务（后台运行），又点了桌面快捷方式
    if not is_service:
        _sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            _sock.connect(("127.0.0.1", port))
            _sock.close()
            # 端口已被占用 → 服务已在运行，只打开浏览器
            webbrowser.open(f"http://localhost:{port}/")
            print(f"[SmartTriage] 服务已在 http://localhost:{port} 运行")
            print("[SmartTriage] 已打开浏览器管理页面")
            sys.exit(0)
        except ConnectionRefusedError:
            pass
        finally:
            _sock.close()

    # ── 安全保护：数据库名校验 ──
    _db_url = os.environ.get("DATABASE_URL", "(未设置)")
    _db_name = _db_url.rsplit("/", 1)[-1] if "/" in _db_url else ""
    _production_names = {"smarttriage", "smarttriage_db", "triage", "triage_db"}
    if _db_name in _production_names:
        msg = (f"[安全保护] 数据库名 '{_db_name}' 可能是旧生产库！\n"
               f"请修改 settings.env 中的 DATABASE_URL")
        print(f"\n{'='*60}\n{msg}\n{'='*60}")
        if getattr(sys, "frozen", False):
            import ctypes
            ctypes.windll.user32.MessageBoxW(0, msg, "Smart Triage", 0)
        sys.exit(1)

    # ── 启动内置 Redis（离线场景无需安装） ──
    _redis_port = int(os.environ.get("REDIS_PORT", "6379"))
    _redis_db = int(os.environ.get("REDIS_DB", "1"))
    _start_bundled_redis(_redis_port, _redis_db)
    atexit.register(_stop_bundled_redis)

    # ── 打印启动信息 ──
    if not is_service:
        _redis_status = "内置" if _redis_proc else "外部"
        print(f"""
╔══════════════════════════════════════════════╗
║          Smart Triage 服务端                  ║
╠══════════════════════════════════════════════╣
║  端口      {port:<5}                          ║
║  数据库    {_db_url[:50]:<50} ║
║  Redis     {'内置(免装)' if _redis_proc else '外部' :<16} ║
║  模式      {'Windows 服务' if is_service else '托盘 + 浏览器' :<16} ║
╚══════════════════════════════════════════════╝
        """)

    # ── 启动 ──
    if is_service:
        _run_service_mode(port, host)
    else:
        _run_interactive_mode(port, host)


if __name__ == "__main__":
    main()