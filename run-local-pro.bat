@echo off
chcp 65001 > nul
echo ========================================
echo MockerView PRO - 로컬 실행
echo ========================================
echo.

echo PRO PostgreSQL 및 Redis 컨테이너 확인...
docker ps | findstr "mockerview_pro_postgres" > nul
if errorlevel 1 (
    echo ❌ PRO DB가 실행되지 않았습니다.
    echo 먼저 init-db.bat을 실행하세요.
    pause
    exit /b 1
)

echo.
echo Gradle 빌드 시작...
call gradlew clean build -x test

if errorlevel 1 (
    echo ❌ 빌드 실패!
    pause
    exit /b 1
)

echo.
echo ========================================
echo PRO 애플리케이션 시작 🚀
echo ========================================
echo 포트: 8081
echo 프로파일: dev
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo.

set SPRING_PROFILES_ACTIVE=dev
set SERVER_PORT=8081
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/mockerview_pro
set SPRING_DATASOURCE_USERNAME=mockerview_pro_user
set SPRING_DATASOURCE_PASSWORD=mockerview_pro_pass
set SPRING_DATA_REDIS_HOST=localhost
set SPRING_DATA_REDIS_PORT=6380

java -jar build/libs/*.jar

pause
