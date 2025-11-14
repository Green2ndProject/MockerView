#!/bin/bash

echo "🛑 MockerView 로컬 환경 종료"

docker-compose -f docker-compose-local.yml down

echo "✅ 종료 완료"
