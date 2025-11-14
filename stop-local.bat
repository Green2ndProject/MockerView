@echo off
echo ============================================
echo   Stopping MockerView Local Environment
echo ============================================

echo.
echo [1] Stopping Docker containers...
docker-compose -f docker-compose-local.yml down

echo.
echo [2] Environment stopped successfully!
pause
