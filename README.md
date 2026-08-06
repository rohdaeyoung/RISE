# RISE Server

> AI 기반 건강 미션 & 그룹 웰니스 서비스 — 백엔드

## 기술 스택

| 항목 | 기술 |
|------|------|
| Framework | Quarkus |
| Language | Java 17 |
| DB | PostgreSQL |
| ORM | Hibernate Panache |
| Auth | JWT (SmallRye JWT) |
| AI | Claude API (Anthropic) |

## 브랜치 전략

```
main          ← 배포용 (PR + 리뷰 필수)
develop       ← 개발 통합 브랜치
feature/BE-*  ← 기능 개발 브랜치 (develop에서 분기 → develop으로 PR)
hotfix/*      ← 긴급 수정 (main → main + develop PR)
```

### 브랜치 네이밍
```
feature/BE-auth-login
feature/BE-ai-mission-generate
feature/BE-group-invite-code
hotfix/BE-jwt-expiry
```

## 패키지 구조

```
src/main/java/com/rise/
├── auth/        # 로그인 / JWT 인증
├── user/        # 사용자 계정
├── group/       # 그룹 생성·참여·코드 발급
├── mission/     # AI 미션 생성·조회·달성
├── feed/        # 그룹 피드 (식단 사진 + AI 분석)
├── character/   # 캐릭터 상태·레벨·표정
├── ranking/     # 방 내·전체 랭킹
└── coin/        # 코인·보상
```

## API 명세

→ `docs/api-spec.md` 참고 (프론트팀과 공유)

## 로컬 실행

```bash
cp .env.example .env   # 환경변수 설정
./mvnw quarkus:dev
```

## PR 규칙

- PR 제목 형식: `[BE] 기능명 - 간략 설명`
- develop PR: 팀원 1명 이상 리뷰 필수
- main PR: 백엔드 팀원 전원 리뷰 필수
- API 변경 시 `docs/api-spec.md` 업데이트 + 프론트팀 공지 필수
