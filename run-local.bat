@echo off
title MockerView Local Development

echo ============================================
echo   MockerView Local Development
echo   (Using LOCAL Database - Safe!)
echo ============================================
echo.

if not exist .env (
    echo [ERROR] .env file not found!
    echo Please create .env file for local development
    pause
    exit /b 1
)

docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

echo [INFO] Loading LOCAL environment variables...
for /f "delims=" %%x in (.env) do (
    echo %%x | findstr /v "^#" >nul && set "%%x"
)

echo [INFO] Active Profile: %SPRING_PROFILES_ACTIVE%
if not "%SPRING_PROFILES_ACTIVE%"=="dev" (
    echo [WARNING] Not using dev profile! 
    echo Make sure you're not connecting to production DB!
    pause
)

echo [INFO] Starting local containers...
docker-compose -f docker-compose-local.yml up -d
timeout /t 10 /nobreak > nul

echo.
echo ============================================
echo Local Database Info:
echo   Host: localhost
echo   Port: 5433
echo   Database: mockerview_local
echo   Username: postgres
echo ============================================
echo.

call gradlew.bat bootRun