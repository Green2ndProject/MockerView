#!/bin/bash

echo "========================================"
echo "MockerView Dev 환경 상태 확인"
echo "========================================"
echo ""

echo "[Docker 컨테이너 상태]"
docker ps -a --filter "name=mockerview_pro" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "[PostgreSQL 연결 테스트]"
if docker exec mockerview_pro_postgres pg_isready -U mockerview_pro_user -d mockerview_pro &> /dev/null; then
    echo "✅ PostgreSQL 정상 작동"
else
    echo "❌ PostgreSQL 연결 실패"
fi

echo ""
echo "[Redis 연결 테스트]"
if docker exec mockerview_pro_redis redis-cli ping &> /dev/null; then
    echo "✅ Redis 정상 작동"
else
    echo "❌ Redis 연결 실패"
fi

echo ""
