@echo off
set MYSQL=F:\mysql\bin\mysql.exe
set SQL=%~dp0..\platform-bootstrap\src\main\resources\db\init.sql

echo Initializing volunteer database via mysql client...
echo If this fails, run DbInit in IDEA instead.
echo.

"%MYSQL%" --protocol=TCP -h127.0.0.1 -P3306 -uroot -proot --ssl-mode=DISABLED < "%SQL%"
if %errorlevel%==0 goto :verify

"%MYSQL%" --protocol=TCP -h127.0.0.1 -P3306 -uroot --ssl-mode=DISABLED < "%SQL%"
if %errorlevel%==0 goto :verify

"%MYSQL%" --protocol=TCP -h127.0.0.1 -P3306 -uroot -p123456 --ssl-mode=DISABLED < "%SQL%"
if %errorlevel%==0 goto :verify

echo.
echo mysql client failed. Please run DbInit in IDEA:
echo   Run Configuration -^> DbInit -^> Run
exit /b 1

:verify
echo.
echo Done. Verifying...
"%MYSQL%" --protocol=TCP -h127.0.0.1 -P3306 -uroot -proot --ssl-mode=DISABLED -e "USE volunteer; SELECT COUNT(*) AS rows FROM appointment_order;"
