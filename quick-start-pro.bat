@echo off
chcp 65001 > nul
echo ========================================
echo MockerView PRO - 빠른 시작 🚀
echo ========================================
echo.

echo 1단계: PRO DB 초기화
call init-db.bat

echo.
echo 2단계: 준비 대기 (10초)
timeout /t 10 /nobreak

echo.
echo 3단계: PRO 애플리케이션 실행
call run-local.bat

pause
