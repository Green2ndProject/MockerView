@echo off
chcp 65001 >nul
echo ============================================
echo   MockerView 로컬 데이터베이스 초기화
echo   (프로덕션 DB에는 영향 없음)
echo ============================================

echo.
set /p confirm="확실합니까? 모든 로컬 데이터가 삭제됩니다! (y/n): "
if not "%confirm%"=="y" (
    echo 취소되었습니다
    pause
    exit /b 0
)

echo.
echo [1단계] 기존 컨테이너 중지 중...
docker-compose -f docker-compose-local.yml down -v

echo.
echo [2단계] 새 컨테이너 시작 중...
docker-compose -f docker-compose-local.yml up -d

echo.
echo [3단계] 데이터베이스 준비 중...
timeout /t 10 /nobreak > nul

echo.
echo ============================================
echo   로컬 데이터베이스 초기화 완료!
echo ============================================
echo   연결 정보:
echo     호스트: localhost
echo     포트: 5433
echo     데이터베이스: mockerview_local
echo     사용자명: postgres
echo     비밀번호: local1234
echo.
echo   참고: Flyway가 다음 실행 시
echo   init.sql에서 테이블을 자동 생성합니다
echo ============================================
echo.
pause