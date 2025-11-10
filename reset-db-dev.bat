@echo off
chcp 65001 > nul
echo ========================================
echo MockerView Dev DB 완전 리셋
echo ========================================
echo.
echo ⚠️ 경고: 모든 데이터가 삭제됩니다!
echo.
set /p confirm="계속하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 취소되었습니다.
    pause
    exit /b 0
)

echo.
echo [1/3] 컨테이너 중지 및 삭제...
docker-compose down -v
if errorlevel 1 (
    echo ❌ 중지 실패
    pause
    exit /b 1
)

echo.
echo [2/3] 볼륨 삭제...
docker volume rm mockerview_pro_postgres_data mockerview_pro_redis_data 2>nul
echo ✅ 볼륨 삭제 완료

echo.
echo [3/3] 컨테이너 재시작...
docker-compose up -d
if errorlevel 1 (
    echo ❌ 재시작 실패
    pause
    exit /b 1
)

echo.
echo ✅ DB 리셋 완료. 20초 후 애플리케이션을 시작하세요.
timeout /t 20 /nobreak > nul
pause
