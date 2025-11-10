@echo off
echo ========================================
echo MockerView PRO - Initialize Database
echo ========================================
echo.

echo Stopping existing containers...
docker-compose down -v

echo.
echo Starting PostgreSQL and Redis...
docker-compose up -d mockerview_pro_postgres mockerview_pro_redis

echo.
echo Waiting for containers...
timeout /t 5 /nobreak > nul

docker ps | findstr "mockerview_pro"

echo.
echo ========================================
echo Database initialized successfully!
echo ========================================
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo Database: mockerview_pro
echo User: mockerview_pro_user
echo ========================================
pause