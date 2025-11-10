#!/bin/bash

echo "========================================"
echo "MockerView Dev DB 초기화"
echo "========================================"
echo ""

echo "[1/2] PostgreSQL 컨테이너 확인..."
if ! docker ps --filter "name=mockerview_pro_postgres" --format "{{.Names}}" | grep -q mockerview_pro_postgres; then
    echo "❌ PostgreSQL 컨테이너가 실행되지 않았습니다."
    echo "run-local-dev.sh를 먼저 실행하세요."
    exit 1
fi

echo ""
echo "[2/2] 초기 스키마 적용..."
docker exec -i mockerview_pro_postgres psql -U mockerview_pro_user -d mockerview_pro < init-pro.sql 2>/dev/null
if [ $? -ne 0 ]; then
    echo "⚠️ 일부 스키마는 이미 존재할 수 있습니다."
else
    echo "✅ DB 초기화 완료"
fi

echo ""
