@echo off
echo ============================================
echo   MockerView Local Environment Status
echo ============================================

echo.
echo [PostgreSQL Status]
docker exec mockerview-postgres-local pg_isready -U postgres 2>nul
if %errorlevel% == 0 (
    echo PostgreSQL is running on port 5433
) else (
    echo PostgreSQL is NOT running!
)

echo.
echo [Redis Status]
docker exec mockerview-redis-local redis-cli ping 2>nul
if %errorlevel% == 0 (
    echo Redis is running on port 6379
) else (
    echo Redis is NOT running!
)

echo.
echo [Docker Containers]
docker ps --filter "name=mockerview"

echo.
pause
