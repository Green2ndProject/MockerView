@echo off
echo ============================================
echo   Initialize MockerView Local Database
echo ============================================

echo.
echo [1] Stopping existing containers...
docker-compose -f docker-compose-local.yml down -v

echo.
echo [2] Starting fresh containers...
docker-compose -f docker-compose-local.yml up -d

echo.
echo [3] Waiting for database...
timeout /t 10 /nobreak > nul

echo.
echo [4] Database initialized!
echo.
echo Connection info:
echo - Host: localhost
echo - Port: 5433
echo - Database: mockerview_local
echo - Username: postgres
echo - Password: local1234
echo.
pause
