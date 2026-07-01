@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%PATH%

java -jar "%~dp0platform-bootstrap\target\platform-bootstrap-1.0.0-SNAPSHOT.jar"
