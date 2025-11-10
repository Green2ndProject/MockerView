#!/bin/bash

echo "========================================"
echo "MockerView PRO 완전 전환 🚀"
echo "========================================"
echo
echo "🚨 주의: 기존 B2C 파일이 삭제되고 PRO로 교체됩니다!"
echo
echo "📦 작업 내용:"
echo "  1. 기존 파일 삭제"
echo "  2. Pro 파일 복사 및 이름 변경"
echo "  3. application.yml 백업 및 교체"
echo
read -p "계속하시겠습니까? (y/N): " confirm

if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "취소되었습니다."
    exit 0
fi

echo
echo "========================================"
echo "1단계: 기존 파일 삭제 중..."
echo "========================================"

rm -f init.sql
rm -f docker-compose.yml
rm -f docker-compose-local.yml
rm -f init-db.bat
rm -f run-local.bat
rm -f stop-local.bat
rm -f quick-start.bat
rm -f check-status.bat
rm -f init-db.sh
rm -f stop-local.sh

echo "✅ 기존 파일 삭제 완료!"

echo
echo "========================================"
echo "2단계: Pro 파일 복사 중..."
echo "========================================"

cp -f init-pro.sql init.sql
cp -f docker-compose-pro.yml docker-compose.yml
cp -f init-db-pro.bat init-db.bat 2>/dev/null || true
cp -f run-local-pro.bat run-local.bat 2>/dev/null || true
cp -f stop-local-pro.bat stop-local.bat 2>/dev/null || true
cp -f quick-start-pro.bat quick-start.bat 2>/dev/null || true
cp -f check-status-pro.bat check-status.bat 2>/dev/null || true
cp -f init-db-pro.sh init-db.sh
cp -f stop-local-pro.sh stop-local.sh

chmod +x init-db.sh
chmod +x stop-local.sh

echo "✅ Pro 파일 복사 완료!"

echo
echo "========================================"
echo "3단계: application.yml 처리 중..."
echo "========================================"

if [ -f "src/main/resources/application.yml" ]; then
    cp -f src/main/resources/application.yml src/main/resources/application.yml.backup
    echo "✅ 기존 application.yml 백업 완료"
fi

cp -f application-pro.yml src/main/resources/application-pro.yml 2>/dev/null || true
echo "✅ application-pro.yml 복사 완료"

echo
echo "========================================"
echo "✅ PRO 전환 완료!"
echo "========================================"
echo
echo "📍 포트 정보:"
echo "  - PostgreSQL: localhost:5433"
echo "  - Redis: localhost:6380"
echo "  - Application: localhost:8082"
echo
echo "📍 DB 정보:"
echo "  - DB명: mockerview_pro"
echo "  - 사용자: mockerview_pro_user"
echo "  - 비밀번호: mockerview_pro_pass"
echo
echo "🚀 다음 단계:"
echo "  1. ./init-db.sh 실행"
echo "  2. src/main/resources/application-pro.yml 확인"
echo "  3. ./gradlew clean build && java -jar build/libs/*.jar"
echo
