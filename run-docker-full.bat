@echo off
chcp 65001 > nul
echo ========================================
echo MockerView 전체 스택 실행 (Docker)
echo ========================================
echo.

echo [1/3] Docker 컨테이너 상태 확인...
docker ps -a --filter "name=mockerview" --format "table {{.Names}}\t{{.Status}}" 2>nul
if errorlevel 1 (
    echo ❌ Docker가 실행되지 않았습니다.
    echo Docker Desktop을 실행한 후 다시 시도하세요.
    pause
    exit /b 1
)

echo.
echo [2/3] Docker 이미지 빌드 및 컨테이너 시작...
docker-compose -f docker-compose-full.yml up -d --build
if errorlevel 1 (
    echo ❌ Docker 컨테이너 시작 실패
    pause
    exit /b 1
)

echo.
echo [3/3] 애플리케이션 시작 대기 중 (30초)...
timeout /t 30 /nobreak > nul

echo.
echo ✅ 실행 완료!
echo 접속: http://localhost:8082
echo.
pause
