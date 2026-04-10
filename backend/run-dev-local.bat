@echo off
cd /d "%~dp0"
echo [study-helper] 后端本机模式 ^(仅 127.0.0.1:8080^)
mvn spring-boot:run
