@echo off
REM 使用环境变量 MYSQL_PASSWORD 或通过命令行传入密码
REM 用法: init-mysql.bat [password]
set MYSQL_PWD=%1
if "%MYSQL_PWD%"=="" set MYSQL_PWD=%MYSQL_PASSWORD%
set SQL=%~dp0..\platform-bootstrap\src\main\resources\db\init.sql

echo Initializing database via mysql client...
echo If this fails, run DbInit in IDEA instead.
echo.

mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot --ssl-mode=DISABLED < "%SQL%" 2>nul
if %errorlevel%==0 goto :verify

if not "%MYSQL_PWD%"=="" (
    mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot -p%MYSQL_PWD% --ssl-mode=DISABLED < "%SQL%"
    if %errorlevel%==0 goto :verify
)

echo.
echo mysql client failed. Please run DbInit in IDEA:
echo   Run Configuration -^> DbInit -^> Run
echo Or set MYSQL_PASSWORD environment variable.
exit /b 1

:verify
echo.
echo Done. Verifying...
mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot --ssl-mode=DISABLED -e "USE ai_platform; SELECT COUNT(*) AS rows FROM appointment_order;"
