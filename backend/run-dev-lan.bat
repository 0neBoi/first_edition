@echo off
cd /d "%~dp0"
echo [study-helper] 后端局域网模式 profile=lan ^(监听 0.0.0.0:8080^)
mvn spring-boot:run -Dspring-boot.run.profiles=lan
