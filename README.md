# MockerView 🎤

> AI 기반 실시간 협업 모의면접 플랫폼

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Oracle](https://img.shields.io/badge/Oracle-21c-red.svg)](https://www.oracle.com/database/)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-blue.svg)](https://stomp.github.io/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o--mini-412991.svg)](https://openai.com/)

---

## 📌 프로젝트 소개

**MockerView**는 취업준비생들이 실전처럼 면접을 연습할 수 있는 실시간 협업 플랫폼입니다.

### 🎯 핵심 기능

- **👥 그룹 면접**: 실시간 WebSocket 기반 다중 사용자 면접 세션
- **🤖 AI 피드백**: OpenAI GPT-4o-mini를 활용한 STAR 기법 기반 답변 평가
- **🎙️ 음성/영상 답변**: Whisper API 음성 인식 + Agora RTC 실시간 통화
- **📊 성장 트래킹**: 답변 히스토리 및 통계 분석
- **✨ 셀프 면접**: AI가 자동 생성한 질문으로 혼자 연습

### 💡 차별점

| 기존 서비스 | MockerView |
|------------|------------|
| Zoom/Google Meet | ❌ 기록 없음 | ✅ 모든 답변/피드백 DB 저장 |
| 오프라인 스터디 | ❌ 피드백 편차 큼 | ✅ AI 객관적 평가 |
| 면접 문제 은행 | ❌ 혼자 연습만 가능 | ✅ 실시간 협업 가능 |

### 🏆 프로젝트 정보

- **프로젝트 기간**: 2025.09.26 ~ 2025.10.15
- **팀 구성**: 6명
- **개발 환경**: VSCode, Oracle DB, Windows

---

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **ORM**: Spring Data JPA
- **Security**: Spring Security + JWT
- **Real-time**: WebSocket (STOMP + SockJS)
- **Build**: Gradle

### Frontend
- **Template Engine**: Thymeleaf
- **UI Framework**: Bootstrap 5
- **JavaScript**: Vanilla JS (ES6+)

### Database
- **Development**: Oracle 21c
- **Production**: PostgreSQL 15

### External APIs
- **AI**: OpenAI GPT-4o-mini (피드백 생성)
- **STT**: OpenAI Whisper API (음성→텍스트)
- **RTC**: Agora SDK (실시간 음성/영상 통화)

### DevOps
- **Media Processing**: FFmpeg (음성/영상 압축)
- **Version Control**: Git
- **IDE**: VSCode

---

## 🏗️ 아키텍처

### 시스템 구조
```
┌─────────────┐     WebSocket       ┌──────────────┐
│   Browser   │◄──────STOMP────────►│ Spring Boot  │
│  (Client)   │                     │   Server     │
└─────────────┘                     └──────────────┘
      │                                     │
      │ HTTPS                               │ JDBC
      ▼                                     ▼
┌─────────────┐                     ┌──────────────┐
│   Agora     │                     │   Oracle DB  │
│   RTC CDN   │                     │              │
└─────────────┘                     └──────────────┘
                                            │
                                            │ REST API
                                            ▼
                                    ┌──────────────┐
                                    │  OpenAI API  │
                                    │  (GPT/Whisper)│
                                    └──────────────┘
```

### 데이터베이스 ERD
```
┌──────────┐        ┌──────────┐        ┌──────────┐
│   User   │───1:N──│ Session  │───1:N──│ Question │
└──────────┘        └──────────┘        └──────────┘
     │                                        │
     │                                        │
     │ 1:N                                 1:N│
     │                                        │
     ▼                                        ▼
┌──────────┐                          ┌──────────┐
│ Review   │                          │  Answer  │
└──────────┘                          └──────────┘
                                            │
                                         1:N│
                                            ▼
                                      ┌──────────┐
                                      │ Feedback │
                                      └──────────┘
```

---

## 🚀 시작하기

### 📋 사전 요구사항

#### 필수 설치
- **Java 17** 이상
- **FFmpeg** (음성/영상 처리용)
- **Git**

#### 선택 설치 (권장)
- **Chocolatey** (Windows 패키지 관리자)
- **VSCode** (IDE)

### 💻 로컬 환경 설정 (Windows)

#### 1️⃣ Chocolatey 설치
```powershell
# PowerShell 관리자 권한으로 실행
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

#### 2️⃣ 필수 프로그램 설치
```powershell
# Java 17
choco install openjdk17 -y

# FFmpeg (필수!)
choco install ffmpeg -y

# Git
choco install git -y

# VSCode (선택)
choco install vscode -y

# 설치 확인
java -version
ffmpeg -version
git --version
```

#### 3️⃣ 프로젝트 클론
```bash
git clone https://github.com/Green2ndProject/MockerView.git
cd mockerview
```

#### 4️⃣ 환경 변수 설정

**프로젝트 루트에 `.env` 파일 생성**
```properties
# OpenAI API Key
OPENAI_API_KEY=your-openai-api-key-here

# Agora 설정
AGORA_APP_ID=your-agora-app-id
AGORA_APP_CERTIFICATE=your-agora-certificate

# 데이터베이스 (운영 환경)
DATABASE_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1
DB_USERNAME=mockerview
DB_PASSWORD=mockerview
```

**또는 Windows 시스템 환경 변수 등록 (PowerShell 관리자 권한)**
```powershell
[System.Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "your-key", [System.EnvironmentVariableTarget]::User)
[System.Environment]::SetEnvironmentVariable("AGORA_APP_ID", "your-app-id", [System.EnvironmentVariableTarget]::User)
[System.Environment]::SetEnvironmentVariable("AGORA_APP_CERTIFICATE", "your-cert", [System.EnvironmentVariableTarget]::User)

# VSCode 재시작 필요!
```

#### 5️⃣ 프로젝트 실행
```powershell
# Gradle 빌드 및 실행
.\gradlew bootRun

# 또는 JAR 파일로 실행
.\gradlew clean build
java -jar build\libs\mockerview-0.0.1-SNAPSHOT.jar
```

#### 6️⃣ 접속 확인
브라우저에서 접속:
```
http://localhost:8080
```

---

## 📖 주요 기능 사용법

### 1. 회원가입 & 로그인
```
1. http://localhost:8080/auth/register
2. 이름, 아이디, 비밀번호, 이메일, 역할 선택
   - STUDENT: 면접자 (답변 제출)
   - HOST: 면접관 (질문 출제, 세션 관리)
   - REVIEWER: 리뷰어 (피드백만 작성)
3. 로그인 → JWT 토큰 쿠키 저장 (3시간 유효)
```

### 2. 그룹 면접 진행 🎬

#### 호스트 (면접관)
```
1. 세션 생성
   - /session/list → "새 세션 만들기"
   - 제목, 타입(TEXT/AUDIO/VIDEO) 선택

2. 질문 출제
   - 세션 입장 → "질문 출제" 버튼
   - 질문 내용, 순서, 타이머(초) 입력
   → WebSocket으로 모든 참가자에게 실시간 전송

3. 답변 확인
   - 학생들이 제출한 답변 실시간 표시
   - AI 피드백 자동 생성 (2-5초 소요)

4. 면접관 피드백 작성
   - 각 답변 카드에서 점수(1-100) 입력
   - 코멘트 작성 → "평가 제출"

5. 세션 종료
   - "세션 종료" 버튼 → status=ENDED
   → 모든 참가자에게 종료 알림
```

#### 학생 (면접자)
```
1. 세션 참가
   - /session/list → 원하는 세션 클릭
   - 역할 선택: STUDENT

2. 질문 대기
   - 호스트가 질문 출제하면 화면에 표시
   - 타이머 카운트다운 시작

3. 답변 제출
   - 텍스트: 입력창에 작성 → "답변 제출"
   - 음성: 🎤 버튼 → 녹음 → 중지 → 자동 전송
   - 영상: 📹 버튼 → 녹화 → 중지 → 자동 전송

4. 피드백 확인
   - AI 피드백: 2-5초 후 자동 표시
   - 면접관 피드백: 실시간 업데이트
```

### 3. 셀프 면접 🏃

```
1. 셀프 면접 생성
   - /selfinterview/create
   - 제목, 질문 개수(3-10개), 난이도, 카테고리 선택
   → AI가 자동으로 질문 생성 (OpenAI API)
   → 실패 시 QuestionPool에서 랜덤 추출

2. 면접 진행
   - 첫 질문 표시 → 타이머 시작
   - 답변 제출 (텍스트/음성/영상)
   → 즉시 AI 피드백 생성 (동기식)
   → 1.5초 후 자동으로 다음 질문

3. 결과 확인
   - 전체 완료 → 통계 화면
   - 평균 점수, 소요 시간, 질문별 피드백
```

### 4. 음성/영상 통화 (Agora RTC) 📞

```
1. 세션 생성 시 타입 선택: AUDIO 또는 VIDEO

2. 세션 입장 → 자동으로 Agora 토큰 발급

3. 통화 시작
   - 마이크/카메라 권한 허용
   - 다른 참가자 영상/음성 자동 연결

4. 통화 중 질문/답변 동시 진행 가능
```

---

## 🔧 핵심 기능 상세

### WebSocket 실시간 통신

#### 구독 토픽
```javascript
/topic/session/{sessionId}/question              // 질문 수신
/topic/session/{sessionId}/answer                // 답변 수신
/topic/session/{sessionId}/feedback              // AI 피드백 수신
/topic/session/{sessionId}/interviewer-feedback  // 면접관 피드백
/topic/session/{sessionId}/status                // 세션 상태 변경
/topic/session/{sessionId}/control               // 제어 메시지
```

#### 발행 경로
```javascript
/app/session/{sessionId}/question   // 질문 출제
/app/session/{sessionId}/answer     // 답변 제출
/app/session/{sessionId}/join       // 세션 참가
/app/session/{sessionId}/start      // 세션 시작
/app/session/{sessionId}/end        // 세션 종료
```

### AI 피드백 생성

#### 평가 루브릭 (100점 만점)
```
1. STAR 구조 (30점)
   - Situation (상황): 7.5점
   - Task (과제): 7.5점
   - Action (행동): 7.5점
   - Result (결과): 7.5점

2. 내용 완성도 (25점)
   - 질문 직접 답변 여부
   - 논리적 일관성
   - 근거 충분성

3. 구체성 (25점)
   - 실제 경험/사례 제시
   - 수치 데이터 활용
   - 상세한 설명

4. 전문성 (20점)
   - 분야 이해도
   - 전문 용어 사용
   - 깊이 있는 분석
```

#### 피드백 구조
```json
{
  "score": 85,
  "summary": "명확한 STAR 구조의 답변",
  "strengths": "구체적인 수치 제시, 결과 명확",
  "weaknesses": "행동 과정 설명 부족",
  "improvement": "어떤 방법으로 문제를 해결했는지 구체적으로 서술"
}
```

### 음성/영상 처리 (FFmpeg)

#### 왜 FFmpeg가 필요한가?
```
OpenAI Whisper API 제한: 최대 25MB
브라우저 녹화 파일:
  - 2분 영상: 약 45MB (WebM)
  - 5분 음성: 약 28MB (WebM)

→ FFmpeg 압축 후:
  - 2분 영상: 약 2.8MB (MP3)
  - 5분 음성: 약 4.2MB (MP3)

압축률: 약 85-94% 감소!
```

#### 압축 과정
```
원본 파일 (45MB WebM)
    ↓ 비디오 제거 (-vn)
    ↓ 샘플레이트 16kHz (-ar 16000)
    ↓ 모노 변환 (-ac 1)
    ↓ 비트레이트 24kbps (-b:a 24k)
    ↓ MP3 변환 (-f mp3)
압축 파일 (2.8MB MP3)
```

---

## 🗂️ 프로젝트 구조

```
MockerView/
├── src/
│   ├── main/
│   │   ├── java/com/mockerview/
│   │   │   ├── config/              # 설정 클래스
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   ├── controller/          # 컨트롤러
│   │   │   │   ├── api/             # REST API
│   │   │   │   ├── web/             # 웹 페이지
│   │   │   │   └── websocket/       # WebSocket
│   │   │   │
│   │   │   ├── dto/                 # 데이터 전송 객체
│   │   │   ├── entity/              # 엔티티 (DB 테이블)
│   │   │   ├── repository/          # 데이터 접근 계층
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   ├── jwt/                 # JWT 인증
│   │   │   ├── scheduler/           # 스케줄러
│   │   │   └── exception/           # 예외 처리
│   │   │
│   │   └── resources/
│   │       ├── application.yml      # 설정 파일
│   │       ├── schema-oracle.sql    # DB 스키마
│   │       ├── static/              # 정적 리소스
│   │       │   ├── css/
│   │       │   └── js/
│   │       └── templates/           # Thymeleaf 템플릿
│   │
│   └── test/                        # 테스트 코드
│
├── build.gradle                     # Gradle 빌드 설정
├── .env                            # 환경 변수 (git 제외)
├── .gitignore
└── README.md
```

---

## 👥 팀 역할 분담

### 🔧 백엔드

| 담당자 | 역할 | 주요 업무 |
|--------|------|-----------|
| **이다겸** (팀장) | WebSocket & AI 통합 | WebSocket 실시간 통신 (STOMP + SockJS)<br>AI 피드백 시스템 (OpenAI GPT-4o-mini)<br>Whisper API 음성→텍스트 변환<br>Agora SDK 음성/화상 통합<br>실시간 점수판 동기화 |
| **윤호준** | 인증 & 보안 | Spring Security 설정<br>회원가입·로그인<br>JWT 토큰 발급 및 쿠키 관리<br>Role 분리 (STUDENT/HOST/REVIEWER)<br>회원 탈퇴 기능 |
| **서원준** | 세션 & Git 관리 | 세션 생성/관리/삭제<br>상태 관리 (PLANNED/RUNNING/ENDED)<br>REST API 설계<br>Git 브랜치 전략·병합 관리<br>호스트 전용 세션 수동 종료<br>스케줄러 기반 자동 만료 |
| **김은채** | 질문·답변 & 리뷰 | 질문·답변 CRUD<br>면접관 리뷰 (Review) CRUD<br>예외 처리 및 페이징<br>AI·인간 피드백 데이터 병합<br>답변 히스토리 관리 |

### 🎨 프론트엔드 & 기타

| 담당자 | 역할 | 주요 업무 |
|--------|------|-----------|
| **최수인** | UI/UX & 검색 | 순수 CSS 반응형 디자인<br>Thymeleaf 템플릿 설계<br>JS/Ajax 비동기 처리<br>키워드 검색 및 필터링<br>모바일 대응 |
| **이수정** | DB 설계 & 검색 지원 | ERD 설계<br>테이블 정의 및 관계 설정<br>데이터 흐름 구성<br>성능 최적화<br>검색 기능 지원 |

---

## 🐛 트러블슈팅

### WebSocket 연결 실패
```
증상: "WebSocket 연결이 실패했습니다" 알림

해결:
1. 브라우저 콘솔(F12) 확인
   - "❌ 토큰을 찾을 수 없음" → 로그인 다시 시도
   - "❌ 토큰 만료됨" → 재로그인 (3시간 후 만료)

2. Cookie 확인 (F12 → Application → Cookies)
   - "Authorization" 쿠키 있는지 확인

3. 서버 로그 확인
   - "WebSocket 인증 성공" 로그 있는지 확인
```

### AI 피드백 생성 안 됨
```
증상: "AI 분석 중..." 에서 멈춤

해결:
1. 환경 변수 확인
   echo $env:OPENAI_API_KEY  (PowerShell)

2. API 키 유효성 확인
   - OpenAI 대시보드에서 키 활성화 상태 확인
   - 사용량 한도 초과 여부 확인

3. 네트워크 확인
   - 방화벽에서 OpenAI API 차단되지 않았는지 확인
```

### 음성 인식 실패
```
증상: "음성 인식에 실패했습니다" 에러

해결:
1. FFmpeg 설치 확인
   ffmpeg -version

2. 없으면 재설치
   choco install ffmpeg -y --force

3. VSCode/PowerShell 완전 재시작

4. 녹음 시간 확인
   - 30분 이상이면 압축 후에도 25MB 초과 가능
   - 답변을 짧게 (2-5분 권장)
```

### 포트 8080 이미 사용 중
```
증상: "Port 8080 is already in use"

해결:
1. 프로세스 종료
   netstat -ano | findstr :8080
   taskkill /PID [PID번호] /F

2. 또는 서버 재시작
```

---

## 📊 주요 기능 요구사항 체크리스트

### ✅ 백엔드
- [x] 세션 생성/조회/종료 기능
- [x] 세션 수동 종료 (호스트 전용)
- [x] 질문 등록/조회 기능
- [x] 답변 작성/조회 기능
- [x] AI 피드백 자동 생성 (OpenAI API)
- [x] AI 질문 자동 생성 (OpenAI API)
- [x] 면접관 피드백 작성/조회
- [x] 세션 히스토리 조회
- [x] 스케줄러 기반 세션 자동 만료
- [x] 회원가입/로그인/탈퇴 (JWT)
- [x] 권한별 접근 제어 (STUDENT/REVIEWER/HOST)
- [x] 사용자 프로필 관리
- [x] 데이터베이스 설계 (ERD)
- [x] 전역 예외 처리 및 로깅
- [x] PDF/Excel 다운로드 기능

### ✅ 프론트엔드
- [x] 로그인/회원가입 UI
- [x] 세션 목록 페이지
- [x] 세션 룸 UI (질문/답변/피드백 실시간 표시)
- [x] 면접관 피드백 입력 모달
- [x] 마이페이지 (기록, 점수, 히스토리)
- [x] 실시간 점수판/리더보드
- [x] 반응형 디자인 (모바일 대응)

### ✅ 실시간 & AI
- [x] WebSocket 실시간 통신 (STOMP + SockJS)
- [x] 연결 끊김 시 자동 재연결
- [x] 음성/화상 면접 (Agora SDK)
- [x] 음성 텍스트 변환 (Whisper API)

### ✅ 기타
- [x] CSV, PDF 내려받기
- [x] 검색 및 필터링 기능
- [x] 테이블 설계 (ERD)

---

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 🙏 감사의 말

이 프로젝트는 다음 오픈소스 라이브러리를 사용합니다:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [OpenAI API](https://openai.com/)
- [Agora RTC](https://www.agora.io/)
- [FFmpeg](https://ffmpeg.org/)
- [Bootstrap](https://getbootstrap.com/)
- [SockJS](https://github.com/sockjs)
- [STOMP.js](https://stomp-js.github.io/)

---

**⭐️ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!**