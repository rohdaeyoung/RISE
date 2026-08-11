# RISE Server (WITHU 백엔드)

WITHU — AI 기반 개인 맞춤 건강 미션과 그룹 동기부여를 결합한 웰니스 그룹 서비스의 백엔드입니다.
멋쟁이사자처럼 대학 14기 중앙 해커톤(ANIMAL LEAGUE) **AAC 트랙** 출품작, 성결대 3팀.

> **이어받는 사람(그리고 AI)에게**: 이 문서 하나만 읽으면 바로 이어서 작업할 수 있게 썼습니다.
> 특히 [지금까지 한 일](#지금까지-한-일)과 [건드릴 때 주의할 것](#건드릴-때-주의할-것)은
> 꼭 읽고 시작하세요. 이미 한 번 밟은 지뢰를 다시 밟지 않기 위한 내용입니다.

---

## 현재 상태 한 줄 요약

**MVP 전 기능 + AI 피드백 루프 + 심사용 데모 시더까지 끝났고, 브라우저에서 전 기능 검증 완료.
남은 건 배포입니다.**

### ⚠️ 이어받는 사람이 가장 먼저 알아야 할 것

**프론트(`RISE-client`)에 아직 push하지 않은 커밋이 있습니다.** 백엔드만 올라와 있어서,
지금 GitHub 상태 그대로 두 저장소를 받아 돌리면 아래 기능이 화면에서 동작하지 않습니다.

| 프론트 로컬 커밋 | 없으면 생기는 문제 |
|---|---|
| `2d32666` 세션 복원 | 다른 기기에서 로그인하면 그룹을 못 찾아 "그룹 만들기"만 뜸 (심사 치명적) |
| `06ea7a6` 로그인 상태 초기화 | 로그아웃 없이 다른 계정 로그인 시 앞 사람 데이터가 보임 |
| `36e4712` 닉네임 동기화 | 랭킹·피드 이름이 전부 "그룹원" |
| `49533c5` 의상 동기화 | 산 의상이 새로고침하면 사라짐 |
| `954201e` 피드/결과 화면 | 그룹원 달성률 0%, 사진 안 보임, 결과 화면 안 뜸 |
| (미커밋) 로그아웃 토큰 삭제 | 로그아웃해도 JWT가 기기에 남음 |

프론트 담당자에게 이 커밋들을 push해달라고 요청하세요.

---

## 빠르게 실행하기

```bash
# 1. DB 준비
mysql -u root -e "CREATE DATABASE withu CHARACTER SET utf8mb4;"

# 2. 프로젝트 루트에 .env 생성 (git에 안 올라감 — 절대 커밋하지 말 것)
echo "OPENAI_API_KEY=발급받은-키" > .env

# 3. 실행
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- MySQL이 3306이 아니면: `DB_PORT=3307 ./gradlew bootRun`
- **`.env` 없이도 그냥 돌아갑니다.** 키가 없으면 AI가 mock 구현체로 자동 전환되므로,
  키를 못 받은 사람도 백엔드 개발을 계속할 수 있습니다.

### 프론트와 같이 띄우기

프론트(`RISE-client`)의 `frontend/.env.local`에 아래 한 줄을 넣고 `npm run dev`:

```
VITE_API_BASE_URL=http://localhost:8080
```

이 값을 **지우면 프론트가 mock 모드로 돌아갑니다.** 백엔드 없이 프론트만 데모할 수 있도록
일부러 이렇게 분리해 두었으니, 연동한다고 mock 코드를 지우지 마세요.

---

## 지금까지 한 일

### 구현 완료 (전부 브라우저에서 실제 동작 확인함)

| 기능 | 상태 |
|---|---|
| 회원가입 / 로그인 (JWT) | ✅ |
| 닉네임 설정 | ✅ 랭킹·그룹 피드 표시 이름 |
| 캐릭터 생성 / 종 변경 | ✅ |
| 그룹 생성 / 참여 (6자리 코드, 2~4인) | ✅ |
| 온보딩 (목표·성별·나이·키·몸무게) | ✅ |
| **AI 개인 맞춤 미션 생성** (GPT-4o-mini) | ✅ 목표별로 실제 다른 미션 생성 |
| **AI 피드백 루프** (어제 식단 → 오늘 미션) | ✅ 실패한 끼니를 정조준한 미션 생성 |
| **미션 난이도 자동 조절** (PRD 6) | ✅ 90%↑상승 / 50~90%유지 / 50%↓하향 / 3일연속실패→1개 |
| **AI 식단 사진 분석** (GPT-4o-mini Vision) | ✅ 샐러드 승인 / 치킨 거절 — 실제 판별함 |
| 미션 시간대별 잠금 해제 (0/3.5/7/11시간) | ✅ |
| 코인 지급 / 상점 구매·착용 | ✅ |
| 그룹 피드 (사진·달성률·표정) | ✅ |
| 캐릭터 표정 3단계 | ✅ 달성률+순위로 실시간 계산 |
| 그룹 랭킹 / 전체 랭킹 | ✅ |
| **7일 챌린지 종료 + 순위별 보상** | ✅ 멱등 처리 (두 번 눌러도 중복 지급 없음) |
| 계속하기 / 방 나가기 | ✅ |
| 심사용 데모 데이터 시더 | ✅ `DEMO_SEED=true` |

### 검증하며 잡은 버그 (같은 실수 반복 방지용)

브라우저로 실제 화면을 보며 검증했더니 **API만 봐서는 안 보이던 버그가 7개** 나왔습니다.
이 중 3개는 "프론트는 이미 그 필드를 쓰는데 백엔드가 안 준다" 유형이었습니다.

1. **캐릭터 표정이 항상 NORMAL** — `changeExpression()`을 호출하는 코드가 아예 없었음
2. **그룹 피드 전원 0%** — 프론트가 쓰는 `achievementRate`를 서버가 안 보냄
3. **그룹원 인증 사진 안 보임** — 프론트가 쓰는 `photo`를 서버가 안 보냄
4. **사진 경로가 상대경로** — 프론트/백 origin이 달라 이미지가 깨짐
5. **동점 시 순위가 임의** — 달성률 95%가 71%보다 아래로 가고 우승 뱃지까지 뒤바뀜
6. **Day가 항상 1/7** — 프론트가 로컬 시계로 계산해 서버 `currentDay`를 무시 → 결과 화면이 안 뜸
7. **산 의상이 새로고침하면 사라짐** — 서버에서 `ownedOutfits`를 안 받아옴

> **교훈**: 기능을 추가하면 curl 검증에서 멈추지 말고 **반드시 브라우저로 화면까지 확인**하세요.
> 프론트가 기대하는 필드는 `RISE-client/frontend/src/api/*.js`의 매핑 함수를 먼저 읽고
> 백엔드 DTO와 대조하면 빠르게 찾을 수 있습니다.

---

## 프로젝트 구조

도메인(기능)별 패키지로 분리, 각 도메인 내부는 `controller / service / repository / entity / dto`.

```
com.withu
  ├── global/         공통 설정, 예외 처리, JWT 시큐리티, 공통 응답 포맷(ApiResponse)
  ├── auth/           회원가입 / 로그인
  ├── character/      캐릭터, 표정 계산(ExpressionPolicy, ExpressionResolver)
  ├── group/          그룹 생성 / 참여 / 설정
  ├── onboarding/     목표 / 신체정보 (그룹 사이클마다 갱신)
  ├── mission/        일일 개인 맞춤 미션 생성 / 인증
  ├── meal/           식단 사진 인증 / AI 분석
  ├── challenge/      7일 챌린지 종료 정산, 보상, 뱃지
  ├── file/           사진 저장 (DB BLOB) / 서빙
  ├── shop/           코인 / 의상 구매·착용
  ├── ranking/        그룹 내 / 전체 랭킹
  └── ai/             AI 포트(인터페이스) + openai 구현체 + mock 구현체
```

## AI 연동

`ai` 패키지의 `MissionAiClient`, `MealVisionAiClient` 인터페이스가 유일한 AI 연동 지점입니다.

- `.env`에 `OPENAI_API_KEY`가 **있으면** → `ai/openai`의 실제 구현체가 `@Primary`로 등록
- **없으면** → `ai/mock`의 mock 구현체가 동작

전환은 설정만으로 이뤄지고 `MissionService` / `MealService` 코드는 건드릴 필요가 없습니다.
모델은 `application.yml`의 `openai.mission-model` / `openai.vision-model`에서 변경 (기본 `gpt-4o-mini`).

---

## API 목록

| 도메인 | 엔드포인트 |
|---|---|
| 인증 | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me`, `PATCH /api/auth/me/nickname` |
| 캐릭터 | `POST /api/characters`, `GET /api/characters/me`, `PATCH /api/characters/me/species` |
| 그룹 | `POST /api/groups`, `POST /api/groups/join`, `GET /api/groups/me`, `DELETE /api/groups/me`, `PATCH /api/groups/me/name`, `PATCH /api/groups/me/mission-time` |
| 온보딩 | `POST /api/onboarding`, `GET /api/onboarding/me` |
| 미션 | `POST /api/missions/today`, `GET /api/missions/today`, `POST /api/missions/{id}/verify` |
| 식단 | `POST /api/meals/{slot}/analyze` (multipart), `GET /api/meals/today` |
| 챌린지 | `POST /api/challenges/end`, `GET /api/challenges/summary`, `POST /api/challenges/continue` |
| 파일 | `GET /api/files/{id}` (인증 불필요) |
| 상점 | `GET /api/shop/outfits`, `POST /api/shop/outfits/{id}/buy`, `POST /api/shop/outfits/{id}/wear` |
| 랭킹 | `GET /api/rankings/group`, `GET /api/rankings/global` |

모든 응답은 `{ success, data, error }`(`ApiResponse`)로 감싸집니다.
인증 필요한 API는 `Authorization: Bearer {accessToken}` 헤더 필요 (회원가입/로그인/파일 제외).

---

## 건드릴 때 주의할 것

이미 한 번씩 문제가 됐던 지점들입니다.

**설계 관련**
- **캐릭터 표정은 저장값이 아니라 파생값입니다.** 조회 시점에 `ExpressionPolicy`로 계산합니다.
  `characters.expression` 컬럼은 단건 조회용 캐시일 뿐이니, 여기 값을 믿고 쓰지 마세요.
  규칙은 프론트 `AppContext.jsx`의 `expressionFromRank`와 **반드시 일치**해야 합니다.
- **사진은 DB에 BLOB으로 저장합니다.** 컨테이너 파일시스템은 재배포하면 날아가서 그렇습니다.
  저장 전 `ImageDownscaler`가 긴 변 1024px JPEG로 줄입니다. S3로 옮긴다면 `FileStorageService`만 교체하면 됩니다.
- **`/api/files/**`는 인증 없이 열려 있습니다.** `<img src>`에 토큰 헤더를 못 붙이기 때문이고,
  주소가 추측 불가능한 UUID라 괜찮다고 판단했습니다.
- **동시 요청 방어는 DB 유니크 제약으로 합니다.** (그룹 중복 참여, 미션 세트 중복 생성)
  React StrictMode가 개발 중 effect를 두 번 실행해서 실제로 터졌던 문제입니다.

**환경 관련**
- **`groups`는 MySQL 예약어**라 테이블명이 `study_groups`입니다.
- **Spring 7은 Jackson 3을 쓰는데 OpenAI 클라이언트는 Jackson 2 API로 파싱합니다.**
  그래서 요청 body를 직접 문자열로 직렬화하고 응답도 `String.class`로 받습니다.
  이걸 "깔끔하게" `JsonNode`로 바꾸면 런타임에 터집니다.
- Gradle wrapper 다운로드가 막힌 네트워크에서는 `./gradlew` 대신 시스템 `gradle`을 쓰세요.

**협업 규칙**
- **`.env`는 절대 커밋하지 마세요.** OpenAI 키가 들어 있고 `.gitignore`에 등록돼 있습니다.
- 커밋 메시지에 AI 도구 이름/서명을 넣지 않습니다.

---

## 심사용 데모 계정 (중요)

제출 서류의 "테스트 계정"에 적을 계정입니다. **`DEMO_SEED=true`로 띄우면 서버가 기동할 때마다
자동으로 만들어집니다.**

```
이메일   test@withu.app
비밀번호  withu1234
그룹코드  TEAM33
```

**왜 시더가 필요한가**: 갓 배포한 서버는 그룹이 Day 1이라, 7일 챌린지 결과 화면처럼
"시간이 지나야 보이는" 기능을 심사위원이 볼 방법이 아예 없습니다. 배포일(8/19) 기준
Day 7은 8/25로 제출 마감(8/21)을 넘깁니다. 그래서 **이미 6일을 함께 달려온 4인 그룹**을
미리 만들어 둡니다.

시더가 만드는 것 (`com.withu.demo.DemoDataSeeder`):
- 4개 계정(테스터·민준·서연·수아) + 캐릭터 + 온보딩(목표 각각 다름)
- **Day 7 상태의 4인 그룹** → 로그인 즉시 "7일 챌린지 결과 보기" 버튼이 보임
- 지난 6일치 미션 기록(사람마다 달성률 다름) + 오늘 미션
- 심사 계정의 오늘 미션은 **비워둠** — 심사위원이 직접 사진 인증을 해볼 수 있게
- 동료들은 오늘 일부 완료 → 그룹 피드가 비어 보이지 않음

**기동할 때마다 데모 계정 데이터를 지우고 다시 만듭니다.** 심사위원이 "계속하기"를 눌러
Day 1로 돌아가더라도 재시작하면 복구됩니다. 데모 계정 외 실제 가입자 데이터는 건드리지 않습니다.

> 로컬에서 확인: `DB_PORT=3307 DEMO_SEED=true gradle bootRun`

## 남은 일 (우선순위 순)

1. **배포** — 배포처 미정 (Railway 또는 클라우드타입 검토 중). Dockerfile 아직 없음.
   - 필요한 환경변수: `OPENAI_API_KEY`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`,
     `DB_PASSWORD`, `JWT_SECRET`, **`DEMO_SEED=true`**
   - 배포 후 프론트 `.env`에 백엔드 주소를 넣어 재빌드해야 Netlify 데모가 실서버를 씁니다.
3. **`main` 브랜치 병합** — 대회 심사는 main/master 기준이라 마감(8/21) 전에 반드시 병합.
4. **PRD 대비 남은 소소한 것** — 식단 피드백 텍스트(PRD 6 출력값), 코인 획득 중
   '하루 전체 달성'·'연속 달성' 보너스(PRD 11), 미션 자동 생성 스케줄러(PRD 7).
   그룹 랭킹 기준은 PRD가 '주간 달성률'인데 구현·프론트 모두 '오늘 달성률'이라 기획 확인 필요.


---

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 배포용 (대회 심사 기준 브랜치) |
| `develop` | 개발 통합 |
| `feature/BE-*` | 기능 개발 |
| `hotfix/*` | 긴급 수정 |

현재 작업 브랜치: **`feature/BE-scaffold`**

## 참고 문서

기획 원문은 프론트 저장소에 있습니다: `RISE-client`의 `frontend/docs/PRD.md`, `frontend/DEVLOG.md`
