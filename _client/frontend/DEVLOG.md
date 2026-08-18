# WITHU 프론트엔드 — 개발 기록

WITHU 웰니스 그룹 챌린지 앱의 프론트엔드(React + Vite) 인수인계 문서입니다. 백엔드는 별도 팀이 작업하며,
이 문서는 지금까지 만든 화면과 백엔드가 붙어야 할 지점을 정리한 자료입니다.

**소스 위치:** `C:\Users\user\withu-app` (`npm run dev`로 로컬 실행, `npm run build`로 정적 빌드)

---

## 1. 화면 구성

| 화면 | 경로 | 설명 |
|---|---|---|
| 로그인 | `/` | 이메일/비밀번호 로그인. 세션이 있으면 자동으로 홈으로 이동 |
| 회원가입 | `/signup` | 이메일/비밀번호 회원가입 |
| 그룹 선택 | `/group-entry` | 그룹 만들기 / 코드로 참여 / 그룹 없이 혼자 시작 |
| 그룹 생성 완료 | `/group-entry/create` | 초대코드 발급 및 복사 |
| 그룹 코드 참여 | `/group-entry/join` | 6자리 코드 입력 |
| 온보딩 | `/onboarding` | STEP1 목표(다이어트/벌크업/건강관리) → STEP2 성별·나이 → STEP3 키·몸무게 |
| 캐릭터 생성 | `/character` | 동물 캐릭터(고양이/개/토끼/곰/여우/펭귄) 선택 — 성별 구분 없음 |
| 마이 (`/my`) | 그룹 미가입 시 홈 | 캐릭터 상태, 오늘 미션, 식단 인증 슬롯 |
| 그룹 피드 (`/group`) | 그룹 가입 시 홈 | 2×2 고정 그리드, 그룹 코드, Day n/7, 내 순위 배지, 7일 종료 시 결과 버튼 |
| 그룹원 프로필 | `/group/member/:id` | 그룹원 캐릭터 상태·인증 여부 (실제 그룹원 데이터는 백엔드 연동 후 채워짐) |
| 식단 인증 | `/meal/:mealKey` | 사진 업로드 → AI 분석(mock) → 달성/미달성만 표시 |
| 랭킹 | `/ranking` | 오늘 달성률 기준 실시간 순위(`buildRanking`). 그룹원이 없으면 "나"만 1등으로 표시 — 정상 동작이며 목업 아님 |
| 상점 | `/shop` | 코인 잔액·구매·착용·캐릭터 종(species) 변경까지 실제로 동작 (로컬 상태 기준) |

**홈 라우팅 규칙** (`src/context/AppContext.jsx`의 `resolveHomeRoute`): 그룹에 속해 있으면 `/group`,
아니면 `/my`가 기본 화면. 그룹 가입/탈퇴 시점마다 이 규칙으로 다시 계산됨.

## 2. 기술 구조

- **스택:** Vite + React 19 + React Router + Tailwind v4
- **상태 관리:** `src/context/AppContext.jsx` — Context + useReducer, localStorage에 자동 저장
- **캐릭터:** 성별 대신 동물 종(species) 기반 — "동물의 숲" 주민 컨셉. `src/components/CharacterAvatar.jsx`
- **디자인 색상:** `siwoo4048-design.github.io/wireframe` 실제 배포 와이어프레임의 CSS에서 추출한 색상표를
  그대로 `src/index.css`의 Tailwind `@theme` 토큰에 반영함 (brand `#6658e8` 등).
- **배포 방식:** `vite-plugin-singlefile`로 HTML 1개 파일로 빌드 가능

### API mock 파일 (백엔드 연동 시 내부만 실제 호출로 교체)

