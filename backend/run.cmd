@echo off
chcp 65001 >nul
cd /d "%~dp0"
where mvn >nul 2>&1
if %errorlevel% equ 0 (
  mvn spring-boot:run
) else (
  echo 未检测到 Maven，请用 PowerShell 运行: .\run.ps1
  echo 或在 IDE 中直接运行 StudyHelperApplication.java
  pause
)
