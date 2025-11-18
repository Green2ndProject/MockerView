#!/bin/bash

echo "🚀 MockerView 로컬 개발 환경 시작"

echo "📦 Docker 컨테이너 시작..."
docker-compose -f docker-compose-local.yml up -d

echo "⏳ 데이터베이스 준비 중..."
sleep 5

if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

echo "🌱 Spring Boot 애플리케이션 시작..."
./gradlew bootRun --args='--spring.profiles.active=dev'
