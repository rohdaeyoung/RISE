# WITHU

AI 기반 개인 맞춤 건강 미션과 그룹 동기부여를 결합한 힐링 웰니스 그룹 서비스의 프론트엔드 프로토타입입니다.

친구들과 그룹을 이루어 각자 AI가 생성한 맞춤 건강 미션을 수행하고, 식단 사진을 인증하면 캐릭터의 표정과 성장으로 결과를 확인할 수 있습니다.

> ## 이어받는 사람(그리고 AI)에게
>
> **현재 상태**: 백엔드(`RISE-server`) 연동까지 끝났고, 브라우저에서 전 기능 동작을 확인했습니다.
> mock 모드도 그대로 살아 있어서 백엔드 없이 프론트만 띄워도 전체 플로우가 돌아갑니다.
> 남은 건 배포입니다.
>
> **작업 전 알아둘 것**
> - **mock 코드를 지우지 마세요.** 백엔드 없이 데모(Netlify 정적 배포)하기 위해 일부러 남긴
>   구조입니다. 전환 지점은 `src/api/client.js`의 `isBackendEnabled` 한 곳뿐입니다.
> - **서버가 주는 사진 경로는 `fileUrl()`로 감싸세요.** 서버는 `/api/files/{id}` 상대경로만
>   주는데 프론트는 다른 origin에서 돌아가서, 그대로 `<img src>`에 넣으면 이미지가 깨집니다.
> - **캐릭터 표정 규칙(`expressionFromRank`)은 백엔드 `ExpressionPolicy`와 쌍입니다.**
>   한쪽만 고치면 화면과 서버 값이 어긋나니 둘 다 같이 고쳐야 합니다.
> - **진행일(Day n/7)은 서버의 `currentDay`가 원본입니다.** 기기 시계로 계산하면 그룹원마다
>   다른 날짜가 보입니다. 로컬 계산은 mock 모드 전용 폴백입니다.
> - **코인·의상·보유 목록의 원본은 서버입니다.** 로컬 상태만 믿으면 새로고침 시 사라집니다.
>
> API 계약을 바꿀 때는 `src/api/*.js`의 매핑 함수와 백엔드 DTO를 **양쪽 다** 확인하세요.
> 실제로 "프론트는 그 필드를 쓰는데 서버가 안 준다" 유형의 버그가 여러 건 나왔고,
> 이건 API 응답만 봐서는 안 보이고 브라우저로 화면을 봐야 발견됩니다.

## 기술 스택

- Vite + React 19 + React Router
- Tailwind CSS v4
- Context + useReducer 기반 상태 관리 (localStorage 자동 저장)

## 실행 방법

```bash
npm install
npm run dev      # 로컬 개발 서버
npm run build    # 정적 빌드 (vite-plugin-singlefile로 HTML 1개 파일 생성)
npm run lint      # oxlint
```

## 화면 구성

| 화면 | 경로 | 설명 |
|---|---|---|
| 로그인 | `/` | 이메일/비밀번호 로그인, 세션 있으면 자동 홈 이동 |
| 회원가입 | `/signup` | 이메일/비밀번호 회원가입 |
| 그룹 선택 | `/group-entry` | 그룹 만들기 / 코드로 참여 / 혼자 시작 |
| 그룹 생성 완료 | `/group-entry/create` | 초대코드 발급 및 복사 |
| 그룹 코드 참여 | `/group-entry/join` | 6자리 코드 입력 |
| 온보딩 | `/onboarding` | 목표 → 성별·나이 → 키·몸무게 3단계 |
| 캐릭터 생성 | `/character` | 동물 캐릭터 선택 (성별 구분 없음) |
| 마이 | `/my` | 캐릭터 상태, 오늘 미션, 식단 인증 슬롯 |
| 그룹 피드 | `/group` | 그룹원 현황, 그룹 코드, Day n/7, 내 순위 |
| 그룹원 프로필 | `/group/member/:id` | 그룹원 캐릭터 상태·인증 여부 |
| 식단 인증 | `/meal/:mealKey` | 사진 업로드 → AI 분석(mock) → 달성 여부 표시 |
| 랭킹 | `/ranking` | 오늘 달성률 기준 실시간 순위 |
| 상점 | `/shop` | 코인으로 의상 구매·착용, 캐릭터 종 변경 |

## 프로젝트 구조

```
src/
  api/          # 백엔드 연동 지점 (현재는 mock)
  assets/       # 캐릭터·의상 이미지
  components/   # 재사용 UI 컴포넌트
  context/      # AppContext (전역 상태)
  pages/        # 라우트별 화면
```

## 백엔드 연동

프론트는 **백엔드 없이도 단독으로 동작**하고, 환경변수 하나로 실제 백엔드에 붙습니다.

```bash
# 백엔드 연동 모드 — frontend/.env.local 생성
VITE_API_BASE_URL=http://localhost:8080

# mock 모드 — 위 값을 비우거나 .env.local을 지우면 됨 (기본값)
```

| 모드 | 조건 | 동작 |
|---|---|---|
| **mock** | `VITE_API_BASE_URL` 없음 | localStorage 기반. 백엔드/DB 없이 프론트만 띄워도 전체 플로우 체험 가능 (Netlify 정적 배포용) |
| **백엔드 연동** | `VITE_API_BASE_URL` 설정 | 실제 서버 호출. AI 미션 생성·식단 분석은 GPT-4o가 수행 |

전환 지점은 `src/api/client.js`의 `isBackendEnabled` 한 곳이고, 각 api 모듈이 이 값을 보고 분기합니다.
페이지 컴포넌트는 두 모드에서 동일하게 동작하므로, 백엔드 작업과 프론트 작업을 서로 막지 않고 진행할 수 있습니다.

| 파일 | mock 모드 | 백엔드 연동 시 |
|---|---|---|
| `authApi.js` | localStorage 가짜 유저 테이블 | `POST /api/auth/signup`, `POST /api/auth/login` (JWT 발급) |
| `groupApi.js` | 코드 형식만 검증, 항상 "나 혼자" 그룹 | `POST /api/groups`, `/join` — 실제 그룹원 목록 |
| `missionApi.js` | 목표별 고정 풀에서 미션 3개 추출 | `POST /api/missions/today` — GPT-4o 개인 맞춤 생성 |
| `mealApi.js` | 랜덤 달성/미달성 판정 | `POST /api/meals/{slot}/analyze` — GPT-4o Vision 분석 |
| `challengeApi.js` | 리듀서가 로컬로 결과 계산 | `POST /api/challenges/end` — 서버가 순위·보상 정산 |
| `profileApi.js` | (미사용) | 캐릭터·온보딩·상점·랭킹 |

자세한 화면별 동작과 백엔드 전달 사항은 [`DEVLOG.md`](./DEVLOG.md), 전체 기획은 [`docs/PRD.md`](./docs/PRD.md)를 참고하세요.

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 배포용 |
| `develop` | 개발 통합 |
| `feature/FE-*` | 기능 개발 |
| `hotfix/*` | 긴급 수정 |
