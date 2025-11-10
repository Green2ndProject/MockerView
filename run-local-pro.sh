#!/bin/bash

echo "========================================"
echo "MockerView PRO - 로컬 실행"
echo "========================================"
echo

echo "PRO PostgreSQL 및 Redis 컨테이너 확인..."
if ! docker ps | grep -q "mockerview_pro_postgres"; then
    echo "❌ PRO DB가 실행되지 않았습니다."
    echo "먼저 ./init-db-pro.sh를 실행하세요."
    exit 1
fi

echo
echo "Gradle 빌드 시작..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "❌ 빌드 실패!"
    exit 1
fi

echo
echo "========================================"
echo "PRO 애플리케이션 시작 🚀"
echo "========================================"
echo "포트: 8082"
echo "프로파일: dev"
echo "PostgreSQL: localhost:5433"
echo "Redis: localhost:6380"
echo

export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8082
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/mockerview_pro
export SPRING_DATASOURCE_USERNAME=mockerview_pro_user
export SPRING_DATASOURCE_PASSWORD=mockerview_pro_pass
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6380

java -jar build/libs/*.jar
