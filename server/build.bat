@echo off
chcp 65001 >nul
title SmartTriage 打包工具

echo ========================================
echo   Smart Triage 服务端打包工具
echo   Windows Server 2008 / 7+
echo ========================================
echo.

REM ── 检查 Python ──
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Python，请安装 Python 3.10+
    pause
    exit /b 1
)
echo [✓] Python %ERRORLEVEL% 就绪

REM ── 检查 Node.js ──
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请安装 Node.js 18+
    pause
    exit /b 1
)
echo [✓] Node.js 就绪

REM ── 构建前端 ──
echo.
echo [1/4] 构建前端静态文件...
cd /d "%~dp0..\web"
call npm install --silent
if %errorlevel% neq 0 (
    echo [错误] npm install 失败
    pause
    exit /b 1
)

call npm run build
if %errorlevel% neq 0 (
    echo [错误] npm run build 失败
    pause
    exit /b 1
)
echo [✓] 前端构建完成
echo       输出: web\dist\

REM ── 安装 Python 依赖 ──
echo.
echo [2/4] 安装 Python 依赖...
cd /d "%~dp0"
pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo [警告] pip install 有错误，继续尝试打包...
)

echo [✓] Python 依赖就绪

REM ── 安装 PyInstaller ──
echo.
echo [3/4] 安装 PyInstaller...
pip install pyinstaller
if %errorlevel% neq 0 (
    echo [错误] PyInstaller 安装失败
    pause
    exit /b 1
)
echo [✓] PyInstaller 就绪

REM ── 运行 PyInstaller ──
echo.
echo [4/4] 打包中（可能需要 2-5 分钟）...
echo.
pyinstaller build.spec
if %errorlevel% neq 0 (
    echo [错误] 打包失败，请查看上方错误信息
    pause
    exit /b 1
)

echo.
echo ========================================
echo   [✓] 打包完成！
echo   [✓] 输出: dist\SmartTriage.exe
echo ========================================
echo.
echo 使用方法：
echo   1. 双击 SmartTriage.exe 启动服务
echo   2. 浏览器会自动打开管理页面
echo   3. 如果要开机自启，请将 SmartTriage.exe
echo      放入 "启动" 文件夹或设为计划任务
echo.
echo 注意：启动前请确保 PostgreSQL 和 Redis
echo       服务已运行，并配置了正确的连接信息。
echo.
pause
