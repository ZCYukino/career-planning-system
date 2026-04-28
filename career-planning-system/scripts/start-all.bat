@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul 2>&1

REM ============================================
REM 大学生职业规划智能系统 - 一键启动脚本
REM 要求：JDK 21+、Maven 3.6+、Node.js 18+
REM       MySQL、Redis
REM ============================================

title 职业规划系统 - 一键启动

echo.
echo  ══════════════════════════════════════════
echo    大学生职业规划智能系统
echo    一键启动
echo  ══════════════════════════════════════════
echo.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "BACKEND_DIR=%SCRIPT_DIR%backend"
set "FRONTEND_DIR=%SCRIPT_DIR%frontend"

REM ──────────────────────────────────────────
REM  1. 环境预检
REM ──────────────────────────────────────────
echo  [预检] 正在检查运行环境...

REM 检查 Java
where java > nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 Java，请安装 JDK 21+
    goto :fail
)

REM 检查 Maven
where mvn > nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 Maven，请安装 Maven 3.6+
    goto :fail
)

REM 检查 Node.js
where node > nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 Node.js，请安装 Node.js 18+
    goto :fail
)

REM 检查包管理器
set "PKG_MANAGER="
where pnpm > nul 2>&1
if %errorlevel% equ 0 (
    set "PKG_MANAGER=pnpm"
) else (
    where npm > nul 2>&1
    if %errorlevel% equ 0 (
        set "PKG_MANAGER=npm"
    ) else (
        echo  [错误] 未找到 pnpm 或 npm
        goto :fail
    )
)

echo  [通过] 环境检查完毕
echo.

REM ──────────────────────────────────────────
REM  2. 检查 .env 配置
REM ──────────────────────────────────────────
echo  [配置] 检查环境配置...

if not exist "%PROJECT_ROOT%.env" (
    echo  [警告] 未找到 .env 文件
    if exist "%PROJECT_ROOT%.env.example" (
        copy "%PROJECT_ROOT%.env.example" "%PROJECT_ROOT%.env" > nul
        echo  [完成] 已从 .env.example 创建 .env 文件
        echo.
        echo  ╔══════════════════════════════════════════╗
        echo  ║  请先编辑 .env 文件，填入以下必填配置：   ║
        echo  ╠══════════════════════════════════════════╣
        echo  ║  1. DB_PASSWORD         数据库密码       ║
        echo  ║  2. DASHSCOPE_API_KEY   阿里云百炼 Key   ║
        echo  ║  3. OSS_ACCESS_KEY_ID   OSS AccessKey   ║
        echo  ║  4. OSS_ACCESS_KEY_SECRET OSS Secret    ║
        echo  ╚══════════════════════════════════════════╝
        echo.
        echo  编辑完成后，请重新运行此脚本
        goto :fail
    )
)

echo  [通过] .env 配置文件已就绪

REM ──────────────────────────────────────────
REM  3. 检查基础服务
REM ──────────────────────────────────────────
echo  [服务] 检查基础服务...

set "SVCS_OK=1"

powershell -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('localhost',3306); $c.Close(); exit 0 } catch { exit 1 }" > nul 2>&1
if %errorlevel% neq 0 (
    echo  [警告] MySQL（3306）未检测到
    set "SVCS_OK=0"
)

powershell -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('localhost',6379); $c.Close(); exit 0 } catch { exit 1 }" > nul 2>&1
if %errorlevel% neq 0 (
    echo  [警告] Redis（6379）未检测到，部分功能可能不可用
    set "SVCS_OK=0"
)

if "!SVCS_OK!"=="1" echo  [通过] 基础服务已就绪

REM ──────────────────────────────────────────
REM  4. 前端依赖检查
REM ──────────────────────────────────────────
echo  [依赖] 检查前端依赖...

if not exist "%FRONTEND_DIR%\node_modules" (
    echo  [安装] 正在安装前端依赖，请稍候...
    cd /d "%FRONTEND_DIR%"
    call !PKG_MANAGER! install
    if %errorlevel% neq 0 (
        echo  [错误] 前端依赖安装失败
        goto :fail
    )
    echo  [完成] 前端依赖安装成功
) else (
    echo  [通过] 前端依赖已就绪
)

REM ──────────────────────────────────────────
REM  5. 启动服务
REM ──────────────────────────────────────────
echo.
echo  [启动] 正在启动所有服务...
echo.

REM 启动后端（新窗口）
start "后端服务 - Spring Boot" cmd /k "cd /d "%BACKEND_DIR%" && mvn spring-boot:run"

REM 等待后端初始化
echo  [等待] 后端服务初始化中（约 20 秒）...
timeout /t 20 /nobreak > nul

REM 启动前端（新窗口）
start "前端服务 - Vite Dev Server" cmd /k "cd /d "%FRONTEND_DIR%" && !PKG_MANAGER! run dev"

echo.
echo  ══════════════════════════════════════════
echo    所有服务已启动
echo  ══════════════════════════════════════════
echo.
echo  前端地址：http://localhost:5173
echo  后端地址：http://localhost:8080
echo.
echo  [提示] 请等待后端启动完成后访问前端页面
echo  [提示] 关闭时请关闭两个命令行窗口
echo.
pause
exit /b 0

:fail
echo.
echo  [启动失败] 请排查以上问题后重试
pause
exit /b 1
