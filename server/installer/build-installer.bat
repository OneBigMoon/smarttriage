@echo off
chcp 65001 >nul
title SmartTriage 安装包制作工具 — 完全离线版

echo ╔══════════════════════════════════════════════════╗
echo ║      Smart Triage 离线安装包制作工具              ║
echo ║      全新 Win2008 无需任何预装依赖                ║
echo ╚══════════════════════════════════════════════════╝
echo.

setlocal enabledelayedexpansion
cd /d "%~dp0"

REM ═══════════════════════════════════════
REM  Step 1: 后端打包
REM ═══════════════════════════════════════
echo [1/5] 打包后端程序...
cd /d "%~dp0.."
call build.bat
if %errorlevel% neq 0 (
    echo [错误] 后端打包失败
    pause
    exit /b 1
)
copy /Y dist\SmartTriage.exe installer\SmartTriage.exe >nul
echo [✓] SmartTriage.exe 已就绪

REM ═══════════════════════════════════════
REM  Step 2: PostgreSQL 便携版
REM ═══════════════════════════════════════
echo [2/5] 准备 PostgreSQL 便携版...
if not exist installer\pgsql\bin\initdb.exe (
    echo   下载 PostgreSQL 10.23 for Windows (约 80MB)...
    mkdir installer\pgsql 2>nul

    powershell -Command "& {
        $url = 'https://get.enterprisedb.com/postgresql/postgresql-10.23-1-windows-x64-binaries.zip'
        $zip = 'pg.zip'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Write-Host '   下载中...' -NoNewline
        try {
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            Write-Host ' 完成'
            Write-Host '   解压中...' -NoNewline
            Expand-Archive -Path $zip -DestinationPath 'pg-tmp' -Force
            xcopy /E /Y /Q 'pg-tmp\pgsql\*' 'installer\pgsql\' >nul
            Remove-Item -Recurse -Force 'pg-tmp'
            Remove-Item $zip
            Write-Host ' 完成'
        } catch {
            Write-Host ''
            Write-Host '   [失败] ' + $_.Exception.Message
            exit 1
        }
    }"
    if !errorlevel! neq 0 (
        echo.
        echo [注意] 自动下载失败，请手动下载:
        echo   下载地址:
        echo   https://www.enterprisedb.com/download-postgresql-10.23-binaries
        echo   将 pgsql\ 目录解压到 installer\pgsql\
        echo.
        pause
    ) else (
        echo [✓] PostgreSQL 便携版已就绪 (!(dir installer\pgsql\bin\*.exe /b ^| find /c /v "" 2^>nul)! 个文件)
    )
) else (
    echo [✓] PostgreSQL 便携版已就绪
)

REM ═══════════════════════════════════════
REM  Step 3: Redis 便携版
REM ═══════════════════════════════════════
echo [3/5] 准备 Redis 便携版...
if not exist installer\redis\redis-server.exe (
    mkdir installer\redis 2>nul
    echo   下载 Redis for Windows (约 3MB)...

    powershell -Command "& {
        $url = 'https://github.com/microsoftarchive/redis/releases/download/win-3.2.100/Redis-x64-3.2.100.zip'
        $zip = 'redis.zip'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        try {
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            Expand-Archive -Path $zip -DestinationPath 'redis-tmp' -Force
            copy /Y 'redis-tmp\redis-server.exe' 'installer\redis\' >nul
            copy /Y 'redis-tmp\redis-cli.exe' 'installer\redis\' >nul
            Remove-Item -Recurse -Force 'redis-tmp'
            Remove-Item $zip
        } catch {
            exit 1
        }
    }"
    if !errorlevel! neq 0 (
        echo   [注意] 下载失败，请手动下载:
        echo   https://github.com/microsoftarchive/redis/releases
        echo   解压后将 redis-server.exe 放入 installer\redis\
        pause
    ) else (
        echo [✓] Redis 已就绪
    )
) else (
    echo [✓] Redis 已就绪
)

REM 生成 redis.conf
if not exist installer\redis\redis.conf (
    echo port 6379> installer\redis\redis.conf
    echo bind 127.0.0.1>> installer\redis\redis.conf
    echo daemonize no>> installer\redis\redis.conf
    echo logfile "" >> installer\redis\redis.conf
    echo databases 16>> installer\redis\redis.conf
    echo save "" >> installer\redis\redis.conf
)

REM ═══════════════════════════════════════
REM  Step 4: 检查 nssm
REM ═══════════════════════════════════════
echo [4/5] 检查 Windows 服务工具...
if not exist installer\nssm.exe (
    echo   下载 nssm (约 300KB)...

    powershell -Command "& {
        $url = 'https://nssm.cc/release/nssm-2.24-101-g897c7ad.zip'
        $zip = 'nssm.zip'
        try {
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            Expand-Archive -Path $zip -DestinationPath 'nssm-tmp' -Force
            copy /Y 'nssm-tmp\nssm-2.24-101-g897c7ad\win64\nssm.exe' 'installer\nssm.exe' >nul
            Remove-Item -Recurse -Force 'nssm-tmp'
            Remove-Item $zip
        } catch {
            exit 1
        }
    }"
    if !errorlevel! neq 0 (
        echo   [注意] 下载失败，请从 https://nssm.cc/download 下载
        echo   将 nssm.exe 放入 installer\ 目录
        pause
    ) else (
        echo [✓] nssm 已就绪
    )
) else (
    echo [✓] nssm 已就绪
)

REM ═══════════════════════════════════════
REM  Step 5: 编译安装包
REM ═══════════════════════════════════════
echo [5/5] 编译安装包...

where iscc >nul 2>&1
if !errorlevel! neq 0 (
    set ISCC="C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
    if not exist !ISCC! (
        echo [错误] 未找到 Inno Setup，请从 https://jrsoftware.org/isdl.php 下载
        pause
        exit /b 1
    )
) else (
    set ISCC=iscc
)

%ISCC% setup.iss
if !errorlevel! neq 0 (
    echo [错误] 安装包编译失败
    pause
    exit /b 1
)

echo.
echo ╔══════════════════════════════════════════════╗
echo ║   [✓] 离线安装包制作完成！                    ║
echo ║                                              ║
echo ║   输出: Output\SmartTriage-Setup-3.0.0.exe   ║
echo ║   大小: ~120MB                                ║
echo ║                                              ║
echo ║   此安装包完全自包含：                        ║
║   ✔ Python 运行时 + FastAPI 服务                  ║
║   ✔ React 前端管理界面                            ║
║   ✔ PostgreSQL 10.23 便携版（自动部署）            ║
║   ✔ Redis 便携版（自动启动）                       ║
║   ✔ nssm 服务管理器                               ║
║                                              ║
║   目标机无需任何预装依赖，离线安装。             ║
╚══════════════════════════════════════════════╝
echo.

REM 显示安装包大小
for %%I in (Output\SmartTriage-Setup-*.exe) do (
    set SZ=%%~zI
    set /A SZMB=!SZ!/1048576
    echo   安装包大小: !SZMB! MB
)

echo.
echo 使用方法：
echo   1. 将安装包复制到目标 Win2008 服务器
echo   2. 双击安装，一路"下一步"
echo   3. 安装程序会自动部署 PostgreSQL 和 Redis
echo   4. 安装完成自动启动服务
echo   5. 浏览器打开 http://localhost:7017 登录
echo   默认账号: admin / admin123
echo.
pause