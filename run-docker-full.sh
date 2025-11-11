#!/bin/bash

echo "========================================"
echo "MockerView 전체 스택 실행 (Docker)"
echo "========================================"
echo ""

echo "[1/3] Docker 컨테이너 상태 확인..."
if ! docker ps &> /dev/null; then
    echo "❌ Docker가 실행되지 않았습니다."
    echo "Docker를 실행한 후 다시 시도하세요."
    exit 1
fi

echo ""
echo "[2/3] Docker 이미지 빌드 및 컨테이너 시작..."
docker-compose -f docker-compose-full.yml up -d --build
if [ $? -ne 0 ]; then
    echo "❌ Docker 컨테이너 시작 실패"
    exit 1
fi
 
echo ""
echo "[3/3] 애플리케이션 시작 대기 중 (30초)..."
sleep 30

echo ""
echo "✅ 실행 완료!"
echo "접속: http://localhost:8082"
echo ""