| 파일 | 지금 하는 일 | 실제로는 |
|---|---|---|
| `src/api/authApi.js` | localStorage를 가짜 유저 테이블로 사용, 이메일 중복 체크 | `POST /api/auth/signup`, `POST /api/auth/login` |
| `src/api/groupApi.js` | 코드 형식만 검증하고 항상 "나 혼자"인 그룹을 돌려줌 (가짜 그룹원을 만들지 않음) | `POST /api/groups`, `POST /api/groups/join` — 실제 그룹원 목록 응답 |
| `src/api/missionApi.js` | 목표(goal) 하나로 정해진 풀에서 미션 3개 추림 | GPT-4o 기반 개인 맞춤 미션 생성 (`POST /api/missions/today`) |
| `src/api/mealApi.js` | 랜덤으로 달성/미달성 판정 | GPT-4o Vision 식단 분석 (`POST /api/meals/analyze`) — UI에는 달성 여부만 노출, 목표적합도는 내부용 |

**중요:** 그룹원은 실제로 같은 코드로 참여해야만 생기며, 프론트에서 가짜 이름/닉네임으로 채우지 않음.
따라서 백엔드 연동 전에는 그룹 피드/그룹원 프로필이 "참여 대기 중" 상태로만 보이는 것이 정상입니다.

## 3. 기획서 대비 이번 스코프에서 제외한 것

- **운동 영상 AI 판정**: 기획서 범위 제외 항목이라 미구현. 생활습관 미션은 사진/영상 인증 없이 완료 버튼으로 처리.

### 상점 / 캐릭터 꾸미기 / 랭킹 (실제 동작으로 전환됨)

- 미션(생활습관 인증, 식단 인증) 1개를 완료할 때마다 코인 +10 지급 (`AppContext.jsx`의 `MISSION_COIN_REWARD`).
- `/shop`에서 코인으로 의상 세트 구매(`BUY_OUTFIT`) → 보유 목록(`character.ownedOutfits`)에 추가 → 착용(`SET_OUTFIT`)까지 로컬 상태에 실제로 반영·저장됨. 데일리 세트는 기본 무료 보유.
- 캐릭터 종(species) 변경은 `/shop`에서 무료로 가능 (`SET_CHARACTER` 재사용, 최초 생성 화면과 동일 액션).
- `/ranking`과 `/group`의 "내 순위" 배지, 7일 결과 화면(`ChallengeSummarySheet`)의 최종 순위 모두 `AppContext.jsx`의
  `buildRanking`/`myRankOf`로 계산 — 나 + 실제 참여한 그룹원의 오늘 달성률을 기준으로 정렬함.
- 백엔드 연동 시: 코인 지급/차감은 서버가 검증하는 원장(ledger)으로 옮기고, `BUY_OUTFIT`/`SET_OUTFIT`/`SET_CHARACTER` 디스패치 지점을 실제 API 호출로 교체해야 함. 그룹원의 `achievementRate`도 실시간으로 서버에서 내려받아야 순위가 실제 의미를 가짐.

## 4. 나이 / 키 / 몸무게 입력 범위

`AppContext.jsx`에 `AGE_RANGE`(1~100), `HEIGHT_RANGE`(100~220cm), `WEIGHT_RANGE`(20~150kg)로 정의.
온보딩 입력창(`OnboardingPage.jsx`)에서 입력 즉시 clamp되고, `SET_ONBOARDING` 리듀서에서도 같은 범위로
한 번 더 clamp하기 때문에 다른 진입 경로가 생기더라도 음수·범위 초과 값이 저장될 수 없음.

## 5. 백엔드 팀에 전달할 것

1. 회원 인증: 이메일 회원가입/로그인 API + 세션 토큰 발급 (`authApi.js` 대체)
2. 그룹: 코드 생성/검증, 참여자 목록 응답, 최대 4인 제한 (`groupApi.js` 대체)
3. AI 미션 생성: 목표 + 신체정보 + 이전 수행결과 기반 (`missionApi.js` 대체)
4. AI 식단 분석: 사진 업로드 → 달성 여부 판정, 목표적합도는 내부 데이터로만 다음 미션에 반영 (`mealApi.js` 대체)
5. 그룹 피드 실시간/폴링: 그룹원의 실제 인증 이벤트를 받아 `/group`, `/group/member/:id`에 반영
