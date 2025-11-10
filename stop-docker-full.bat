@echo off
chcp 65001 > nul
echo ========================================
echo MockerView 전체 스택 중지 (Docker)
echo ========================================
echo.

echo Docker 컨테이너 중지 중...
docker-compose -f docker-compose-full.yml down
if errorlevel 1 (
    echo ❌ 중지 실패
    pause
    exit /b 1
)

echo.
echo ✅ 모든 컨테이너가 중지되었습니다.
pause
