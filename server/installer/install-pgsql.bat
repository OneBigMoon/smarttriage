@echo off
chcp 65001 >nul
title SmartTriage — PostgreSQL 部署

REM ════════════════════════════════════════════════════════════════
REM  Smart Triage 安装程序：PostgreSQL 便携版部署脚本
REM
REM  检测目标机是否已有 PostgreSQL，如果没有则从安装包部署。
REM  支持 Windows Server 2008 / 7+
REM ════════════════════════════════════════════════════════════════

set PG_VERSION=10.23
set PG_PORT=5432
set PG_USER=postgres
set PG_PASS=postgres
set PG_DB=smarttriage_new

REM ── 安装目录（由 Inno Setup 传入） ──
set APP_DIR=%~1
if "%APP_DIR%"=="" set APP_DIR=C:\Program Files\SmartTriage

REM ── 检测是否已有 PostgreSQL ──

set PG_FOUND=
set PG_BIN=

REM 检查注册表（标准 PostgreSQL 安装）
for /f "skip=2 tokens=2*" %%A in ('reg query "HKLM\SOFTWARE\PostgreSQL\Services" /s 2^>nul') do (
    if /i "%%A"=="Base Directory" set PG_FOUND=1&& set PG_BIN=%%B\bin
)
if not defined PG_BIN (
    for /f "skip=2 tokens=2*" %%A in ('reg query "HKLM\SOFTWARE\PostgreSQL" /s 2^>nul') do (
        if /i "%%A"=="Base Directory" set PG_FOUND=1&& set PG_BIN=%%B\bin
    )
)

REM 检查 pg_isready 是否在 PATH 中
if not defined PG_FOUND (
    where pg_isready >nul 2>&1
    if !errorlevel! equ 0 (
        set PG_FOUND=1
        for /f "delims=" %%A in ('where pg_isready') do set PG_BIN=%%~dpA
    )
)

REM ── 如果已发现 PostgreSQL ──
if defined PG_FOUND (
    echo [PostgreSQL] 检测到已有安装: %PG_BIN%
    goto create_db
)

REM ── 部署便携版 PostgreSQL ──
echo [PostgreSQL] 未检测到已有安装，正在部署便携版...
if not exist "%APP_DIR%\pgsql\bin\initdb.exe" (
    echo [PostgreSQL] 错误：便携版 PostgreSQL 安装包缺失！
    echo [PostgreSQL] 预期位置: %APP_DIR%\pgsql\bin\initdb.exe
    pause
    exit /b 1
)

set PG_BIN=%APP_DIR%\pgsql\bin
set PG_DATA=%APP_DIR%\pgsql\data

if exist "%PG_DATA%" (
    echo [PostgreSQL] 检测到已有数据目录，跳过初始化
    goto start_pg
)

echo [PostgreSQL] 初始化数据库 (数据目录: %PG_DATA%)...
"%PG_BIN%\initdb.exe" -D "%PG_DATA%" -U postgres --encoding=UTF8 --lc-collate=C --lc-ctype=C
if %errorlevel% neq 0 (
    echo [PostgreSQL] initdb 失败！
    pause
    exit /b 1
)

REM 配置监听地址
echo listen_addresses = '127.0.0.1' >> "%PG_DATA%\postgresql.conf"
echo port = %PG_PORT% >> "%PG_DATA%\postgresql.conf"

REM 配置密码认证
echo local all all trust >> "%PG_DATA%\pg_hba.conf"
echo host  all all 127.0.0.1/32 md5 >> "%PG_DATA%\pg_hba.conf"
echo host  all all ::1/128 md5 >> "%PG_DATA%\pg_hba.conf"

echo [PostgreSQL] 初始化完成

:start_pg
REM 启动 PostgreSQL
echo [PostgreSQL] 启动数据库服务...
"%PG_BIN%\pg_ctl.exe" start -D "%PG_DATA%" -w -t 30 -l "%APP_DIR%\pgsql\pg.log"
if %errorlevel% neq 0 (
    echo [PostgreSQL] 启动失败，尝试强制启动...
    "%PG_BIN%\pg_ctl.exe" start -D "%PG_DATA%" -w -t 60
)

REM 注册为 Windows 服务（开机自启）
"%PG_BIN%\pg_ctl.exe" register -N "SmartTriage_PostgreSQL" -D "%PG_DATA%" -w
echo [PostgreSQL] 已注册为 Windows 服务: SmartTriage_PostgreSQL
net start SmartTriage_PostgreSQL >nul 2>&1

:create_db
REM 创建数据库
echo [PostgreSQL] 创建数据库: %PG_DB% ...
"%PG_BIN%\psql.exe" -U %PG_USER% -d postgres -c "SELECT 1 FROM pg_database WHERE datname='%PG_DB%'" 2>nul | find "1" >nul
if %errorlevel% neq 0 (
    "%PG_BIN%\createdb.exe" -U %PG_USER% -E UTF8 %PG_DB%
    if %errorlevel% equ 0 (
        echo [PostgreSQL] 数据库 %PG_DB% 创建成功
    ) else (
        echo [PostgreSQL] 创建数据库 %PG_DB% 失败（可能已存在，跳过）
    )
) else (
    echo [PostgreSQL] 数据库 %PG_DB% 已存在，跳过
)

REM 设置密码（便携版默认 trust 认证，设密码不重要但保持兼容）
"%PG_BIN%\psql.exe" -U %PG_USER% -d postgres -c "ALTER USER postgres PASSWORD '%PG_PASS%';" >nul 2>&1

echo.
echo [PostgreSQL] 部署完成！
echo   主机: localhost
echo   端口: %PG_PORT%
echo   数据库: %PG_DB%
echo   用户: %PG_USER%
echo.
exit /b 0