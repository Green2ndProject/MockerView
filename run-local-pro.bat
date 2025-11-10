@echo off
echo ========================================
echo MockerView PRO - 로컬 실행
echo ========================================
echo.

echo PRO PostgreSQL 및 Redis 컨테이너 확인...
docker ps | findstr mockerview_pro_postgres >nul 2>&1
if errorlevel 1 (
    echo PostgreSQL 컨테이너가 실행되지 않았습니다.
    echo init-db-pro.bat를 먼저 실행하세요.
    pause
    exit /b 1
)

docker ps | findstr mockerview_pro_redis >nul 2>&1
if errorlevel 1 (
    echo Redis 컨테이너가 실행되지 않았습니다.
    echo init-db-pro.bat를 먼저 실행하세요.
    pause
    exit /b 1
)

echo.
echo Gradle 빌드 시작...
call gradlew.bat clean build -x test
if errorlevel 1 (
    echo.
    echo 빌드 실패!
    pause
    exit /b 1
)

echo.
echo ========================================
echo PRO 애플리케이션 시작
echo ========================================
echo 포트: 8082
echo 프로파일: dev
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo.

java -jar -Dspring.profiles.active=dev build\libs\mockerview-0.0.1-SNAPSHOT.jar

pause