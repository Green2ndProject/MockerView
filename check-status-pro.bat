@echo off
echo ========================================
echo MockerView PRO - Status Check
echo ========================================
echo.

echo [Container Status]
docker ps -a | findstr "mockerview_pro"

echo.
echo [PostgreSQL Connection Test]
docker exec mockerview_pro_postgres pg_isready -U mockerview_pro_user -d mockerview_pro

echo.
echo [Redis Connection Test]
docker exec mockerview_pro_redis redis-cli ping

echo.
echo [Volume Info]
docker volume ls | findstr "mockerview_pro"

echo.
echo [Network Info]
docker network ls | findstr "mockerview_pro"

echo.
echo ========================================
echo Connection Information
echo ========================================
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo Application: localhost:8082
echo ========================================
pause