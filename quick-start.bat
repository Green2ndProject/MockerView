@echo off
title MockerView Local Development

echo ============================================
echo   MockerView Quick Start
echo ============================================
echo.

:: Docker 실행 확인
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

:: 백그라운드 실행 옵션
echo Start mode:
echo   1. Background mode (recommended)
echo   2. Show all logs (debug)
echo.
set /p mode="Select (1 or 2): "

if "%mode%"=="2" (
    echo [INFO] Starting with full logs...
    docker-compose -f docker-compose-local.yml up
) else (
    echo [INFO] Starting in background...
    docker-compose -f docker-compose-local.yml up -d
    timeout /t 10 /nobreak > nul
    
    echo.
    echo ============================================
    echo Application: http://localhost:8080
    echo DB Admin: http://localhost:8081
    echo ============================================
    echo.
    
    call gradlew.bat bootRun --args="--spring.profiles.active=dev"
)
