@echo off
chcp 65001 >nul
title MockerView 로컬 개발 환경

echo ============================================
echo   MockerView 로컬 개발 환경
echo   로컬 DB만 사용 - 안전합니다!
echo ============================================
echo.

if not exist .env (
    echo [오류] .env 파일을 찾을 수 없습니다!
    echo 로컬 개발용 .env 파일을 생성해주세요
    pause
    exit /b 1
)

docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [오류] Docker가 실행 중이 아닙니다!
    echo Docker Desktop을 먼저 실행해주세요
    pause
    exit /b 1
)

echo [정보] 로컬 환경변수 로딩 중...
for /f "delims=" %%x in (.env) do (
    echo %%x | findstr /v "^#" >nul && set "%%x"
)

echo [정보] 활성 프로필: %SPRING_PROFILES_ACTIVE%
if not "%SPRING_PROFILES_ACTIVE%"=="dev" (
    echo [경고] dev 프로필이 아닙니다! 
    echo 프로덕션 DB에 연결하지 않는지 확인하세요!
    pause
)

echo [정보] 로컬 컨테이너 시작 중...
docker-compose -f docker-compose-local.yml up -d
timeout /t 10 /nobreak > nul

echo.
echo ============================================
echo 로컬 데이터베이스 정보:
echo   호스트: localhost
echo   포트: 5433
echo   데이터베이스: mockerview_local
echo   사용자명: postgres
echo ============================================
echo.
echo [정보] Flyway가 init.sql을 자동으로 실행합니다
echo [정보] 첫 실행 시 테이블이 자동 생성됩니다
echo.

call gradlew.bat bootRun