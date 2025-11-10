MockerView PRO - 한글 배치 파일 (포트 8082)
==========================================

수정 사항
---------
✅ iconv로 UTF-8 → CP949 변환 (제대로 된 인코딩!)
✅ chcp 명령어 없음
✅ 포트번호 8082
✅ 한글 완벽 지원

파일 목록
---------
[배치 파일 - Windows]
- init-db-pro.bat       : DB 초기화 (최초 1회)
- check-status-pro.bat  : 상태 확인
- run-local-pro.bat     : 로컬 실행
- stop-local-pro.bat    : 중지
- reset-db-pro.bat      : DB 리셋

[쉘 스크립트 - Linux/Mac]
- init-db-pro.sh
- run-local-pro.sh
- stop-local-pro.sh
- auto-switch-to-pro.sh

[설정 파일]
- docker-compose.yml
- application-pro.yml
- build.gradle
- init-pro.sql
- Dockerfile
- gradlew.bat

사용법
------
1. Docker Desktop 실행
2. init-db-pro.bat 더블클릭
3. run-local-pro.bat 더블클릭
4. http://localhost:8082 접속

포트
----
- App: 8082
- PostgreSQL: 5433
- Redis: 6380

주의
----
- 명령 프롬프트(cmd)에서 실행
- PowerShell 금지
- 한글 잘 나옴!

인코딩 정보
-----------
이 배치 파일들은 iconv를 사용하여
UTF-8에서 CP949(Windows 기본 인코딩)로
정확하게 변환되었습니다.

한글이 깨지지 않습니다!
