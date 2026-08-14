# RISE Client (WITHU 프론트엔드)

WITHU — AI 기반 개인 맞춤 건강 미션과 그룹 동기부여를 결합한 웰니스 그룹 서비스의 프론트엔드입니다.
멋쟁이사자처럼 대학 14기 중앙 해커톤(ANIMAL LEAGUE) **AAC 트랙** 출품작, 성결대 3팀.

## 배포된 주소

| 무엇 | 주소 |
|---|---|
| **웹앱** | https://rise-client-rohdaeyoungs-projects.vercel.app |
| 백엔드 API 문서 | https://rise-server-production.up.railway.app/swagger-ui.html |
| 백엔드 저장소 | https://github.com/rohdaeyoung/RISE-server |

**테스트 계정: `test@withu.app` / `withu1234`** (그룹 코드 `TEAM33`, Day 7 상태)

`main`에 push하면 Vercel이 자동으로 다시 배포합니다.

---

## 시작하기

```bash
cd frontend
npm install
npm run dev
```

`http://localhost:5173` 이 열립니다. **이대로도 그냥 돌아갑니다** — 백엔드 없이
브라우저 안에서만 도는 mock 모드입니다. 화면 작업만 할 거라면 이걸로 충분합니다.

### 실제 백엔드에 붙여서 개발하기

```bash
cd frontend
echo "VITE_API_BASE_URL=https://rise-server-production.up.railway.app" > .env.local
npm run dev
```

배포된 백엔드에 그대로 붙습니다. 별도 서버를 띄울 필요가 없습니다.
`.env.local`은 git에 올라가지 않으니 각자 만들어 쓰세요.

> **주의**: 배포된 백엔드는 실제 DB를 씁니다. 여기서 만든 계정과 그룹은 다른 팀원에게도 보입니다.
> 마음껏 부수면서 실험하려면 백엔드를 로컬로 띄우세요 (`RISE-server` README의 "빠르게 실행하기").

`VITE_API_BASE_URL`을 지우면 다시 mock 모드가 됩니다. 백엔드 없이도 데모할 수 있도록
일부러 분리해 둔 구조이니, **연동한다고 mock 코드를 지우지 마세요.**

---

## 구조

```
frontend/src
  ├── api/          서버 통신. mock ↔ 실제 백엔드 전환도 여기서 일어남
  │   ├── client.js       ★ 공통 fetch 래퍼, 토큰 처리, 이미지 주소 변환
  │   ├── authApi.js      로그인 / 회원가입
  │   ├── groupApi.js     그룹 생성·참여·설정·나가기
  │   ├── missionApi.js   미션 조회·인증, 시간대별 잠금 해제
  │   ├── mealApi.js      식단 사진 업로드 / AI 분석
  │   └── profileApi.js   내 정보, 캐릭터, 온보딩
  ├── context/
  │   └── AppContext.jsx  ★ 앱 전체 상태(useReducer) + 서버 동기화
  ├── pages/        화면 단위 컴포넌트
  ├── components/   재사용 UI (캐릭터, 미션 카드, 결과 시트 등)
  └── utils/        이미지 리사이즈 등
```

★ 표시한 두 파일이 핵심입니다. 고치기 전에 먼저 읽으세요.

### `client.js` — 모든 통신이 지나가는 곳

- `isBackendEnabled` : `VITE_API_BASE_URL` 설정 여부. 각 API 파일이 이 값으로 mock/실서버를 가름
- `api.get/post/patch/delete/postForm` : 서버 응답 `{ success, data, error }` 껍데기를 벗겨 `data`만 돌려줌
- `fileUrl(path)` : 서버가 주는 `/api/files/{id}` 같은 상대 경로를 완전한 주소로 바꿈.
  **이걸 안 거치면 이미지가 프론트 서버를 찾아가서 깨집니다.**

### `AppContext.jsx` — 상태와 동기화

- `useReducer` 기반. 화면은 `useAppState()` / `useAppDispatch()`로 접근
- `sync()` 가 **15초마다** 서버에서 내 정보·캐릭터·그룹·미션·식단을 다시 받아옴
- 상태는 localStorage에 자동 저장되어 새로고침해도 남음

---

## 고칠 때 꼭 알아야 할 것

### 1. 서버에 남아야 하는 값은 반드시 API를 호출하세요

가장 많이 낸 실수입니다. `dispatch`만 하면 **내 화면에서만 잠깐 바뀌었다가,
15초 뒤 `sync()`가 서버 값을 도로 가져와 원래대로 돌아갑니다.**

```js
// ✗ 이렇게 하면 그룹을 나간 것처럼 보였다가 다시 들어와집니다
function handleLeaveGroup() {
  dispatch({ type: 'LEAVE_GROUP' });
  navigate('/my');
}

// ✓ 서버에도 알려야 진짜로 나가집니다
function handleLeaveGroup() {
  leaveGroup()
    .catch(() => {})
    .finally(() => {
      dispatch({ type: 'LEAVE_GROUP' });
      navigate('/my');
    });
}
```

실제로 **그룹 나가기, 방 이름 변경, 미션 시작 시간 변경** 세 가지가 이 문제였습니다.
상태를 바꾸는 화면을 만들면 `dispatch` 옆에 API 호출이 있는지 확인하세요.

### 2. 반대로, 서버에 있는 값은 `sync()`가 받아와야 합니다

