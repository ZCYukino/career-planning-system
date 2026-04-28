@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul 2>&1

REM ============================================
REM 大学生职业规划智能系统 - 前端启动脚本
REM 要求：Node.js 18+、pnpm（推荐）或 npm
REM ============================================

title 职业规划系统 - 前端服务

echo.
echo  ══════════════════════════════════════════
echo    大学生职业规划智能系统 - 前端启动
echo  ══════════════════════════════════════════
echo.

REM ─── 定位目录 ───
set "SCRIPT_DIR=%~dp0"
set "FRONTEND_DIR=%SCRIPT_DIR%..\frontend"

REM ──────────────────────────────────────────
REM  1. 检查 Node.js 环境（要求 18+）
REM ──────────────────────────────────────────
echo  [1/4] 检查 Node.js 环境...

where node > nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 Node.js，请安装 Node.js 18 或更高版本
    echo  [提示] 下载地址：https://nodejs.org/
    goto :fail
)

for /f "tokens=1" %%v in ('node -v 2^>^&1') do (
    set "NODE_VER=%%v"
)

REM 去掉 v 前缀
set "NODE_VER_NUM=!NODE_VER:v=!"

for /f "tokens=1 delims=." %%a in ("!NODE_VER_NUM!") do (
    set "NODE_MAJOR=%%a"
)

if !NODE_MAJOR! lss 18 (
    echo  [错误] Node.js 版本过低：!NODE_VER!，项目要求 Node.js 18+
    echo  [提示] 请前往 https://nodejs.org/ 下载最新 LTS 版本
    goto :fail
)

echo  [通过] Node.js !NODE_VER!

REM ──────────────────────────────────────────
REM  2. 检测并选择包管理器
REM ──────────────────────────────────────────
echo  [2/4] 检查包管理器...

set "PKG_MANAGER="

where pnpm > nul 2>&1
if %errorlevel% equ 0 (
    set "PKG_MANAGER=pnpm"
    for /f "tokens=1" %%v in ('pnpm -v 2^>^&1') do (
        echo  [通过] pnpm %%v
    )
) else (
    where npm > nul 2>&1
    if %errorlevel% equ 0 (
        set "PKG_MANAGER=npm"
        for /f "tokens=1" %%v in ('npm -v 2^>^&1') do (
            echo  [通过] npm %%v（建议安装 pnpm 以获得更快的依赖安装）
        )
    ) else (
        echo  [错误] 未找到 pnpm 或 npm
        echo  [提示] 请确认 Node.js 已正确安装
        goto :fail
    )
)

REM ──────────────────────────────────────────
REM  3. 安装依赖
REM ──────────────────────────────────────────
echo  [3/4] 检查前端依赖...

cd /d "%FRONTEND_DIR%"
if %errorlevel% neq 0 (
    echo  [错误] 前端目录不存在：%FRONTEND_DIR%
    goto :fail
)

if not exist "node_modules" (
    echo  [安装] 首次运行，正在安装依赖...
    echo  [提示] 这可能需要几分钟，请耐心等待...
    echo.
    call !PKG_MANAGER! install
    if %errorlevel% neq 0 (
        echo.
        echo  [错误] 依赖安装失败，请检查网络连接
        echo  [提示] 如果使用 npm，可尝试：npm config set registry https://registry.npmmirror.com
        goto :fail
    )
    echo.
    echo  [完成] 依赖安装成功
) else (
    echo  [通过] node_modules 已存在
)

REM ──────────────────────────────────────────
REM  4. 启动开发服务器
REM ──────────────────────────────────────────
echo  [4/4] 启动前端开发服务器...
echo.
echo  ──────────────────────────────────────────
echo  前端地址：http://localhost:5173
echo  后端代理：http://localhost:8080
echo  按 Ctrl+C 可停止服务
echo  ──────────────────────────────────────────
echo.

call !PKG_MANAGER! run dev
if %errorlevel% neq 0 (
    echo.
    echo  [错误] 前端启动失败
    goto :fail
)

goto :end

:fail
echo.
echo  [启动失败] 请排查以上问题后重试
pause
exit /b 1

:end
pause
exit /b 0
