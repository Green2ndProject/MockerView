@echo off
chcp 65001 >nul
echo ============================================
echo   MockerView 로컬 환경 종료
echo ============================================

echo.
echo [1단계] Docker 컨테이너 중지 중...
docker-compose -f docker-compose-local.yml down

echo.
echo [2단계] 로컬 환경이 성공적으로 종료되었습니다!
echo          로컬 데이터는 보존됩니다.
echo.
pause