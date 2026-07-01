@echo off
REM 从环境变量 REDIS_HOME 读取 Redis 安装路径，或使用 PATH 中的 redis-server
if defined REDIS_HOME (
    cd /d "%REDIS_HOME%"
)

echo Starting Redis on 127.0.0.1:6379 ...
start "Redis Server" redis-server.exe 2>nul
if %errorlevel% neq 0 (
    echo redis-server.exe not found. Please set REDIS_HOME or add Redis to your PATH.
    exit /b 1
)

timeout /t 2 /nobreak >nul
redis-cli.exe ping 2>nul
if %errorlevel%==0 (
    echo Redis started successfully.
) else (
    echo Redis may still be starting, check the Redis Server window.
)
