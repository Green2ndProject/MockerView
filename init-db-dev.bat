@echo off
chcp 65001 > nul
echo ========================================
echo MockerView Dev DB 초기화
echo ========================================
echo.

echo [1/2] PostgreSQL 컨테이너 확인...
docker ps --filter "name=mockerview_pro_postgres" --format "{{.Names}}" | findstr mockerview_pro_postgres >nul
if errorlevel 1 (
    echo ❌ PostgreSQL 컨테이너가 실행되지 않았습니다.
    echo run-local-dev.bat를 먼저 실행하세요.
    pause
    exit /b 1
)

echo.
echo [2/2] 초기 스키마 적용...
docker exec -i mockerview_pro_postgres psql -U mockerview_pro_user -d mockerview_pro < init-pro.sql 2>nul
if errorlevel 1 (
    echo ⚠️ 일부 스키마는 이미 존재할 수 있습니다.
) else (
    echo ✅ DB 초기화 완료
)

echo.
pause
