@echo off
chcp 65001 > nul
echo ========================================
echo MockerView PRO - 중지
echo ========================================
echo.

echo PRO 컨테이너 중지...
docker-compose down

echo.
echo 컨테이너 상태 확인...
docker ps -a | findstr "mockerview_pro"

echo.
echo ========================================
echo ✅ PRO 중지 완료!
echo ========================================
pause
