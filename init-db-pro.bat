@echo off
chcp 65001 > nul
echo ========================================
echo MockerView PRO - PostgreSQL 초기화
echo ========================================
echo.

echo 기존 PRO 컨테이너 중지 및 제거...
docker-compose down -v

echo.
echo PRO PostgreSQL 및 Redis 시작...
docker-compose up -d mockerview_pro_postgres mockerview_pro_redis

echo.
echo 컨테이너 상태 확인 중...
timeout /t 5 /nobreak > nul

docker ps | findstr "mockerview_pro"

echo.
echo ========================================
echo PRO DB 초기화 완료! 🎉
echo ========================================
echo PostgreSQL: localhost:5433
echo Redis: localhost:6380
echo DB명: mockerview_pro
echo 사용자: mockerview_pro_user
echo ========================================
pause
