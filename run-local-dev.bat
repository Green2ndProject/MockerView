@echo off
chcp 65001 > nul
echo ========================================
echo MockerView Dev 환경 실행
echo ========================================
echo.

echo [1/4] Docker 컨테이너 상태 확인...
docker ps -a --filter "name=mockerview_pro" --format "table {{.Names}}\t{{.Status}}" 2>nul
if errorlevel 1 (
    echo ❌ Docker가 실행되지 않았습니다.
    echo Docker Desktop을 실행한 후 다시 시도하세요.
    pause
    exit /b 1
)

echo.
echo [2/4] Docker 컨테이너 시작...
docker-compose up -d
if errorlevel 1 (
    echo ❌ Docker 컨테이너 시작 실패
    pause
    exit /b 1
)

echo.
echo [3/4] 데이터베이스 준비 대기 중 (20초)...
timeout /t 20 /nobreak > nul

echo.
echo [4/4] Spring Boot 애플리케이션 시작...
call gradlew.bat clean bootRun --args='--spring.profiles.active=dev'

pause
