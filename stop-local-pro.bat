@echo off
echo ========================================
echo MockerView PRO - Stop
echo ========================================
echo.

echo Stopping containers...
docker-compose down

echo.
echo Checking status...
docker ps -a | findstr "mockerview_pro"

echo.
echo ========================================
echo Stopped successfully!
echo ========================================
pause