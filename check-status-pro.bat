@echo off
chcp 65001 > nul
echo ========================================
echo MockerView PRO - 상태 확인
echo ========================================
echo.

echo [PRO 컨테이너 상태]
docker ps -a | findstr "mockerview_pro"

echo.
echo [PRO PostgreSQL 연결 테스트]
docker exec mockerview_pro_postgres pg_isready -U mockerview_pro_user -d mockerview_pro

echo.
echo [PRO Redis 연결 테스트]
docker exec mockerview_pro_redis redis-cli ping

echo.
echo [PRO 볼륨 정보]
docker volume ls | findstr "mockerview_pro"

echo.
echo [PRO 네트워크 정보]
docker network ls | findstr "mockerview_pro"

echo.
echo ========================================
echo 📍 접속 정보
echo ========================================
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo Application: localhost:8081
echo ========================================
pause
