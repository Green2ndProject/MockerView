@echo off
chcp 65001 >nul
echo ============================================
echo   MockerView 로컬 환경 상태
echo ============================================

echo.
echo [PostgreSQL 상태]
docker exec mockerview-postgres-local pg_isready -U postgres 2>nul
if %errorlevel% == 0 (
    echo   상태: 실행 중
    echo   포트: 5433 (로컬)
) else (
    echo   상태: 실행 중이 아님!
)

echo.
echo [Redis 상태]
docker exec mockerview-redis-local redis-cli ping 2>nul
if %errorlevel% == 0 (
    echo   상태: 실행 중
    echo   포트: 6380 (로컬)
) else (
    echo   상태: 실행 중이 아님!
)

echo.
echo [Docker 컨테이너]
docker ps --filter "name=mockerview" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo ============================================
echo   참고: 로컬 환경만 표시됩니다
echo   프로덕션 DB는 Render에 있습니다 (별도)
echo ============================================
echo.
pause