@echo off
echo ========================================
echo MockerView PRO - Local Run
echo ========================================
echo.

echo Checking PostgreSQL container...
docker ps | findstr mockerview_pro_postgres >nul 2>&1
if errorlevel 1 (
    echo ERROR: PostgreSQL container not running!
    echo Please run init-db-pro.bat first.
    pause
    exit /b 1
)

echo Checking Redis container...
docker ps | findstr mockerview_pro_redis >nul 2>&1
if errorlevel 1 (
    echo ERROR: Redis container not running!
    echo Please run init-db-pro.bat first.
    pause
    exit /b 1
)

echo.
echo Starting Gradle build...
call gradlew.bat clean build -x test
if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Starting Application
echo ========================================
echo Port: 8082
echo Profile: dev
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo.

java -jar -Dspring.profiles.active=dev build\libs\mockerview-0.0.1-SNAPSHOT.jar

pause