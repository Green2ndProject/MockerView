#!/bin/bash

echo "========================================"
echo "MockerView Dev DB 완전 리셋"
echo "========================================"
echo ""
echo "⚠️ 경고: 모든 데이터가 삭제됩니다!"
echo ""
read -p "계속하시겠습니까? (Y/N): " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "취소되었습니다."
    exit 0
fi

echo ""
echo "[1/3] 컨테이너 중지 및 삭제..."
docker-compose down -v
if [ $? -ne 0 ]; then
    echo "❌ 중지 실패"
    exit 1
fi

echo ""
echo "[2/3] 볼륨 삭제..."
docker volume rm mockerview_pro_postgres_data mockerview_pro_redis_data 2>/dev/null
echo "✅ 볼륨 삭제 완료"

echo ""
echo "[3/3] 컨테이너 재시작..."
docker-compose up -d
if [ $? -ne 0 ]; then
    echo "❌ 재시작 실패"
    exit 1
fi

echo ""
echo "✅ DB 리셋 완료. 20초 후 애플리케이션을 시작하세요."
sleep 20
