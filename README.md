# MockerView

AI 기반 실시간 협업 모의면접 플랫폼

프로덕션: https://mockerview.net

## 프로젝트 개요

MockerView는 기업과 교육기관을 위한 B2B 우선 AI 면접 플랫폼입니다.

### 핵심 가치 제안

**기업/교육기관 (B2B)**
- 연간 채용 교육비 90% 절감 - AI 비용 200만원 → 20만원 (QuestionPool Learning)
- 면접관 시간 80% 단축 - 실시간 AI 피드백으로 즉시 평가
- Admin Dashboard - 50명 동시 세션 관리, 부서별 통계
- API 연동 - 기존 LMS/HR 시스템 통합

**취준생/개인 (B2C)**
- 실시간 AI 피드백 - GPT-4o-mini 기반 STAR 방법론 분석
- Whisper 자막 - 음성 자동 텍스트 변환 (환각 필터링)
- 영상 녹화 - Agora WebRTC + Cloudinary 저장
- 다차원 분석 - 음성/표정/MBTI 종합 평가

### 차별화 포인트

```
QuestionPool Learning System
┌────────────────────────────────────────┐
│ 1. AI 질문 생성 (초기 100%)                │
│ 2. 고품질 질문 Pool 저장                   │
│ 3. Pool 우선 재사용 (3개월 후 90%)          │
│ 4. AI 호출 최소화 (비용 90% 절감)           │
└────────────────────────────────────────┘

초기 월 2만원 → 1개월 후 월 2천원
```

---

## 주요 기능

### 1. 실시간 협업 면접
- WebSocket (STOMP) - 실시간 채팅, 질문/답변 동기화
- 그룹 면접 - 면접관 1명 + 면접자 N명 (최대 50명)
- 셀프 모드 - 혼자 연습, AI 피드백만 수신

### 2. AI 고도화 기능
- GPT-4o-mini - 질문 생성, 피드백 생성, MBTI 분석
- Whisper API - 실시간 음성→텍스트 자막 (환각 필터링)
- 음성 분석 - WPM, 톤, 일시정지, 에너지 레벨
- 표정 분석 - 감정 점수, 시선 접촉 (준비 중)

### 3. 구독 시스템
- 플랜 - FREE / BASIC / PRO / ENTERPRISE
- 자동 갱신, 사용량 추적, 스케줄러 운영
- Toss Payments - 간편결제/가상계좌/휴대폰

### 4. 보안
- JWT (Access 1시간, Refresh 7일)
- OAuth2 (Google/Kakao)
- Rate Limiting (AOP)
- Soft Delete (30일) → Hard Delete

### 5. PWA
- 오프라인 지원
- Push Notification (VAPID)
- 설치 프롬프트 (iOS/Android)

---

## 기술 스택

### Backend
- Java 17
- Spring Boot 3.3.4
- JPA + PostgreSQL (프로덕션) / Oracle (로컬)
- WebSocket (STOMP) + SimpleBroker
- Spring Security + JWT (jjwt 0.12.3)
- Redis (캐싱, Refresh Token)

### Frontend
- Thymeleaf + JavaScript
- Bootstrap 5.3.2 + jQuery
- SockJS + STOMP Client
- Chart.js

### External APIs
- OpenAI (GPT-4o-mini, Whisper)
- Agora WebRTC
- Cloudinary (영상 저장)
- Toss Payments
- Web Push API

### DevOps
- Docker
- Render (프로덕션 배포)
- PostgreSQL + Redis (매니지드)
- Gradle

---

## 시스템 아키텍처

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  Frontend   │────▶│ Spring Boot  │────▶│ PostgreSQL  │
│ (Thymeleaf) │     │  (WebSocket) │     │   + Redis   │
└─────────────┘     └──────────────┘     └─────────────┘
                           │
                           ├─────▶ OpenAI API
                           ├─────▶ Agora RTC
                           ├─────▶ Cloudinary
                           └─────▶ Toss Payments
```

### 주요 플로우

**1. 실시간 면접**
```
사용자 로그인 (JWT)
→ 세션 생성 (구독 제한 확인)
→ WebSocket 연결 (STOMP)
→ Agora Token 발급
→ 영상 스트리밍 (WebRTC)
→ 음성 녹화 → Whisper 자막 (환각 필터링)
→ 답변 제출 → AI 피드백 비동기 생성
→ WebSocket 브로드캐스트
→ 영상 Cloudinary 업로드
→ 리포트 저장
```

**2. QuestionPool Learning**
```
질문 요청
→ QuestionPool 조회 (카테고리 + 난이도)
→ 없으면 AI 생성
→ 고품질 질문 Pool 저장
→ 다음 요청 시 Pool 우선 사용
→ AI 호출 90% 감소
```

---

## 데이터베이스 설계

### 핵심 엔티티

**User** - 사용자 (4 ROLE: USER, INTERVIEWER, ADMIN, ENTERPRISE)
**Session** - 면접 세션 (TEXT/AUDIO/VIDEO 모드)
**Question** - 질문 (카테고리, 난이도)
**Answer** - 답변 (video_url 인덱싱)
**Feedback** - 피드백 (AI / INTERVIEWER)
**Subscription** - 구독 (플랜별 제한)
**Payment** - 결제 (Toss)
**QuestionPool** - 질문 풀 (학습 시스템)
**RefreshToken** - 토큰 (user_id, token 복합 인덱스)

### 최적화

- 복합 인덱스 (status, expires_at)
- Partial Index (video_url IS NOT NULL)
- Soft Delete (is_deleted, deleted_at)
- Fetch Join (N+1 방지)
- Redis 캐싱 (Refresh Token, Rate Limit)

---

## 설치 및 실행

### 사전 요구사항

- Java 17+
- Docker (PostgreSQL, Redis 로컬 실행)
- Gradle 8.10+

### 로컬 실행

```bash
# 1. 레포 클론
git clone https://github.com/your-org/MockerView.git
cd MockerView

