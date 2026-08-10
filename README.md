# WITHU

AI 기반 개인 맞춤 건강 미션과 그룹 동기부여를 결합한 힐링 웰니스 그룹 서비스의 프론트엔드 프로토타입입니다.

친구들과 그룹을 이루어 각자 AI가 생성한 맞춤 건강 미션을 수행하고, 식단 사진을 인증하면 캐릭터의 표정과 성장으로 결과를 확인할 수 있습니다.

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

## 백엔드 연동 현황

현재 `src/api/`의 각 파일은 백엔드 없이 동작하도록 mock으로 구현되어 있습니다. 실제 연동 시 아래 API로 교체가 필요합니다.

| 파일 | 현재 | 연동 시 |
|---|---|---|
| `authApi.js` | localStorage 가짜 유저 테이블 | `POST /api/auth/signup`, `POST /api/auth/login` |
| `groupApi.js` | 코드 형식만 검증, 항상 "나 혼자" 그룹 반환 | `POST /api/groups`, `POST /api/groups/join` |
| `missionApi.js` | 목표별 고정 풀에서 미션 3개 추출 | GPT-4o 기반 개인 맞춤 미션 생성 |
| `mealApi.js` | 랜덤 달성/미달성 판정 | GPT-4o Vision 식단 분석 |

자세한 화면별 동작과 백엔드 전달 사항은 [`DEVLOG.md`](./DEVLOG.md), 전체 기획은 [`docs/PRD.md`](./docs/PRD.md)를 참고하세요.
