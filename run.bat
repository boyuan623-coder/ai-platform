@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%PATH%

if "%DEEPSEEK_API_KEY%"=="" (
    echo [WARN] 请先设置环境变量 DEEPSEEK_API_KEY
    echo   set DEEPSEEK_API_KEY=sk-xxx
)

java -jar "%~dp0platform-bootstrap\target\platform-bootstrap-1.0.0-SNAPSHOT.jar"