# 2. Docker로 DB 실행
docker-compose up -d

# 3. 환경변수 설정 (application-local.yml)
# DB, Redis, API 키 설정

# 4. 빌드 및 실행
./gradlew clean build
./gradlew bootRun

# 5. 접속
http://localhost:8080
```

### 환경변수

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

openai:
  api:
    key: ${OPENAI_API_KEY}

agora:
  app-id: ${AGORA_APP_ID}
  app-certificate: ${AGORA_APP_CERTIFICATE}

cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}

toss:
  payments:
    secret-key: ${TOSS_SECRET_KEY}
```

---

## 배포

### Render

```yaml
# render.yaml
services:
  - type: web
    name: mockerview
    env: docker
    plan: free
    buildCommand: ./gradlew clean build
    startCommand: java -jar build/libs/MockerView-0.0.1-SNAPSHOT.jar
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: DATABASE_URL
        fromDatabase:
          name: mockerview-db
          property: connectionString
      - key: REDIS_HOST
        fromService:
          name: mockerview-redis
          property: host
```

### Docker

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 팀 구성

**악귀멸살 (6인)**

| 이름 | 역할 | 담당 |
|------|------|------|
| 이다겸 | Tech Lead | OpenAI/Whisper, WebSocket, Agora, 전체 아키텍처 |
| 윤호준 | Backend | 구독/결제, STOMP, 스케줄러, 알림 |
| 이수정 | Database | PostgreSQL, 인덱싱, PWA, 성능 최적화 |
| 서원준 | Backend | 리뷰, 면접관 노트, 통계 |
| 김은채 | Backend | 알림, QuestionPool, 검색 |
| 최수인 | Frontend | 셀프 모드, 리포트, UI/UX |

---

## 주요 의사결정

### 1. B2B 우선 전략
- 이유: 기업 고객이 안정적 수익, B2C는 자연 유입
- 근거: HR Tech 시장 2조원, 연 15% 성장
- 결과: 부트캠프 3곳 MoU 추진 중

### 2. QuestionPool Learning
- 이유: AI 비용 90% 절감 (200만원 → 20만원)
- 방법: 고품질 질문 자동 Pool 저장 → 재사용
- 효과: 3개월 후 AI 호출 100% → 10%

### 3. SimpleBroker → RabbitMQ 마이그레이션 예정
- 현재: SimpleBroker로 빠른 프로토타이핑
- 한계: 단일 서버만 지원, 수평 확장 불가
- 계획: RabbitMQ로 전환 → 멀티 서버 메시지 브로드캐스트

### 4. PostgreSQL 선택
- 이유: 무료 호스팅 + 엔터프라이즈 기능 (JSONB, 풀텍스트 검색)
- 로컬: Oracle (학원 환경), 프로덕션: PostgreSQL
- JPA Dialect로 DB 독립성 유지

### 5. JWT + Refresh Token
- Access Token: 1시간 (짧게 → 보안)
- Refresh Token: 7일 (길게 → UX)
- DB + Redis 이중 저장 (폴백 + 성능)

---

## 성능 지표

### 부하 테스트 (JMeter)
- 동시 접속: 50명
- 평균 응답: 200ms
- AI 피드백: 3-5초 (비동기)
- WebSocket 지연: <100ms

### 비용 절감
- 초기 AI 비용: 월 200만원
- 3개월 후: 월 20만원 (90% 절감)
- QuestionPool 적중률: 90%

---

## 라이선스

MIT License

---

## 기여

현재 팀 프로젝트로 진행 중이며, 외부 기여는 받지 않습니다.

---

## 문의

- 프로덕션: https://mockerview.net
- 이메일: admin@mockerview.net
- 팀: 악귀멸살

---

## 향후 계획

**단기 (3개월)**
- 파일럿 고객 3곳 확보
- RabbitMQ 마이그레이션
- 표정 분석 완성

**중기 (6개월)**
- Elasticsearch 도입
- S3로 영상 저장소 이관
- API 라이선스 판매

**장기 (1년+)**
- MSA 전환
- Kubernetes 오케스트레이션
- 글로벌 진출 (영어/일본어)

---

2025 MockerView Team. All rights reserved.
