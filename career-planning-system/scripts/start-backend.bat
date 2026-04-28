@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul 2>&1

REM ============================================
REM 大学生职业规划智能系统 - 后端启动脚本
REM 要求：JDK 21+、Maven 3.6+、MySQL、Redis
REM ============================================

title 职业规划系统 - 后端服务

echo.
echo  ══════════════════════════════════════════
echo    大学生职业规划智能系统 - 后端启动
echo  ══════════════════════════════════════════
echo.

REM ─── 定位项目根目录 ───
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "BACKEND_DIR=%SCRIPT_DIR%..\backend"

REM ──────────────────────────────────────────
REM  1. 检查 Java 环境（要求 JDK 21+）
REM ──────────────────────────────────────────
echo  [1/5] 检查 Java 环境...

where java > nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 Java，请安装 JDK 21 或更高版本
    echo  [提示] 下载地址：https://adoptium.net/
    goto :fail
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%~v"
)

for /f "tokens=1,2 delims=." %%a in ("!JAVA_VER!") do (
    set "JAVA_MAJOR=%%a"
)

REM 兼容 JDK 1.x 旧版本号格式（如 1.8.0）
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2,3 delims=." %%a in ("!JAVA_VER!") do (
        set "JAVA_MAJOR=%%a"
    )
)

if !JAVA_MAJOR! lss 21 (
    echo  [错误] Java 版本过低：!JAVA_VER!，项目要求 JDK 21+
    echo  [提示] 当前 JAVA_HOME：!JAVA_HOME!
    echo  [提示] 请前往 https://adoptium.net/ 下载 JDK 21
    goto :fail
)

echo  [通过] Java !JAVA_VER!

REM ──────────────────────────────────────────
REM  2. 检查 Maven 环境
REM ──────────────────────────────────────────
echo  [2/5] 检查 Maven 环境...

where mvn > nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 Maven，请安装 Maven 3.6+
    echo  [提示] 下载地址：https://maven.apache.org/download.cgi
    goto :fail
)

for /f "tokens=3" %%v in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
    echo  [通过] Maven %%v
)

REM ──────────────────────────────────────────
REM  3. 检查 .env 配置文件
REM ──────────────────────────────────────────
echo  [3/5] 检查环境配置...

if not exist "%PROJECT_ROOT%\.env" (
    echo  [警告] 未找到 .env 文件
    if exist "%PROJECT_ROOT%\.env.example" (
        copy "%PROJECT_ROOT%\.env.example" "%PROJECT_ROOT%\.env" > nul
        echo  [完成] 已从 .env.example 创建 .env 文件
        echo.
        echo  ╔══════════════════════════════════════════╗
        echo  ║  请先编辑 .env 文件，填入以下必填配置：   ║
        echo  ╠══════════════════════════════════════════╣
        echo  ║  1. DB_PASSWORD         数据库密码       ║
        echo  ║  2. DASHSCOPE_API_KEY   阿里云百炼 Key   ║
        echo  ║  3. OSS_ACCESS_KEY_ID   OSS AccessKey   ║
        echo  ║  4. OSS_ACCESS_KEY_SECRET OSS Secret    ║
        echo  ╠══════════════════════════════════════════╣
        echo  ║  文件位置：%PROJECT_ROOT%\.env           ║
        echo  ╚══════════════════════════════════════════╝
        echo.
        echo  编辑完成后，请重新运行此脚本
        goto :fail
    ) else (
        echo  [错误] .env 和 .env.example 均不存在，项目可能损坏
        goto :fail
    )
)

REM 检查 .env 中的关键配置项
set "HAS_EMPTY_DB_PASS=0"
for /f "usebackq tokens=1,* delims==" %%a in ("%PROJECT_ROOT%\.env") do (
    set "KEY=%%a"
    set "VAL=%%b"
    if "!KEY!"=="DB_PASSWORD" if "!VAL!"=="" set "HAS_EMPTY_DB_PASS=1"
)

echo  [通过] .env 配置文件已就绪

REM ──────────────────────────────────────────
REM  4. 检查基础服务（MySQL、Redis）
REM ──────────────────────────────────────────
echo  [4/5] 检查基础服务...

REM 检测 MySQL（默认 3306）
powershell -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('localhost',3306); $c.Close(); exit 0 } catch { exit 1 }" > nul 2>&1
if %errorlevel% neq 0 (
    echo  [警告] MySQL 服务（端口 3306）未检测到，请确认 MySQL 已启动
    echo  [提示] 如使用其他端口，请修改 application.yml 中的数据库连接地址
)

REM 检测 Redis（默认 6379）
powershell -Command "try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('localhost',6379); $c.Close(); exit 0 } catch { exit 1 }" > nul 2>&1
if %errorlevel% neq 0 (
    echo  [警告] Redis 服务（端口 6379）未检测到，JWT 黑名单功能可能不可用
    echo  [提示] 请安装并启动 Redis：https://github.com/tporadowski/redis/releases
)

echo  [完成] 基础服务检查完毕

REM ──────────────────────────────────────────
REM  5. 启动 Spring Boot 后端
REM ──────────────────────────────────────────
echo  [5/5] 启动后端服务...
echo.
echo  ──────────────────────────────────────────
echo  后端地址：http://localhost:8080
echo  按 Ctrl+C 可停止服务
echo  ──────────────────────────────────────────
echo.

cd /d "%BACKEND_DIR%"
if %errorlevel% neq 0 (
    echo  [错误] 无法进入后端目录：%BACKEND_DIR%
    goto :fail
)

call mvn spring-boot:run -q
if %errorlevel% neq 0 (
    echo.
    echo  [错误] 后端启动失败，请检查上方错误日志
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
