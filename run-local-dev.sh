#!/bin/bash

echo "========================================"
echo "MockerView Dev 환경 실행"
echo "========================================"
echo ""

echo "[1/4] Docker 컨테이너 상태 확인..."
if ! docker ps &> /dev/null; then
    echo "❌ Docker가 실행되지 않았습니다."
    echo "Docker를 실행한 후 다시 시도하세요."
    exit 1
fi

echo ""
echo "[2/4] Docker 컨테이너 시작..."
docker-compose up -d
if [ $? -ne 0 ]; then
    echo "❌ Docker 컨테이너 시작 실패"
    exit 1
fi

echo ""
echo "[3/4] 데이터베이스 준비 대기 중 (20초)..."
sleep 20

echo ""
echo "[4/4] Spring Boot 애플리케이션 시작..."
./gradlew clean bootRun --args='--spring.profiles.active=dev'
