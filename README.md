# RISE Client

> AI 기반 건강 미션 & 그룹 웰니스 서비스 — 프론트엔드

## 기술 스택

> 확정 후 업데이트 예정

| 항목 | 기술 |
|------|------|
| Framework | TBD (React Native / Flutter) |
| 상태관리 | TBD |
| API 통신 | TBD |

## 브랜치 전략

```
main          ← 배포용 (PR + 리뷰 필수)
develop       ← 개발 통합 브랜치
feature/FE-*  ← 기능 개발 (develop에서 분기 → develop으로 PR)
hotfix/*      ← 긴급 수정 (main → main + develop PR)
```

### 브랜치 네이밍
```
feature/FE-login-screen
feature/FE-group-feed
feature/FE-mission-upload
hotfix/FE-camera-permission
```

## 화면 구조

```
src/screens/
├── auth/        # 로그인·회원가입
├── onboarding/  # 온보딩 3단계
├── group/       # 그룹 홈 (캐릭터·현황)
├── mission/     # 오늘의 미션
├── feed/        # 그룹 피드
├── ranking/     # 방 내·전체 랭킹
├── store/       # 상점
└── mypage/      # 마이페이지
```

## API 연동

→ `docs/api-spec.md` 참고 (백엔드팀과 동일 문서 사용)

## API 변경 알림

백엔드 팀이 API를 변경하면 `RISE-server`의 `docs/api-spec.md`를 업데이트하고 프론트팀에 공지합니다.

## PR 규칙

- PR 제목 형식: `[FE] 화면명 - 간략 설명`
- develop PR: 팀원 1명 이상 리뷰 필수
- main PR: 프론트 팀원 전원 리뷰 필수
