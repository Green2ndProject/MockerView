#!/bin/bash

echo "========================================"
echo "MockerView 전체 스택 중지 (Docker)"
echo "========================================"
echo ""

echo "Docker 컨테이너 중지 중..."
docker-compose -f docker-compose-full.yml down
if [ $? -ne 0 ]; then
    echo "❌ 중지 실패"
    exit 1
fi

echo ""
echo "✅ 모든 컨테이너가 중지되었습니다."
