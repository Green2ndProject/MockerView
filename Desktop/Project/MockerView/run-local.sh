#!/bin/bash

# 로컬 개발용 - 환경변수 없이 실행
unset SPRING_DATASOURCE_URL
unset SPRING_DATASOURCE_USERNAME
unset SPRING_DATASOURCE_PASSWORD
unset DATABASE_URL

export CLOUDINARY_CLOUD_NAME=dgtxigdit
export CLOUDINARY_API_KEY=838357254369448
export CLOUDINARY_API_SECRET=c5xRZ7AD5wTd6l0KBqTnaBYpWSU

export OPENAI_API_KEY=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

export AGORA_APP_ID=1f1255ab0cc84d84892039382f886cf6
export JWT_SECRET=0y0KPgwYtmYq1AoSfRRJav2/ImhCV6ueHW0Le+MEeCQ=

echo "🔧 로컬 모드: PostgreSQL localhost:5433"
./gradlew bootRun
