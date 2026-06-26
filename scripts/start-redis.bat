@echo off
set REDIS_HOME=F:\Redis-x64-3.2.100
cd /d %REDIS_HOME%

echo Starting Redis on 127.0.0.1:6379 ...
start "Redis Server" redis-server.exe redis.windows.conf

timeout /t 2 /nobreak >nul
redis-cli.exe ping
if %errorlevel%==0 (
    echo Redis started successfully.
) else (
    echo Redis may still be starting, check the Redis Server window.
)
