@echo off
chcp 65001 >nul
title MockerView 로컬 개발 환경

echo ============================================
echo   MockerView 빠른 시작 (로컬만)
echo ============================================
echo.

docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [오류] Docker가 실행 중이 아닙니다!
    echo Docker Desktop을 먼저 실행해주세요
    pause
    exit /b 1
)

echo 시작 모드:
echo   1. 백그라운드 모드 (권장)
echo   2. 모든 로그 표시 (디버그)
echo.
set /p mode="선택 (1 또는 2): "

if "%mode%"=="2" (
    echo.
    echo [정보] 전체 로그 모드로 시작 중...
    echo [정보] 종료하려면 Ctrl+C를 누르세요
    echo.
    docker-compose -f docker-compose-local.yml up
) else (
    echo.
    echo [정보] 백그라운드로 시작 중...
    docker-compose -f docker-compose-local.yml up -d
    timeout /t 10 /nobreak > nul
    
    echo.
    echo ============================================
    echo   로컬 환경 준비 완료!
    echo ============================================
    echo   PostgreSQL: localhost:5433
    echo   Redis: localhost:6380
    echo.
    echo   Flyway가 init.sql에서 테이블을 자동 생성합니다
    echo ============================================
    echo.
    echo [정보] Spring Boot 시작 중...
    echo.
    
    call gradlew.bat bootRun
)