식단 인증이 **새로고침하면 사라지는** 문제가 있었습니다. `sync()`가 미션·그룹·코인은
받아오는데 식단만 빠져 있었기 때문입니다. 새 데이터를 서버에 저장하기 시작했다면
`AppContext.jsx`의 `sync()`에도 조회를 추가하세요.

### 3. 미션 개수는 3개가 아닐 수 있습니다

백엔드가 상황에 따라 **1개 / 3개 / 4개**를 내려줍니다
(달성률 90% 이상이면 4개, 3일 연속 실패하면 1개).
3개를 전제로 짜면 화면이 깨집니다.

### 4. 이미지 주소는 `fileUrl()`을 거치세요

서버는 `/api/files/{uuid}` 형태의 상대 경로를 줍니다. 그대로 `<img src>`에 넣으면
프론트 주소를 찾아가서 깨집니다.

### 5. `internalFit`은 화면에 띄우지 마세요

식단 분석 결과의 목표적합도(`good`/`normal`/`bad`)는 **다음 미션 난이도를 정하는 내부 값**입니다.
기획상 사용자에게는 "달성 / 미달성"만 보여줍니다. (애초에 서버가 응답에 담지 않습니다.)

### 6. 이미지가 무거우니 단일 파일 번들로 되돌리지 마세요

`vite-plugin-singlefile`을 쓰면 캐릭터·의상 이미지까지 HTML 하나에 인라인되어
첫 화면이 14MB가 되고, 다 받을 때까지 흰 화면이 유지됩니다.
나눠서 내보내면 처음엔 100KB 정도만 받습니다. 자세한 건 `vite.config.js` 주석 참고.

---

## 최근 수정 (2026-08-14)

팀 피드백으로 올라온 문제들을 반영했습니다. 관련 파일과 함께 정리합니다.

1. **미션이 시간대별로 나눠 오던 문제** — "다음 미션은 O시에 도착해요"가 뜨던 것을,
   하루치(식단 2개 + 생활습관 1개)가 한 번에 도착하도록 변경. (`api/missionApi.js`)
2. **걷기 미션에 아무 사진이나 인증되던 문제** — 생활습관 미션 인증이 사진 없이 호출되고
   있어서, 걷기 미션에 음식 사진을 올려도 통과했습니다. 사진을 실제로 서버에 함께
   전송하도록 수정. (`api/missionApi.js`, `pages/MissionVerifyPage.jsx`)
3. **식단 인증 화면에 미션 제목이 안 뜨던 문제** — 모든 끼니에서 "아침 사진 업로드" 같은
   똑같은 문구만 떴던 것을, 실제 미션 제목을 보여주도록 변경. (`pages/MealUploadPage.jsx`)
4. **7일 결산에서 구매한 옷이 반영 안 되던 문제** — 랭킹·그룹 피드에는 넘기던 `outfit`
   값을 결산 화면(`ChallengeSummarySheet`)에는 빼먹어서, 상점에서 산 옷을 입고 있어도
   결산에서만 기본 옷으로 보였습니다. (`components/ChallengeSummarySheet.jsx`)
5. **계정 탈퇴가 로컬에서만 처리되던 문제** — 로그아웃만 하고 화면에서 사라진 것처럼
   보였지만 서버 DB에는 계정이 그대로 남아, 같은 이메일로 재가입이 막혔습니다.
   서버 삭제 API를 실제로 호출하도록 수정. (`api/authApi.js`, `pages/SettingsPage.jsx`)
6. **그룹 나가기 이후에도 서버 상태를 못 따라가던 문제** — 그룹 조회가 실패했을 때
   네트워크 오류와 "진짜로 그룹원이 아님(403)"을 구분하지 않아, 나간 뒤에도 화면에
   그룹이 계속 보이는 유령 상태가 됐습니다. 에러에 상태 코드를 실어 구분하도록 수정.
   (`api/client.js`, `context/AppContext.jsx`)
7. **미션이 저절로 바뀌어 보이던 문제** — 그룹/온보딩 시점에 프론트가 임시로 만든
   미션이 화면에 잠깐 보였다가, 서버 동기화가 실제 미션 세트로 갈아끼우면서 미션이
   저절로 바뀐 것처럼 보였습니다. 백엔드 연동 모드에서는 프론트가 미션을 만들지
   않도록 정리. (`context/AppContext.jsx`)
8. **방 이름·미션 시간이 참여자에게 다르게 보이던 문제** — 그룹 설정 변경이 동기화에
   반영되지 않아, 나중에 참여한 사람이나 방장이 이름을 바꾼 뒤에도 예전 값이 그대로
   보였습니다. (`context/AppContext.jsx`)
9. **"계속하기" 시 온보딩 없이 새 사이클이 시작되던 문제** — 목표/신체정보를 다시
   묻지 않고 바로 그룹 화면으로 넘어갔던 것을, PRD대로 사이클마다 온보딩을 다시
   거치도록 수정. (`context/AppContext.jsx`, `components/ChallengeSummarySheet.jsx`)

---

## 브랜치

`main`이 기준입니다. Vercel이 여기서 자동 배포합니다.

작업할 때는 `feature/FE-*` 브랜치를 파고, 끝나면 `main`으로 합쳐주세요.

```bash
git checkout main
git pull
git checkout -b feature/FE-작업이름
```

## 기술 스택

Vite · React 19 · React Router (HashRouter) · Tailwind CSS v4 · lucide-react
