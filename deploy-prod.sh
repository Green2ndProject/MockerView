#!/bin/bash
# 서버 배포용 (실제 배포시에만 사용)

echo "⚠️  WARNING: Production Deployment"
read -p "Are you sure? (y/n): " confirm

if [ "$confirm" != "y" ]; then
    echo "Cancelled"
    exit 1
fi

if [ -f .env.production ]; then
    export $(cat .env.production | grep -v '^#' | xargs)
fi
git push render main
