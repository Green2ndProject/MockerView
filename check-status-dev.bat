@echo off
chcp 65001 > nul
echo ========================================
echo MockerView Dev 환경 상태 확인
echo ========================================
echo.

echo [Docker 컨테이너 상태]
docker ps -a --filter "name=mockerview_pro" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo [PostgreSQL 연결 테스트]
docker exec mockerview_pro_postgres pg_isready -U mockerview_pro_user -d mockerview_pro 2>nul
if errorlevel 1 (
    echo ❌ PostgreSQL 연결 실패
) else (
    echo ✅ PostgreSQL 정상 작동
)

echo.
echo [Redis 연결 테스트]
docker exec mockerview_pro_redis redis-cli ping 2>nul
if errorlevel 1 (
    echo ❌ Redis 연결 실패
) else (
    echo ✅ Redis 정상 작동
)

echo.
pause
