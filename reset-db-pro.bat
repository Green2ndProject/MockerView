@echo off
echo ========================================
echo MockerView PRO - Reset Database
echo ========================================
echo.

echo Stopping and removing containers...
docker-compose down -v

echo.
echo Starting containers...
docker-compose up -d

echo.
echo Waiting for containers...
timeout /t 5 /nobreak >nul
docker ps --filter "name=mockerview_pro"

echo.
echo ========================================
echo Database reset complete!
echo ========================================
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo Database: mockerview_pro
echo User: mockerview_pro_user
echo ========================================

pause