# RISE Server

WITHU — AI 기반 개인 맞춤 건강 미션과 그룹 동기부여를 결합한 웰니스 그룹 서비스의 백엔드입니다.

## 기술 스택

- Spring Boot 4 (Java 21) + Gradle
- Spring Data JPA + MySQL
- Spring Security + JWT (Access Token)
- springdoc-openapi (Swagger UI)

## 실행 방법

```bash
# 로컬 MySQL에 withu 데이터베이스 생성
mysql -u root -e "CREATE DATABASE withu CHARACTER SET utf8mb4;"

# 환경변수(DB_USERNAME/DB_PASSWORD/JWT_SECRET 등)는 필요 시 application-local.yml 참고
./gradlew bootRun
```

기동 후 Swagger UI: http://localhost:8080/swagger-ui.html

## 프로젝트 구조

도메인(기능)별 패키지로 분리되어 있고, 각 도메인 내부는 `controller / service / repository / entity / dto`로 나뉩니다.

```
com.withu
  ├── global/        공통 설정, 예외 처리, JWT 시큐리티, 공통 응답 포맷
  ├── auth/           회원가입 / 로그인
  ├── character/      캐릭터 생성 / 종 변경
  ├── group/          그룹 생성 / 참여 / 설정
  ├── onboarding/     목표 / 신체정보 (그룹 사이클마다 갱신)
  ├── mission/        일일 개인 맞춤 미션 생성 / 인증
  ├── meal/            식단 사진 인증 / AI 분석
  ├── shop/            코인 / 의상 구매·착용
  ├── ranking/         그룹 내 / 전체 랭킹
  └── ai/              AI 연동 포트(MissionAiClient, MealVisionAiClient) + mock 구현체
```

## AI 연동 (mock → 실제 전환)

`ai` 패키지의 `MissionAiClient`, `MealVisionAiClient` 인터페이스가 AI 연동 지점입니다.
현재는 `ai/mock`의 mock 구현체(프론트 mock 로직을 그대로 이식)만 등록되어 있고,
OpenAI 키가 준비되면 같은 인터페이스를 구현하는 `OpenAiMissionClient` / `OpenAiMealVisionClient`를
추가해 mock 대신 등록하면 됩니다. 도메인 서비스(`MissionService`, `MealService`) 코드는 수정할 필요가 없습니다.

## API 개요

| 도메인 | 엔드포인트 |
|---|---|
| 인증 | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me` |
| 캐릭터 | `POST /api/characters`, `GET /api/characters/me`, `PATCH /api/characters/me/species` |
| 그룹 | `POST /api/groups`, `POST /api/groups/join`, `GET /api/groups/me`, `DELETE /api/groups/me`, `PATCH /api/groups/me/name`, `PATCH /api/groups/me/mission-time` |
| 온보딩 | `POST /api/onboarding`, `GET /api/onboarding/me` |
| 미션 | `POST /api/missions/today`, `GET /api/missions/today`, `POST /api/missions/{id}/verify` |
| 식단 | `POST /api/meals/{slot}/analyze` (multipart), `GET /api/meals/today` |
| 상점 | `GET /api/shop/outfits`, `POST /api/shop/outfits/{id}/buy`, `POST /api/shop/outfits/{id}/wear` |
| 랭킹 | `GET /api/rankings/group`, `GET /api/rankings/global` |

모든 응답은 `{ success, data, error }` 형태(`ApiResponse`)로 감싸집니다. 인증이 필요한 API는
`Authorization: Bearer {accessToken}` 헤더가 필요합니다 (회원가입/로그인 제외).

자세한 기획은 프론트 저장소의 `frontend/docs/PRD.md`, `frontend/DEVLOG.md`를 참고하세요.

## 앞으로 할 일

- OpenAI(GPT-4o / GPT-4o Vision) 실제 연동 — 키 발급 후 `ai` 패키지에 구현체 추가
- 그룹 랭킹 실시간 반영을 위한 폴링/웹소켓 방식 검토
- 미션 난이도 자동 조절 (달성률 기반) — 현재는 mock 생성기에 미반영, PRD 6번 참고
- 챌린지 7일 종료 처리(결과 화면, 보상 지급) API
- 실제 배포 환경에서의 파일 스토리지(S3 등) 전환 — 현재는 로컬 디스크(`FileStorageService`)

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 배포용 |
| `develop` | 개발 통합 |
| `feature/BE-*` | 기능 개발 |
| `hotfix/*` | 긴급 수정 |
