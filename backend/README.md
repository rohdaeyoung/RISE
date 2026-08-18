# RISE Server (WITHU 백엔드)

WITHU — AI 기반 개인 맞춤 건강 미션과 그룹 동기부여를 결합한 웰니스 그룹 서비스의 백엔드입니다.
멋쟁이사자처럼 대학 14기 중앙 해커톤(ANIMAL LEAGUE) **AAC 트랙** 출품작, 성결대 3팀.

> **이어받는 사람(그리고 AI)에게**: 이 문서 하나만 읽으면 바로 이어서 작업할 수 있게 썼습니다.
> 특히 [지금까지 한 일](#지금까지-한-일)과 [건드릴 때 주의할 것](#건드릴-때-주의할-것)은
> 꼭 읽고 시작하세요. 이미 한 번 밟은 지뢰를 다시 밟지 않기 위한 내용입니다.

---

## 이 문서 읽는 법

위에서부터 **심사·배포에 필요한 것**, 아래로 갈수록 **고칠 때 필요한 것**입니다.
처음 보신다면 1~3번만 읽어도 앱을 띄우고 확인할 수 있습니다.

1. [현재 상태 한 줄 요약](#현재-상태-한-줄-요약)
2. [심사용 데모 계정 (중요)](#심사용-데모-계정-중요)
3. [빠르게 실행하기](#빠르게-실행하기)
4. [프로젝트 구조](#프로젝트-구조)
5. [API 목록](#api-목록)
6. [AI 연동](#ai-연동)
7. [인증 사진 판정 정책](#인증-사진-판정-정책)
8. [배포 (가비아 클라우드 + Vercel)](#배포-가비아-클라우드--vercel)
9. [DB 확인·관리](#db-확인관리)
10. [해커톤에서 받은 서버(가비아 클라우드)로 옮긴 기록 — 2026-08-18 완료](#해커톤에서-받은-서버가비아-클라우드로-옮긴-기록--2026-08-18-완료)
11. [보안 — 처리한 것과 남은 것](#보안--처리한-것과-남은-것)
12. [OpenAI 사용량 한도 — 지금 가장 급한 문제](#openai-사용량-한도--지금-가장-급한-문제)
13. [건드릴 때 주의할 것](#건드릴-때-주의할-것)
14. [지금까지 한 일](#지금까지-한-일)
15. [프론트에서 해야 할 일](#프론트에서-해야-할-일)
16. [남은 일 (우선순위 순)](#남은-일-우선순위-순)
17. [브랜치 전략](#브랜치-전략)
18. [참고 문서](#참고-문서)

---

## 현재 상태 한 줄 요약

**배포까지 끝났습니다. 아래 주소로 지금 바로 동작합니다.**

| 무엇 | 주소 |
|---|---|
| **웹앱** (팀원·심사위원에게 줄 주소) | https://rise-client-rohdaeyoungs-projects.vercel.app |
| API 문서 (Swagger) | https://1-201-117-9.nip.io/swagger-ui.html |
| 백엔드 서버 | `1-201-117-9.nip.io` — 화면이 없는 API 서버라 브라우저로 열면 `AUTH_003` JSON에 **401**이 뜨는 것이 정상입니다 |

**테스트 계정: `test@withu.app` / `withu1234`** (그룹 코드 `TEAM33`, Day 7 상태)

프론트는 Vercel, 백엔드와 MySQL은 해커톤에서 제공받은 가비아 클라우드 서버에 올라가 있습니다.

백엔드 주소가 IP처럼 생긴 이유는, 도메인을 사지 않고 HTTPS를 붙이기 위해서입니다. `nip.io`는
`1-201-117-9.nip.io` 같은 이름을 그대로 `1.201.117.9`로 풀어주는 무료 DNS라, 이 이름으로
Let's Encrypt 인증서를 받을 수 있습니다. 프론트가 https라 백엔드도 https여야 하는데
(브라우저가 https 페이지에서 http 요청을 막습니다) IP만으로는 인증서를 받을 수 없어서 쓴 방법입니다.

### ⚠️ 배포 전 반드시 해야 할 것 (안 하면 서버가 안 뜨거나 뚫립니다)

| 환경변수 | 안 하면 |
|---|---|
| `JWT_SECRET` (48자 이상) | **서버가 뜨지 않습니다.** 일부러 그렇게 막아뒀습니다 — 아래 설명 참고 |
| `SPRING_PROFILES_ACTIVE=prod` | 로컬 DB 설정으로 뜨려다 실패 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | DB 연결 실패 |
| `OPENAI_API_KEY` | 서버는 뜨지만 AI가 mock으로 동작(고정 문구 미션, 랜덤 판정) |
| `DEMO_SEED=true` | **심사용 데모 계정이 안 만들어집니다** |
| `CORS_ALLOWED_ORIGINS` | 안 넣으면 **모든 출처 허용(`*`)** — 아무 사이트나 우리 API를 부를 수 있습니다. 넣을 값은 "CORS 설정값" 참고 |

`application.yml`의 JWT 기본 키는 이 저장소에 그대로 적혀 있습니다. 그 키로 서명하면
저장소를 본 누구나 원하는 userId의 토큰을 위조해 아무 계정에나 접근할 수 있습니다.
그래서 **local 프로필이 아닌데 기본 키를 쓰면 서버가 아예 뜨지 않게** 막아뒀습니다
(`JwtTokenProvider.requireSecureSecret`). 조용히 뚫린 채 도는 것보다 낫습니다.

```bash
JWT_SECRET=$(openssl rand -hex 32)   # 64자 — 이 값을 배포 환경변수에 넣으세요
```

### 프론트(`RISE-client`)와의 관계

프론트도 `main`에 push되어 Vercel에 배포돼 있습니다. 프론트는 `VITE_API_BASE_URL` 환경변수 하나로
이 백엔드를 바라봅니다. 그 값이 비어 있으면 프론트가 mock 모드(브라우저 안에서만 도는 가짜 데이터)로
동작하므로, **배포 환경에 이 값이 반드시 설정돼 있어야 합니다.**

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
Day 7은 8/25로 제출 마감(8/21)을 넘깁니다. 그래서 **이미 6일을 함께 달려온 그룹**을
미리 만들어 둡니다.

시더가 만드는 것 (`com.withu.demo.DemoDataSeeder`):
- 3개 계정(테스터·민준·서연) + 캐릭터 + 온보딩(목표 각각 다름)
- **Day 7 상태의 그룹** → 로그인 즉시 "7일 챌린지 결과 보기" 버튼이 보임
- **정원 4명 중 한 자리는 비워 둠** → 부스에서 앱을 본 사람이 직접 가입해
  `TEAM33`으로 바로 합류할 수 있음 (넷을 다 채우면 "인원이 가득 찼습니다"만 보게 됨)
- **오늘자 그룹 피드 반응·댓글** → 피드 탭이 빈 화면이 되지 않음
- 지난 6일치 미션 기록(사람마다 달성률 다름) + 오늘 미션
- 심사 계정의 오늘 미션은 **비워둠** — 심사위원이 직접 사진 인증을 해볼 수 있게
- 동료들은 오늘 일부 완료 → 그룹 피드가 비어 보이지 않음

**기동할 때, 그리고 날짜가 바뀔 때마다 데모 계정 데이터를 지우고 다시 만듭니다.**
기동 시에만 만들면 배포일과 심사일이 다를 때(8/19 배포 → 8/21 심사) 동료 계정은 아무도
앱을 켜지 않으므로 그날 미션이 전부 미완료로 남고, 심사위원이 보는 그룹 피드가
**전원 0%·슬픈 표정·사진 한 장 없는** 상태가 됩니다. 심사위원이 "계속하기"나 "방 나가기"를
눌러 상태가 망가져도 재시작하면 복구됩니다(실제로 눌러서 확인함).
데모 계정 외 실제 가입자 데이터는 어떤 경로로도 건드리지 않습니다.

> 로컬에서 확인: `DB_PORT=3307 DEMO_SEED=true gradle bootRun`

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

### 프론트와 백엔드를 연결해 실제 웹앱으로 띄우기

> 그냥 써보기만 할 거라면 배포된 https://rise-client-rohdaeyoungs-projects.vercel.app 로 들어가면 됩니다.
> 아래는 **코드를 고치면서 개발할 때** 각자 컴퓨터에 띄우는 방법입니다.
> `localhost` 주소는 "그 명령을 실행한 컴퓨터"를 가리켜서 남에게 보내도 열리지 않습니다.

띄우면 이렇게 물립니다.

```
브라우저 → localhost:5173 (프론트 화면)
              ↓ REST 호출
          localhost:8080 (이 백엔드)
              ↓                ↘
          MySQL (데이터)      OpenAI API (미션 생성·식단 분석)
```

**터미널 1 — 백엔드**

```bash
cd RISE-server
DB_PORT=3307 DEMO_SEED=true gradle bootRun
```

`DB_PORT`는 MySQL 포트에 맞추세요(기본 3306). `DEMO_SEED=true`를 켜면 7일차 데모 그룹이
자동으로 만들어져 로그인하자마자 전 기능을 볼 수 있습니다.

**터미널 2 — 프론트**

```bash
cd RISE-client/frontend
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.local
npm install && npm run dev
```

**브라우저에서 `http://localhost:5173` 접속 → `test@withu.app` / `withu1234` 로 로그인.**

로그인 직후 MY 화면에 오늘의 미션과 달성률이 뜨고, 그룹 탭에서 Day 7 피드와
7일 챌린지 결과까지 확인할 수 있습니다.

`VITE_API_BASE_URL`을 **지우면 프론트가 mock 모드로 돌아갑니다.** 백엔드 없이 프론트만
데모할 수 있도록 일부러 분리해 둔 구조이니, 연동한다고 mock 코드를 지우지 마세요.

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
  ├── feed/           그룹 피드 반응·댓글
  ├── ranking/        그룹 내 / 전체 랭킹
  └── ai/             AI 포트(인터페이스) + openai 구현체 + mock 구현체
```

## API 목록

| 도메인 | 엔드포인트 |
|---|---|
| 인증 | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me`, `PATCH /api/auth/me/nickname` |
| 캐릭터 | `POST /api/characters`, `GET /api/characters/me`, `PATCH /api/characters/me/species` |
| 그룹 | `POST /api/groups`, `POST /api/groups/join`, `GET /api/groups/me`, `GET /api/groups/me/members/{userId}`, `DELETE /api/groups/me`, `PATCH /api/groups/me/name`, `PATCH /api/groups/me/mission-time` |
| 온보딩 | `POST /api/onboarding`, `GET /api/onboarding/me` |
| 미션 | `POST /api/missions/today`, `GET /api/missions/today`, `POST /api/missions/{id}/verify` (multipart, 사진 필수) |
| 식단 | `POST /api/meals/{slot}/analyze` (multipart), `GET /api/meals/today` |
| 챌린지 | `POST /api/challenges/end`, `GET /api/challenges/summary`, `POST /api/challenges/continue` |
| 파일 | `GET /api/files/{id}` (인증 불필요) |
| 탈퇴 | `DELETE /api/auth/me` |
| 상점 | `GET /api/shop/outfits`, `POST /api/shop/outfits/{id}/buy`, `POST /api/shop/outfits/{id}/wear` |
| 피드 | `GET /api/feed`, `POST /api/feed/reactions`, `POST /api/feed/comments` |
| 랭킹 | `GET /api/rankings/group`, `GET /api/rankings/global` |

모든 응답은 `{ success, data, error }`(`ApiResponse`)로 감싸집니다.
인증 필요한 API는 `Authorization: Bearer {accessToken}` 헤더 필요 (회원가입/로그인/파일 제외).

---

## AI 연동

`ai` 패키지의 `MissionAiClient`, `MealVisionAiClient`, `LifestyleVisionAiClient` 인터페이스가
유일한 AI 연동 지점입니다.

- `.env`에 `OPENAI_API_KEY`가 **있으면** → `ai/openai`의 실제 구현체가 `@Primary`로 등록
- **없으면** → `ai/mock`의 mock 구현체가 동작

전환은 설정만으로 이뤄지고 `MissionService` / `MealService` 코드는 건드릴 필요가 없습니다.
모델은 `application.yml`의 `openai.mission-model` / `openai.vision-model`에서 변경 (기본 `gpt-4o-mini`).

---

## 인증 사진 판정 정책

사진 인증이 이 서비스의 핵심이라 판정 기준을 한곳에 모아둡니다.

| 무엇 | 어디서 | 무엇을 묻는가 |
|---|---|---|
| 식단 인증 | `OpenAiMealVisionClient` | 이 음식이 오늘의 식단 미션·건강 목표에 맞는가 |
| 생활습관 인증 | `OpenAiLifestyleVisionClient` | 그 행동을 하는 상황에서 찍을 법한 사진인가 |

둘을 나눈 이유는 묻는 것이 다르기 때문입니다. 걷기 인증에 걷는 자기 모습을 찍을 수는 없으므로
**바깥 풍경이면 인정**하고, 같은 사진이라도 식단 미션에는 통하지 않습니다.

판정 전에 서버가 먼저 걸러내는 것:

1. 이미지가 아닌 파일 → `FILE_003`
2. 이미 인증에 쓰인 사진(원본 SHA-256 대조) → `FILE_004`

그다음 AI가 봅니다. 미달성이면 식단은 `achieved: false`로 기록되고, 생활습관은 `MISSION_004`로
거절되어 완료 처리되지 않습니다. AI가 죽었을 때는 통과시키지 않고 재시도를 안내합니다
(`MEAL_002` / `MISSION_005`) — 여기서 봐주면 장애 중에 아무 사진이나 인증되기 때문입니다.

**부정 사용을 완전히 막을 수는 없습니다.** 브라우저가 보내는 이미지는 무엇이든 위조할 수 있습니다.

| 수법 | 막히는가 |
|---|---|
| 같은 사진 반복 사용 | 막힘 (SHA-256 대조) |
| 명백한 화면 캡처 | 대체로 막힘 (AI 판별) |
| 매번 새 이미지를 검색해서 다운로드 | **못 막음** |
| 남이 찍은 실제 사진을 받아서 올림 | **못 막음** |

더 조이려면 EXIF 촬영 시각을 검사해 "오늘 찍은 사진만" 허용하는 방법이 있습니다. 다만 메신저로 받은
사진은 EXIF가 지워지고 갤러리의 어제 사진도 거절되므로, **정상 사진이 막히는 위험**이 더 커서
지금은 넣지 않았습니다. 필요하면 환경변수로 켜고 끄는 형태로 추가하는 것이 안전합니다.

---

## 배포 (가비아 클라우드 + Vercel)

**순서가 중요합니다.** 백엔드와 프론트가 서로의 주소를 알아야 하는데, 주소는 배포해야 생깁니다.
그래서 백엔드를 먼저 띄우고 → 그 주소를 프론트에 넣고 → 프론트 주소를 다시 백엔드 CORS에 넣습니다.

### 1단계. 서버 준비 (Rocky Linux 8)

보안그룹에서 **22번과 443번**이 열려 있어야 합니다. 80번은 없어도 됩니다(아래 3단계 참고).

```bash
sudo dnf install -y git java-21-openjdk-devel mysql-server
sudo systemctl enable --now mysqld
```

DB와 전용 계정을 만듭니다. **비밀번호는 서버에서 만들어 쓰세요** — 채팅·문서에 남기지 않기 위해서입니다.

```bash
DBPASS=$(openssl rand -base64 24 | tr -d '/+=' | head -c 28)
sudo mysql <<SQL
CREATE DATABASE withu CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'withu'@'localhost' IDENTIFIED BY '${DBPASS}';
GRANT ALL PRIVILEGES ON withu.* TO 'withu'@'localhost';
SQL
```

### 2단계. 백엔드 배포

레포에서 직접 받아 빌드합니다. 이렇게 하면 제출한 저장소와 실제 배포가 같은 코드임이 분명해집니다.

```bash
sudo mkdir -p /opt/withu && sudo chown $USER /opt/withu
git clone --depth 1 https://github.com/rohdaeyoung/RISE.git /opt/withu/src
cd /opt/withu/src/backend && ./gradlew clean bootJar -x test --no-daemon
cp build/libs/withu-server-0.0.1-SNAPSHOT.jar /opt/withu/app.jar
```

환경변수는 **root만 읽는 파일**에 둡니다(`/etc/withu/withu.env`, 권한 600).

```
SPRING_PROFILES_ACTIVE=prod
PORT=8080
TZ=Asia/Seoul
DB_URL=jdbc:mysql://127.0.0.1:3306/withu?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=withu
DB_PASSWORD=<위에서 만든 값>
DDL_AUTO=update
JWT_SECRET=<openssl rand -base64 48>
OPENAI_API_KEY=<대회에서 받은 키>
DEMO_SEED=true
CORS_ALLOWED_ORIGINS=<4단계에서 채움>
```

systemd에 등록하면 **서버가 재부팅돼도, 프로세스가 죽어도 알아서 다시 뜹니다.**
`/etc/systemd/system/withu.service`:

```ini
[Unit]
Description=WITHU Spring Boot backend
After=network-online.target mysqld.service
Requires=mysqld.service

[Service]
User=rocky
EnvironmentFile=/etc/withu/withu.env
ExecStart=/usr/bin/java -Xms256m -Xmx1024m -jar /opt/withu/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now withu
```

### 3단계. HTTPS 붙이기 (Caddy)

프론트가 https라 백엔드도 https여야 합니다. 도메인을 사지 않고 `nip.io`로 해결했습니다.

```bash
sudo dnf install -y 'dnf-command(copr)'
sudo dnf copr enable -y @caddy/caddy && sudo dnf install -y caddy
```

`/etc/caddy/Caddyfile`:

```
{
	auto_https disable_redirects
	email <연락 가능한 메일>
}

1-201-117-9.nip.io {
	tls {
		issuer acme {
			disable_http_challenge
		}
	}
	encode gzip
	reverse_proxy 127.0.0.1:8080
}
```

`disable_http_challenge`가 **80번 없이 443만으로 인증서를 받게 하는 설정**입니다(TLS-ALPN-01).
80을 열 수 있으면 이 줄과 `auto_https disable_redirects`를 지우는 편이 낫습니다.

```bash
sudo systemctl enable --now caddy
```

`https://<이름>/swagger-ui.html`이 열리면 성공입니다.

### 4단계. Vercel에 프론트 배포

1. Vercel에서 **Add New → Project → `RISE`** 선택 (브랜치 `main`)
2. **Root Directory를 `frontend`로 지정** — 이걸 빠뜨리면 빌드가 실패합니다.
3. **Environment Variables**에 백엔드 주소를 넣습니다. **끝에 슬래시를 붙이지 마세요**
   (`/api/...`를 이어 붙이므로 `//api/...`가 되어 전부 404 납니다).

```
VITE_API_BASE_URL = https://1-201-117-9.nip.io
```

> 이 값은 **빌드 시점에 코드에 박히므로**, 나중에 바꾸면 반드시 재배포(Redeploy)해야 합니다.
> 재배포할 때 "Use existing Build Cache"는 끄세요. 켜두면 옛 값이 박힌 캐시를 그대로 씁니다.

### 5단계. 백엔드 CORS에 프론트 주소 등록

`/etc/withu/withu.env`를 고치고 `sudo systemctl restart withu`.

```
CORS_ALLOWED_ORIGINS=https://rise-client-rohdaeyoungs-projects.vercel.app,https://rise-client-*-rohdaeyoungs-projects.vercel.app,http://localhost:5173
```

이걸 안 넣으면 **모든 출처가 허용된 채로 돌아갑니다**(동작은 하지만 열려 있음).
실제로 서버를 옮긴 직후 `*`인 상태로 며칠 둘 뻔했고, `evil.example.com`으로 요청해 보고서야
발견했습니다. 옮길 때마다 위 "옮긴 뒤 확인한 것"의 2·3번을 꼭 돌리세요.

### 6단계. 확인

프론트 주소에 접속해 `test@withu.app` / `withu1234` 로 로그인 →
MY 화면에 미션이 뜨고, 그룹 탭에서 Day 7 결과가 보이면 연동 성공입니다.

### 예전 배포(Railway)에 대하여

2026-08-13 ~ 08-18에는 Railway + Vercel로 운영했습니다. 가비아 서버로 옮긴 뒤에도
**만일을 대비해 2026-08-25까지 Railway를 켜둡니다.** 새 서버에 문제가 생기면 Vercel의
`VITE_API_BASE_URL`만 되돌리고 재배포하면 즉시 복구됩니다. 저장소 루트의 `Dockerfile`은
그때 쓰던 것이고, 지금도 유효합니다.

### 로컬에서 배포 이미지 검증하는 법

Docker로 실제 배포와 같은 조건을 재현할 수 있습니다(빈 DB 기준).

```bash
docker build -t withu-server .
docker run -p 18080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/withu_deploy?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul" \
  -e DB_USERNAME=root -e DB_PASSWORD= \
  -e JWT_SECRET="$(openssl rand -hex 32)" \
  -e DEMO_SEED=true \
  withu-server
```

> 이미 로컬에서 `gradle bootRun`이 같은 DB를 쓰고 있으면 스키마 갱신 단계에서 서로 락을 물고
> 멈춥니다. 검증할 때는 로컬 서버를 끄거나 위처럼 별도 DB를 쓰세요.

---

## DB 확인·관리

배포 서버의 MySQL은 **외부에 열려 있지 않습니다.** 3306 포트를 보안그룹에 열지 않았고,
DB 계정도 `withu@localhost`라 서버 밖에서는 아예 접속되지 않습니다. 그래서 확인·관리는
**SSH로 서버에 들어가서** 합니다. DB 접속 비밀번호는 `/etc/withu/withu.env`에 있고
이 파일은 root만 읽을 수 있습니다(권한 600).

```bash
ssh -i <키파일>.pem rocky@1.201.117.9
```

### 지금 누가 가입해 있는지 보기

```bash
sudo mysql withu -e "
SELECT u.id, u.email, u.nickname, DATE(u.created_at) AS 가입일,
       (SELECT COUNT(*) FROM missions m WHERE m.user_id = u.id) AS 미션수
FROM users u ORDER BY u.id;"
```

데모 계정(`test@withu.app`, `mate1@`, `mate2@`)만 보이면 정상입니다. 그 외 계정은
실제로 가입한 사람이므로 **함부로 지우지 마세요.**

### 그룹과 진행 상황

```bash
sudo mysql withu -e "
SELECT sg.id, sg.code, sg.name, DATE(sg.started_at) AS 시작일,
       (SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = sg.id) AS 인원
FROM study_groups sg ORDER BY sg.id;"
```

### 데이터가 깨지지 않았는지 (고아 행 검사)

주인이 사라진 행이 남아 있으면 랭킹에 유령이 뜨거나 빈 방이 보입니다. 0이어야 정상입니다.

```bash
sudo mysql withu -e "
SELECT '고아 group_members' AS 검사, COUNT(*) AS 건수 FROM group_members WHERE user_id NOT IN (SELECT id FROM users)
UNION ALL SELECT '고아 missions',  COUNT(*) FROM missions   WHERE user_id NOT IN (SELECT id FROM users)
UNION ALL SELECT '고아 meals',     COUNT(*) FROM meals      WHERE user_id NOT IN (SELECT id FROM users)
UNION ALL SELECT '고아 characters',COUNT(*) FROM characters WHERE user_id NOT IN (SELECT id FROM users)
UNION ALL SELECT '빈 그룹',        COUNT(*) FROM study_groups WHERE id NOT IN (SELECT DISTINCT group_id FROM group_members)
UNION ALL SELECT '미참조 사진',    COUNT(*) FROM stored_files sf
   WHERE sf.id NOT IN (SELECT SUBSTRING_INDEX(photo_url,'/',-1) FROM missions WHERE photo_url IS NOT NULL)
     AND sf.id NOT IN (SELECT SUBSTRING_INDEX(photo_url,'/',-1) FROM meals    WHERE photo_url IS NOT NULL);"
```

### 사진이 얼마나 쌓였는지

사진은 파일이 아니라 DB에 BLOB으로 들어갑니다. 용량이 늘면 여기서 봅니다.

```bash
sudo mysql withu -e "
SELECT COUNT(*) AS 장수, ROUND(SUM(LENGTH(data))/1024/1024, 2) AS 총MB FROM stored_files;"
```

### 백업

BLOB이 들어 있어 덤프가 커질 수 있습니다. 심사 전후로 한 번씩 떠두면 안전합니다.

```bash
sudo mysqldump --single-transaction --routines withu | gzip > ~/withu-$(date +%Y%m%d-%H%M).sql.gz
ls -lh ~/withu-*.sql.gz
```

되돌릴 때는 이렇게 합니다. **덮어쓰기이므로 지금 데이터가 사라집니다.**

```bash
gunzip -c ~/withu-<날짜>.sql.gz | sudo mysql withu
sudo systemctl restart withu
```

### 검증하며 만든 계정 정리

정리 스크립트가 [`scripts/cleanup-test-accounts.sql`](scripts/cleanup-test-accounts.sql)에 있습니다.
**남길 계정 목록(`@keep`)을 직접 채운 뒤** 실행하세요. 되돌릴 수 없습니다.

가능하면 앱의 탈퇴 API를 쓰는 편이 낫습니다. 사진·피드까지 함께 지워지고, SQL을 잘못 짜서
남의 데이터를 건드릴 위험도 없습니다.

```bash
curl -X DELETE https://1-201-117-9.nip.io/api/auth/me -H "Authorization: Bearer <그 계정 토큰>"
```

### 서비스·DB 상태 한 번에 보기

```bash
systemctl is-active withu mysqld caddy      # 셋 다 active 여야 정상
sudo journalctl -u withu -n 50 --no-pager   # 백엔드 로그
sudo journalctl -u withu -p warning         # 경고·에러만
```

> **데모 계정은 서버가 뜰 때와 날짜가 바뀔 때 자동으로 지워졌다 다시 만들어집니다.**
> 그래서 데모 계정의 `id`는 계속 바뀝니다. id로 뭔가를 기억해 두지 마세요.
> 실제 가입자 데이터는 이 과정에서 건드리지 않습니다.

## 해커톤에서 받은 서버(가비아 클라우드)로 옮긴 기록 — 2026-08-18 완료

**코드는 한 줄도 안 고쳤습니다.** 주소·포트·시간대·DB가 전부 환경변수로 빠져 있고,
사진도 파일이 아니라 DB에 저장하므로 서버가 바뀌어도 따라갑니다.

옮긴 뒤 구성은 이렇습니다.

```
[브라우저] ──https──> [Vercel 프론트]
                          │ https
                          ▼
                    [가비아 서버 1.201.117.9]
                      ├─ Caddy :443        Let's Encrypt 인증서, 자동 갱신
                      ├─ Spring Boot :8080  systemd(withu.service), 죽으면 자동 재시작
                      └─ MySQL 8 :3306      같은 서버 안, 외부에 열지 않음
```

주소가 `1-201-117-9.nip.io`인 이유는 도메인을 사지 않고 HTTPS를 붙이기 위해서입니다.
프론트가 https라 백엔드도 https여야 하는데(브라우저가 https 페이지에서 http 요청을 막습니다)
IP만으로는 인증서를 받을 수 없습니다. `nip.io`가 그 이름을 그대로 `1.201.117.9`로 풀어주므로
이 이름으로 인증서를 받았습니다.

**보안그룹에 80번을 열 수 없어서** 흔히 쓰는 HTTP-01 방식 대신 443만 쓰는 **TLS-ALPN-01**로
발급받았습니다. Caddyfile에서 `disable_http_challenge`가 그 설정입니다. 80을 열 수 있는
환경으로 옮긴다면 이 줄을 지우는 편이 낫습니다(http로 들어온 사람을 https로 넘겨줍니다).

### 옮기기 전에 확인할 것

| 항목 | 어떻게 처리되나 |
|---|---|
| 포트 | `PORT` 환경변수를 주면 그 포트에 붙습니다. 없으면 8080 |
| 시간대 | **코드에서 한국 시간으로 고정**했습니다. 서버가 UTC여도 그대로 동작 |
| DB | `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`. 빈 DB면 테이블을 자동 생성(`DDL_AUTO=update`) |
| 업로드 사진 | DB에 저장하므로 서버를 옮겨도 남습니다 (디스크에 안 씁니다) |
| 프론트 주소 | `CORS_ALLOWED_ORIGINS`에 새 프론트 도메인 |
| HTTPS 프록시 뒤 | `forward-headers-strategy: framework`로 이미 처리 |

### 반드시 넣어야 하는 환경변수

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://호스트:포트/DB이름?useUnicode=true&characterEncoding=utf8
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...            # 48자 이상. 없으면 서버가 아예 뜨지 않습니다(의도된 동작)
OPENAI_API_KEY=...        # 대회에서 받은 키
CORS_ALLOWED_ORIGINS=...  # 위 "CORS 설정값" 참고. 주소 하나만 넣으면 미리보기·로컬이 막힙니다
DEMO_SEED=true            # 심사용 데모 계정이 필요할 때만
```

`JWT_ACCESS_VALIDITY_MS`는 넣지 않아도 됩니다. 기본값이 7일(`604800000`)이라 챌린지 한 사이클
동안은 재로그인 없이 쓸 수 있습니다. 더 짧게 잡으면 사이클 도중에 로그인이 끊깁니다.

### 옮긴 뒤 확인한 것 (또 옮기게 되면 이대로 다시 하세요)

```bash
# 1. 서버가 살아 있는가 (401이 정상 — 화면 없는 API 서버라 인증 없이는 거절)
curl -o /dev/null -w "%{http_code}\n" https://1-201-117-9.nip.io/api/auth/me

# 2. CORS가 좁혀졌는가 — 403만 나오고 허용 헤더는 안 보여야 정상
curl -s -D- -o /dev/null -X OPTIONS https://1-201-117-9.nip.io/api/auth/login \
  -H "Origin: https://evil.example.com" \
  -H "Access-Control-Request-Method: POST" | grep -i "^HTTP/\|access-control-allow-origin"

# 3. 진짜 프론트는 통과하는가 — 2번만 보고 끝내면 프론트까지 막아놓고 모를 수 있다
curl -s -D- -o /dev/null -X OPTIONS https://1-201-117-9.nip.io/api/auth/login \
  -H "Origin: https://rise-client-rohdaeyoungs-projects.vercel.app" \
  -H "Access-Control-Request-Method: POST" | grep -i "access-control-allow-origin"

# 4. 로그인이 실제로 되는가
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://1-201-117-9.nip.io/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@withu.app","password":"withu1234"}'
```

2026-08-18 실측 결과입니다.

| 확인 | 결과 |
|---|---|
| 인증 필요 엔드포인트 9종 | 전부 200 |
| `evil.example.com` | **403**, 허용 헤더 없음 |
| 실서비스·미리보기·`localhost:5173` | 전부 허용 |
| Swagger | 200 |
| 인증서 | Let's Encrypt, 2026-11-16까지 |

**시간대 확인은 굳이 계정을 만들지 마세요.** 예전 절차는 `tz-check@withu.app`으로 가입해
보라고 했는데, 그렇게 만든 계정이 전체 랭킹에 그대로 남아 나중에 지워야 했습니다.
서버에서 `timedatectl`로 KST인지 보는 것으로 충분합니다.

프론트는 Vercel 환경변수 `VITE_API_BASE_URL`을 새 주소로 바꾸고 **재배포**하면 끝입니다.
Vite는 빌드할 때 이 값을 코드에 박기 때문에, 환경변수만 바꾸고 재배포를 안 하면 아무것도
안 바뀝니다. 재배포할 때 "Use existing Build Cache"는 **꺼야** 합니다.

### 시간대를 왜 코드에서 고정했나 (건드리지 마세요)

`WithuServerApplication.main()`이 `SpringApplication.run()` **전에** 시간대를 한국으로 정합니다.
이 앱은 날짜에 크게 기댑니다 — 오늘의 미션, 미션 도착 시각, 끼니, 7일 사이클, 연속 인증.
`LocalDate.now()`는 서버의 기본 시간대를 따르는데 클라우드 서버는 대부분 UTC입니다.

고정하지 않으면 서버를 옮기는 것만으로 이렇게 어긋납니다.

```
UTC 서버에서 "오늘"이 바뀌는 시점  → 한국 시간 오전 9시
미션 시각을 오전 9시로 설정하면     → 실제로는 오후 6시에 도착
밤 10시에 한 인증                 → 다음 날 기록으로 저장
```

**`@PostConstruct`로 늦게 부르면 안 됩니다.** 그 사이에 만들어진 DB 커넥션 풀이 옛 시간대를
붙잡습니다. 실제로 `TZ=UTC`로 띄워서 확인했더니 `created_at`은 한국 시간으로 맞는데
미션 날짜만 하루 전으로 저장됐습니다 — MySQL 드라이버가 커넥션의 시간대로 날짜를 변환하기
때문입니다. `main()`에서 먼저 부르도록 고친 뒤 `TZ=UTC`로 다시 띄워 한국 날짜로 저장되는 것을
확인했습니다.

---

## 보안 — 처리한 것과 남은 것

### 처리 완료

| 항목 | 내용 |
|---|---|
| JWT 기본 키 배포 방지 | local 프로필이 아닌데 공개된 기본 키를 쓰면 **서버가 기동 실패**. 키 길이(48바이트)도 함께 검사 |
| **CORS 좁히기 (2026-08-15 배포 적용)** | `CORS_ALLOWED_ORIGINS`를 넣어 실제로 닫았습니다. 2026-08-18 가비아 서버로 옮길 때 이 값을 빠뜨려 잠시 다시 열렸다가, `evil.example.com`으로 확인해 되잡았습니다. 그 전까지는 기본값 `*`라 아무 사이트나 API를 부를 수 있었습니다 (아래 "CORS 설정값" 참고) |
| 비밀번호 | BCrypt 해시 저장, 가입 시 8자 이상 강제 |
| 소유권 검사 | 남의 미션 인증 시도 → `COMMON_003 권한 없음` (실제 다른 계정 토큰으로 확인) |
| 인증 필수 | 토큰 없이 API 호출 → 403 |
| 신체 정보 비공개 | 그룹원 프로필 응답에 키·몸무게·나이·성별 없음 (PRD 12) |
| AI 비용 폭주 방지 | 식단 분석은 슬롯당 하루 1회(중복 시 `MEAL_001`), 미션 생성은 하루 1세트로 DB 유니크 제약이 막음 |
| 업로드 파일 검증 | 이미지가 아니면 AI를 부르기 전에 `FILE_003`(400)으로 차단 — 예전엔 OpenAI까지 보내고 500이 났음 |
| **AI 장애 대응** | OpenAI가 죽어도 미션은 고정 풀로 계속 생성(앱이 멈추지 않음), 식단 분석은 `MEAL_002`(503)로 재시도 안내 |

### 남은 것 (배포 담당자 판단 필요)

1. **`/api/files/**`가 공개 경로입니다.** `<img>` 태그로 직접 불러와야 해서 토큰 헤더를 붙일 수
   없기 때문입니다. 주소가 임의의 UUID라 추측은 어렵지만, **URL을 아는 사람은 누구나 그 인증
   사진을 볼 수 있습니다.** MVP 범위에서는 수용 가능하다고 판단했으나, 실서비스라면 서명된
   짧은 만료 URL이 필요합니다.
2. **Swagger UI(`/swagger-ui.html`)가 배포 환경에서도 열립니다.** 심사위원이 API를 보기엔
   좋지만, 대회가 끝나면 prod에서 꺼야 합니다.
3. **회원가입이 열려 있어 무제한 가입이 가능합니다.** 가입 자체는 AI를 호출하지 않지만,
   대량 가입 후 각자 미션을 받으면 OpenAI 비용이 늘 수 있습니다. 심사 기간에는 문제없는 수준.
4. **접근 토큰 만료가 24시간이고 리프레시 토큰이 없습니다.** 심사에는 충분하지만 실서비스라면
   짧은 만료 + 리프레시 구조가 필요합니다.
5. **데모 계정 비밀번호가 공개돼 있습니다.** 의도된 것이지만, 이 계정은 실제 API를 그대로
   쓸 수 있으므로 대회 종료 후 `DEMO_SEED=false`로 끄세요.

### CORS 설정값 (서버를 옮기면 반드시 다시 넣어야 합니다)

`CORS_ALLOWED_ORIGINS`는 **코드가 아니라 서버의 환경변수**에 들어 있습니다(`/etc/withu/withu.env`).
그래서 저장소만 옮기면 따라오지 않습니다. 안 넣으면 기본값 `*`로 돌아가 다시 열립니다.

현재 서버에 넣은 값입니다. 콤마로 구분하고 **띄어쓰기를 넣지 마세요.**

```
https://rise-client-rohdaeyoungs-projects.vercel.app,https://rise-client-*-rohdaeyoungs-projects.vercel.app,http://localhost:5173
```

세 개를 다 넣는 이유가 있습니다. 실서비스 주소 하나만 넣으면 나머지가 막힙니다.

| 넣는 값 | 없으면 생기는 일 |
|---|---|
| 실서비스 주소 | 배포된 앱이 서버를 못 부름 |
| `https://*-rohdaeyoungs-projects.vercel.app` | **Vercel 미리보기 배포가 전부 막힘** (PR 올릴 때마다 앱이 안 돎) |
| `http://localhost:5173` | **팀원 로컬 개발이 막힘** |

`setAllowedOriginPatterns`를 쓰므로 `*` 와일드카드가 동작합니다(`SecurityConfig`). 배포 후
아래로 확인하세요 — 공격자 주소는 **403이고 `access-control-allow-origin` 줄이 없어야** 정상입니다.

```bash
curl -s -D- -o /dev/null -X OPTIONS https://1-201-117-9.nip.io/api/auth/login \
  -H "Origin: https://evil.example.com" -H "Access-Control-Request-Method: POST" \
  | grep -i "^HTTP/\|access-control-allow-origin"
```

실제 배포에서 확인한 결과입니다.

| 부르는 주소 | 결과 |
|---|---|
| 실서비스 프론트 | 200 통과 |
| Vercel 미리보기 (`...-abc123-...`) | 200 통과 |
| `http://localhost:5173` | 200 통과 |
| `https://evil.example.com` | **403 차단** |
| `https://rise-client-....vercel.app.evil.com` (주소 흉내내기) | **403 차단** |

> 값에 오타가 있으면 정상 프론트까지 막혀 앱 전체가 안 돕니다. 재배포 후 앱을 한 번 열어보세요.
> 문제가 생기면 그 변수를 지우고 재배포하면 원래대로 돌아옵니다.
>
> 그리고 **재배포 직후 30초~1분은 502가 납니다.** 서버가 새 버전으로 갈아타는 구간이라 고장이
> 아닙니다. 다만 **심사 직전에는 재배포하지 마세요** — 하필 그 1분과 겹치면 앱이 안 열립니다.

---

## OpenAI 사용량 한도 — 지금 가장 급한 문제

무료 등급은 **모델별로 하루 요청 수(RPD)가 정해져 있고, gpt-4o-mini는 50회**입니다.
사진 인증 1회 = 요청 1회, 미션 세트 생성 1회 = 요청 1회이므로 **그룹 하나가 이틀도 못 씁니다.**

한도를 넘기면:

| 기능 | 한도 초과 시 |
|---|---|
| 미션 생성 | 고정 풀로 대체 — 앱은 돌아가지만 **개인 맞춤이 아닌 정해진 문구** |
| 식단 사진 분석 | `MEAL_002` 503 — 인증 불가 |
| 생활습관 사진 인증 | `MISSION_005` 503 — 인증 불가 |

한도는 하루 지나면 다시 풀립니다. 다만 50회는 심사 당일을 못 버팁니다.

**제대로 된 해결**: OpenAI 계정에 결제 수단 등록, 또는 해커톤에서 주는 키로 교체.
서버의 `/etc/withu/withu.env`에서 `OPENAI_API_KEY`만 바꾸고 `systemctl restart withu` 하면 됩니다.

### 한도가 차면 서버가 알아서 다음 모델로 넘어갑니다

RPD는 **모델마다 따로** 셉니다. gpt-4o-mini가 막혀도 다른 모델은 살아 있습니다.
**같은 키·같은 계정이므로 추가 결제가 아니라 이미 가진 한도를 마저 쓰는 것입니다.**

`OpenAiChatCaller`가 429를 받으면 다음 모델로 이어서 시도합니다. 미션 생성·식단 분석·생활습관
인증 세 곳 모두에 적용됩니다. 심사 도중에 한도가 차도 사람이 손댈 필요가 없습니다.

```
OPENAI_MISSION_MODEL=gpt-4o-mini                    # 기본 모델
OPENAI_VISION_MODEL=gpt-4o-mini
OPENAI_FALLBACK_MODELS=gpt-4.1-mini,gpt-4.1-nano    # 막히면 앞에서부터 순서대로
```

- **넘어가는 조건은 429뿐입니다.** 잘못된 요청이나 서버 오류로 모델을 바꿔가며 재시도하면
  같은 실패를 모델 수만큼 반복할 뿐입니다.
- **대체 모델도 이미지를 볼 수 있어야 합니다.** 사진 판정에도 같은 목록을 씁니다.
- 전부 소진되면 그때 `AI_001`로 안내합니다. 로그에 시도한 모델이 남습니다.
- 실제 429를 흉내 내는 `OpenAiChatCallerTest`로 검증합니다 — 기본 모델만 부르는 경우,
  막히면 다음으로 넘어가는 경우, 전부 막힌 경우, 목록에 중복이 있어도 두 번 부르지 않는 경우.

### AI 미션이 실제로 어떻게 나오는지 (gpt-4.1-mini로 확인)

로컬 DB에서 날짜를 하루씩 밀어 3일치를 돌린 결과입니다. 겹치는 미션이 하나도 없습니다.

| | 상황 | 나온 미션 |
|---|---|---|
| 1일차 | 기록 없음 | 오늘 식사 꼭 기록하기 / 채소 1접시 추가 섭취 / 하루 30분 가벼운 걷기 |
| 2일차 | 전날 100% 달성, 저녁 식단 GOOD → **난이도 상승·4개** | 점심에 채소 150g 이상 / 아침에 단백질 20g 이상 / 저녁은 나트륨 1500mg 이하 / 하루 30분 **빠르게** 걷기 |
| 3일차 | 전날 0% 달성, 저녁 식단 BAD → **난이도 하향·3개** | 오늘 **저녁은** 채소 중심 식사 / 점심에 단백질 1종 추가 / 저녁 후 10분 가벼운 스트레칭 |

잘하면 수치가 구체적으로 올라가고(150g·20g·1500mg), 못 하면 다시 완만해집니다.
저녁 식단이 BAD였던 다음 날은 **저녁을 겨냥한 미션**이 나옵니다 — `MissionHistoryAnalyzer`가
식단 분석 결과를 프롬프트에 넣기 때문입니다.

> 같은 테스트를 gpt-4o-mini(한도 초과 상태)로 돌리면 `저녁 과식하지 않기`처럼
> **코드에 박힌 고정 풀 문구**가 나옵니다. 미션이 밋밋해 보이면 한도부터 의심하세요.

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
| **일일 미션 자동 생성 스케줄러** (PRD 6.6) | ✅ 그룹이 정한 시간이 지나면 앱을 안 켠 사람 것도 미리 생성 |
| **AI 피드백 루프** (어제 식단 → 오늘 미션) | ✅ 실패한 끼니를 정조준한 미션 생성 |
| **미션 난이도 자동 조절** (PRD 6.7) | ✅ 90%↑상승(4개) / 50~90%유지(3개) / 50%↓하향 / 3일연속실패→1개 |
| **그룹원 프로필** (PRD 6.12) | ✅ 캐릭터 상태 + 식단/건강 미션 수행 결과 (신체 정보는 비공개) |
| **AI 식단 사진 분석** (GPT-4o-mini Vision) | ✅ 샐러드 승인 / 치킨 거절 — 실제 판별함 |
| **AI 생활습관 인증 사진 판정** | ✅ 걷기 미션에 바깥 풍경은 인정, 음식 사진은 거절 |
| **인증 사진 재사용 차단** | ✅ 원본 SHA-256 대조 — 같은 사진을 다시 쓰면 거절 |
| **캡처 이미지 판별** | ✅ 화면 캡처·워터마크·화면 재촬영은 미달성 (완전 방어는 불가) |
| 미션 도착 | ✅ 방에서 정한 시각에 하루치가 한 번에 (첫날은 즉시) |
| 코인 지급 / 상점 구매·착용 | ✅ |
| 그룹 피드 (사진·달성률·표정) | ✅ |
| **피드 반응·댓글** | ✅ 그룹원 모두에게 보임 (오늘 기준) |
| 캐릭터 표정 3단계 | ✅ 달성률+순위로 실시간 계산 |
| 그룹 랭킹 / 전체 랭킹 | ✅ |
| **7일 챌린지 종료 + 순위별 보상** | ✅ 멱등 처리 (두 번 눌러도 중복 지급 없음) |
| 계속하기 / 방 나가기 | ✅ 둘 다 그 사이클의 미션·식단·온보딩을 정리 — 새 사이클은 온보딩부터 다시 |
| **계정 탈퇴** | ✅ DB에서 완전 삭제 (사진·피드까지) |
| 심사용 데모 데이터 시더 | ✅ `DEMO_SEED=true`, 날짜 바뀌면 자동 재생성 |
| **OpenAI 장애 내성** | ✅ 잘못된 키로 띄워서 실제 확인 — 미션은 계속 나오고, 사진 판정은 503 안내 |
| **AI 한도 소진 시 자동 전환** | ✅ 429가 나면 대체 모델로 이어서 처리 (`OpenAiChatCallerTest` 4개) |
| **폰 사진 회전 보정** | ✅ EXIF 회전값을 픽셀에 구워 저장 (`ImageDownscalerTest`로 자동 검사) |

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

### 하루 지나면 미션이 안 바뀌던 문제 (2026-08-18)

**증상** — 방을 만들고 다음날 열어보면 어제 미션이 그대로 있고, 인증도 되지 않습니다.

**원인은 미션 생성이 아니었습니다.** 그쪽은 정상입니다. 스케줄러가 방에서 정한 시각이 지나면
그룹원이 앱을 켜지 않아도 1분 안에 오늘 미션을 만들어 둡니다(로컬에서 날짜를 되돌려 확인).

진짜 원인은 **토큰 유효기간이 정확히 24시간**이었다는 것입니다. 하루가 지나면 모든 요청이
거절되는데, 프론트에는 그걸 처리하는 코드가 없어서 실패를 조용히 삼키고 브라우저에 남은
**어제 화면을 계속 보여줬습니다.** 서버에는 오늘 미션이 멀쩡히 있는데도 말이죠.

로그인 가드도 이걸 못 잡습니다. 가드가 보는 건 브라우저에 저장된 사용자 정보라,
토큰만 죽고 그 값이 남으면 **로그인된 것처럼 통과시켜 줍니다.**

**고친 것 (두 군데)**

1. `jwt.access-token-validity-ms` 기본값을 24시간 → **7일**로. 챌린지 한 사이클보다 짧으면
   사이클 도중에 반드시 한 번은 끊깁니다. 리프레시 토큰이 없어 그때 재로그인 말고는 방법이 없습니다.
2. 토큰이 없거나 만료되면 **401 + `AUTH_003`**을 내려줍니다(`JwtAuthenticationEntryPoint`).
   예전에는 Spring Security 기본값인 403이 나갔는데, 우리 API에서 403은 이미
   "그룹원이 아님"(`GROUP_005`)이라는 다른 뜻으로 쓰고 있어 프론트가 구분할 수 없었습니다.

**주의 — 403의 의미를 바꾸지 마세요.** 프론트는 그룹 조회가 403이면 "정말 그룹에서 나갔다"로
보고 로컬 그룹 정보를 지웁니다. 인증 실패까지 403으로 돌려주면, 토큰이 만료된 것뿐인데
그룹이 사라진 것으로 처리됩니다.

### 배포 후 실사용에서 잡은 버그 (2026-08-13 ~ 08-14)

배포된 웹앱을 실제 폰으로 쓰면서 나온 것들입니다. **전부 자동화 테스트로는 안 잡히는 유형**이라,
앞으로도 이런 종류는 사람이 직접 써봐야 발견됩니다.

1. **폰 사진이 그룹원에게만 옆으로 누워 보임** (백엔드)
   폰은 사진을 센서가 읽은 방향 그대로 저장하고 "이만큼 돌려서 보여라"는 EXIF에만 적어둡니다.
   `ImageDownscaler`가 1024px로 줄이며 다시 인코딩할 때 그 EXIF가 사라져, 눕힌 화소가 그대로 굳었습니다.
   올린 본인은 브라우저가 만든 썸네일을 보므로 멀쩡했고 **받아 보는 쪽에서만** 돌아가 보였습니다.
   → `ExifOrientation`으로 회전값을 직접 읽어 **픽셀 자체를 돌려서** 저장합니다.
   회전은 눈으로만 확인되는 버그라 `ImageDownscalerTest`로 자동 검사합니다.

2. **AI가 식단 인증을 잘 못함** (백엔드)
   프롬프트는 "오늘의 식단 미션을 달성했는지 판단하라"고 했는데 **정작 그 미션을 안 보내고 있었습니다.**
   AI가 기준을 스스로 지어내니 보수적으로 미달성이 나왔습니다.
   → 미션 제목을 함께 넘기고 판정 기준을 명시했습니다(완벽하지 않아도 방향이 맞으면 인정,
   명백히 어긋날 때만 미달성). 난이도 조절용 `internalFit`은 그대로 솔직하게 매깁니다.

3. **인증해도 표시가 안 바뀜 / 그룹 나가기가 안 됨 / 방 설정이 저장 안 됨** (프론트)
   세 가지 모두 원인이 같습니다 — **화면 상태만 바꾸고 서버에 요청을 안 보냈습니다.**
   서버 기록은 그대로라 15초마다 도는 `sync()`가 원래 값을 도로 불러왔습니다.
   백엔드 API는 셋 다 정상이었고 프론트의 호출 누락이었습니다.

4. **방을 나갔다 새로 만들어도 이전 기록이 그대로 이어짐** (백엔드)
   미션과 식단은 그룹이 아니라 `사용자 + 날짜`로 저장됩니다. 그래서 새 방을 만들어도 이전 방의
   미션과 인증 상태가 따라와, Day 1인데 달성률 33%로 시작하는 것처럼 보였습니다.
   → `GroupService.leave()`에서 그 사이클의 미션·식단·온보딩을 함께 지웁니다.
   누적 코인과 지난 챌린지 결과·뱃지는 개인 이력이라 그대로 둡니다.

5. **서버에서는 나갔는데 화면에는 그룹이 남는 유령 상태** (프론트)
   `sync()`가 그룹 조회에 실패하면 아무것도 안 하고 넘어갔습니다. 그래서 서버에서 탈퇴된 뒤에도
   화면에는 그룹이 계속 보이고, 그 안의 기능은 전부 403이 나면서 아무 반응이 없었습니다.
   → 403(서버의 확답)일 때만 로컬 그룹을 지웁니다. 네트워크 오류로는 지우지 않습니다 —
   잠깐 끊겼다고 그룹이 사라지면 안 되기 때문입니다.

6. **AI가 아무 사진이나 인증해줌** (백엔드)
   4번을 고치면서 판정 기준을 "애매하면 인정" 쪽으로 너무 느슨하게 잡은 것이 원인이었습니다.
   음식이 아닌 사진까지 통과돼 인증이 무의미해졌습니다.
   → 판정을 단계로 나눴습니다. 먼저 사진에 무엇이 보이는지 적게 하고(`food` 필드),
   음식이 아니면 거기서 false, 음식이면 미션의 핵심 요소가 보이는지로 판단합니다.
   사용자가 적어낸 음식 이름과 사진이 다르면 사진을 믿도록 했습니다.
   인식 결과는 로그로 남겨 오판 신고가 들어왔을 때 추적할 수 있게 했습니다.

7. **생활습관 미션은 아무 사진이나 올려도 인증됨** (백엔드 + 프론트)
   걷기 미션에 채소 사진을 올려도 통과했습니다. 원인은 판정이 느슨해서가 아니라
   **사진이 서버로 아예 오지 않아서**였습니다. 프론트가 본문 없이 인증 API를 부르고,
   서버는 "생활습관은 단순 완료 인증"이라며 무조건 완료 처리하고 있었습니다.
   → `LifestyleVisionAiClient`를 추가해 사진을 AI가 판정합니다. 인증 API를 multipart로 바꾸고
   프론트가 사진을 실어 보냅니다. 판정에 실패하면 `MISSION_004`로 거절하고 완료 처리하지 않습니다.
   식단 판정과 나눈 이유는 묻는 것이 다르기 때문입니다 — 식단은 "목표에 맞는 음식인가",
   생활습관은 "그 행동을 하는 상황에서 찍을 법한 사진인가"를 봅니다.
   걷기 인증에 걷는 자기 모습을 찍을 수는 없으므로 **바깥 풍경이면 인정**합니다.

8. **다른 데서 캡처해 온 이미지도 인증됨** (백엔드)
   두 가지를 넣어 줄였습니다. **완전히 막을 수는 없습니다** — 브라우저가 보내는 이미지는
   무엇이든 위조할 수 있고, 새 사진을 계속 구해오면 그만입니다.
   → ① 같은 파일 재사용 차단: 업로드 원본의 SHA-256을 저장하고, 이미 인증에 쓰인 사진이면
   `FILE_004`로 거절합니다. 사진 한 장을 돌려쓰는 가장 쉬운 편법이 막힙니다.
   → ② AI가 캡처 이미지를 가려냅니다: 상태 표시줄·브라우저 UI·마우스 커서가 보이거나,
   워터마크·자막이 얹혀 있거나, 화면을 다시 찍은 티(테두리·모아레)가 나면 미달성입니다.

9. **"계속하기"가 지난 사이클을 이어받음** (백엔드 + 프론트)
   7일이 끝나고 계속하기를 누르면 새 사이클이 시작돼야 하는데, 점수와 시작일만 초기화하고
   미션·식단·온보딩은 그대로 뒀습니다. Day 1인데 어제 미션이 완료된 채로 시작했고,
   목표·신체정보를 다시 입력할 기회도 없이 곧장 그룹 화면으로 넘어갔습니다.
   → 방 나가기에 있던 정리 로직을 `CycleResetService`로 빼서 양쪽이 같이 씁니다.
   프론트는 계속하기 후 온보딩 화면으로 보냅니다. PRD상 목표·신체정보는 사이클마다 다시 받는 값이고,
   7일 사이에 몸이나 목표가 달라졌을 수 있어야 AI가 새 기준으로 미션을 만듭니다.

10. **OpenAI 무료 등급의 하루 요청 한도(50회)를 넘겨 사진 인증이 전부 멈춤** ⚠️ **운영 이슈 — 코드로 못 고칩니다**
    팀원 테스트에서 "처음 한 번은 되는데 그 뒤로는 계속 `지금은 사진을 분석할 수 없어요`"가 나왔습니다.
    코드 문제가 아니라 **키의 하루 한도가 소진된 것**이었습니다. 확인한 응답은 아래와 같습니다.
    ```
    Rate limit reached for gpt-4o-mini ... on requests per day (RPD): Limit 50, Used 50
    ```
    사진 인증 1회 = 요청 1회, 미션 세트 생성 1회 = 요청 1회이므로 **하루 50회면 그룹 하나가
    이틀도 못 씁니다. 심사 당일에 반드시 터집니다.**
    → **해야 할 일: 결제 수단을 등록하거나(한도 해제), 해커톤에서 주는 키로 교체.**
    서버의 `/etc/withu/withu.env`에서 `OPENAI_API_KEY`만 바꾸고 `systemctl restart withu` 하면 됩니다.
    → 코드 쪽에서는 한도 초과(429)를 `AI_001`로 따로 구분했습니다(`OpenAiErrors`).
    로그에 "사용량 한도에 걸렸습니다 — 결제 수단 등록이나 키 교체가 필요합니다"가 찍히므로,
    다음에 같은 증상이 나오면 서버 버그를 뒤지지 않고 바로 판단할 수 있습니다.
    → 미션 생성은 한도에 걸려도 고정 풀로 대체되어 앱이 멈추지 않습니다. 사진 인증은 대체하지
    않습니다 — 사진을 보지도 않고 통과시키면 인증이 거짓말이 되기 때문입니다.

11. **미션이 하루 동안 나눠서 도착함** (백엔드 + 프론트)
    미션 시각 기준 +0h/+3.5h/+7h로 하나씩 열려서, 화면에는 미션 1개만 보이고
    "다음 미션은 오후 12:30에 도착해요"가 떴습니다. PRD 8번은
    **"설정된 시간에 모든 그룹원의 개인 맞춤 미션이 동시에 생성된다"**입니다.
    → `MissionSetCreator`가 세트 전체를 `unlockTime = null`로 저장합니다. 하루치가 함께 열립니다.

12. **방에서 정한 미션 시각이 지켜지지 않음** (백엔드)
    미션 시각을 오후 9시로 설정해도, 아침에 앱을 열면 `POST /api/missions/today`가 그 자리에서
    세트를 만들어버렸습니다. 설정이 저장만 되고 아무 일도 안 한 셈입니다.
    → `MissionSetCreator`가 **미션 시각 전에는 만들지 않습니다.** 그 시각이 되면
    `DailyMissionScheduler`가 그룹원 전원의 세트를 한꺼번에 만듭니다.
    → **첫날은 예외입니다.** 방을 만들거나 코드로 참여한 사람은 그 자리에서 미션을 받습니다.
    아니면 오후 9시로 설정하고 아침에 방을 만든 사람이 하루를 빈 화면으로 보내게 됩니다.
    판정은 `isFirstDay()` — 사이클 시작일이 오늘이거나(방 생성·계속하기) 참여일이 오늘인 경우입니다.

    | 상황 | 미션이 나오는 시점 |
    |---|---|
    | 방을 만든 날 / 코드로 참여한 날 | 즉시 |
    | "계속하기"로 새 사이클을 시작한 날 | 즉시 |
    | 둘째 날부터 | 방에서 정한 미션 시각 (그룹원 전원 동시에) |

    → **미션 시각은 방장뿐 아니라 그룹원 누구나 바꿀 수 있습니다** (PRD 8).
    바꾼 시각은 그룹 전체에 적용되고, 다음 세트부터 반영됩니다. 이미 만들어진 오늘 미션이
    사라지지는 않습니다 — 받은 미션이 설정 변경으로 없어지면 그날 인증한 기록까지 날아갑니다.

    실제로 돌려서 확인한 내용입니다 (미션 시각을 "2시간 뒤"로 잡고 시작).

    ```
    1. 방을 만든 날            → 방장 3개 / 참여자 3개   (시각 전이지만 즉시)
    2. 다음 날, 아직 그 시각 전  → 방장 0개 / 참여자 0개
    3. 참여자(방장 아님)가 시각 변경 → 200, DB에 반영됨
    4. 바뀐 시각이 지난 뒤       → 방장 3개 / 참여자 3개
       unlock_time 있는 미션 0개 (하루치가 한 번에 열림)
    ```

13. **계정 탈퇴가 DB에서 지우지 않음** (백엔드 + 프론트)
    탈퇴 버튼이 브라우저 저장소만 비웠습니다. 서버에는 탈퇴 API 자체가 없었습니다.
    화면에서만 사라지고 계정은 그대로 남아, 같은 이메일로 다시 가입하면 "이미 사용 중인
    이메일"이 뜨고 전체 랭킹에도 계속 나왔습니다.
    → `DELETE /api/auth/me` 추가 (`AccountDeletionService`). 캐릭터·미션·식단·온보딩·
    챌린지 결과·뱃지·그룹 소속·인증 사진까지 전부 지웁니다.
    → 사진은 **미션·식단 행을 지우기 전에** 주소를 모아둡니다. 먼저 지우면 어떤 파일이
    이 사람 것인지 알 수 없게 됩니다.
    → 방장이 탈퇴하면 남은 그룹원 중 가장 먼저 들어온 사람에게 방장을 넘기고,
    마지막 한 명이 탈퇴하면 빈 방까지 정리합니다 (`GroupService.leave()`와 같은 규칙).
    → 로컬 MySQL로 확인: 방장 탈퇴 후 users/characters/missions/onboardings/group_members가
    전부 0행, 방장은 남은 그룹원에게 이동, 같은 이메일 재가입 201, 마지막 그룹원까지
    탈퇴하면 방 0개.

14. **프론트가 만든 반응·댓글이 내 기기에만 저장됨** (백엔드에서 대응)
    프론트가 그룹 피드에 반응(❤️👍😂😮😢)과 댓글 기능을 먼저 만들었는데, 서버 API가 없어서
    브라우저 저장소에만 쌓고 있었습니다. **남긴 사람 기기에서만 보이고 그룹원에게는 안 보입니다.**
    서로의 진행을 보며 동기부여를 받는 것이 그룹 피드의 목적이라 반쪽짜리가 됩니다.
    → `feed` 도메인 추가: `GET /api/feed`, `POST /api/feed/reactions`, `POST /api/feed/comments`.
    → 반응은 `(그룹, 날짜, 남긴 사람, 받는 사람)` 유니크 — 한 사람이 같은 상대에게 하루 하나.
    같은 이모지를 다시 보내면 취소, 다른 이모지면 교체 (프론트 `TOGGLE_REACTION`과 같은 규칙).
    → 셋 다 갱신된 피드 전체를 돌려줍니다. 반응 하나 누를 때마다 목록을 다시 조회하는 왕복을
    없애고, 그 사이 다른 그룹원이 남긴 것까지 함께 받아가게 하려는 것입니다.
    → **프론트가 아직 이 API를 호출하지 않습니다.** 연결 전까지는 여전히 기기별로만 보입니다.

15. **상점 의상이 프론트와 어긋남** (백엔드)
    프론트가 캐릭터 아트를 교체하면서 의상이 `formal/picnic/sport` → `sailor/coat/detective`로
    바뀌었는데 백엔드 카탈로그는 그대로였습니다. 그대로 두면 **세일러·코트·탐정 세트를 사려 할 때
    `SHOP_001 존재하지 않는 의상`으로 거절**되고, 파자마는 프론트 30코인 / 백엔드 35코인으로
    코인이 어긋납니다.
    → `OutfitCatalog`을 프론트 `ShopPage.jsx`의 `OUTFIT_SETS`와 일치시켰습니다
    (pajama 30 / sailor 35 / coat 40 / detective 50).
    → 예전 의상을 입은 채 남아 있는 사람은 프론트에 그 이미지가 없어 캐릭터가 깨져 보입니다.
    `Character.getOutfit()`이 카탈로그에 없는 의상을 기본 의상으로 바꿔 내보냅니다.
    저장된 값은 건드리지 않아, 그 옷이 돌아오면 다시 입은 상태가 됩니다.

16. **내 카드에만 반응이 안 남음** (프론트, 백엔드에서 원인 확인)
    프론트가 피드 반응·댓글을 서버에 연결하면서, 그룹원 카드의 식별자를 그대로
    `targetUserId`로 보냈습니다. 그런데 화면은 **나를 `'me'`로 부릅니다**(`buildRanking`).
    `Number('me')`는 `NaN`이고 JSON에서 `null`로 나가 서버가 400으로 거절합니다.
    받는 쪽도 마찬가지로, 서버는 실제 userId를 키로 주는데 화면은 `reactions['me']`를 찾아
    **내가 받은 반응이 내 카드에서만 안 보였습니다.**
    → 서버가 프론트 내부 표기(`'me'`)를 알아야 하는 구조는 잘못이므로 **API는 그대로 두고
    프론트에서 변환**했습니다. 보낼 때는 `'me'` → 내 userId, 받을 때는 내 userId → `'me'`.
    변환은 `SET_FEED` 리듀서 한 곳에 모았습니다 — 거기서만 `state.auth.userId`를 알기 때문입니다.

> **교훈**: "내 화면에서는 되는데" 유형은 대부분 **서버에 안 보냈거나, 서버에서 안 받아오는 것**입니다.
> 상태를 바꾸는 화면을 만들면 `dispatch` 옆에 API 호출이 있는지, 그리고 그 값이
> `AppContext`의 `sync()`가 다시 받아오는 목록에 포함되는지 두 가지를 같이 확인하세요.

### 같은 기간 프론트(`RISE-client`)에서 고친 것

백엔드만 고쳐서는 해결되지 않는 것들이라 함께 정리합니다. 상세 설명은 `RISE-client/README.md`에 있습니다.

| 파일 | 고친 내용 |
|---|---|
| `pages/GroupSettingsPage.jsx` | 그룹 나가기·방 이름·미션 시각이 **서버 호출 없이** 화면만 바꾸던 것 수정 |
| `context/AppContext.jsx` | `sync()`에 식단 조회 추가 / 403이면 유령 그룹 정리 / 방 이름·미션 시각까지 동기화 / 백엔드 모드에서 미션 로컬 생성 중단 |
| `api/client.js` | 오류에 `status`와 `code`를 실어 보냄 — 403(서버의 확답)과 네트워크 오류를 구분하기 위해 |
| `pages/MealUploadPage.jsx` | 모든 식단 미션에서 "아침 사진 업로드"만 뜨던 것을 **미션 제목**으로 바꿈 |
| `components/ChallengeSummarySheet.jsx` | 7일 결산 캐릭터에 `outfit`을 안 넘겨 산 옷이 반영되지 않던 것 수정 |
| `api/missionApi.js` | mock 미션도 시간대별 도착을 없애고 한 번에 열리도록 통일 (위 11번) |
| `api/missionApi.js` | 생활습관 인증에 **사진을 multipart로 전송** (예전에는 본문 없이 호출) |
| `api/mealApi.js` | `fetchTodayMeals()` 추가 |
| `pages/MissionVerifyPage.jsx` | 인증 거절 시 완료 표시 대신 사유 노출, 재시도 가능하게 |
| `pages/OnboardingPage.jsx` | 온보딩 저장 직후 `sync()` 호출 (미션이 15초간 비어 보이던 문제) |

---

## 프론트에서 해야 할 일

프론트는 `main`에 push되어 Vercel에 배포까지 끝났습니다. 아래는 남은 것들입니다.

### 1. 미션 개수 3개 고정 가정 확인

백엔드가 상황에 따라 **1개 / 3개 / 4개**를 내려줍니다(난이도 상승 시 4개, 3일 연속 실패 시 1개).
3개를 전제로 짜인 화면이 있으면 깨집니다.

### 2. 남은 화면에도 같은 유형이 없는지 점검

`GroupSettingsPage`(나가기·방 설정), `MissionVerifyPage`(인증 사진), `sync()`(식단·방 이름)에서
같은 유형의 누락이 연달아 나왔습니다. 점검 방법은 간단합니다 — `dispatch({ type: ... })`를 검색해서,
서버에 남아야 하는 값인데 옆에 API 호출이 없으면 그게 버그입니다.

### 3. 인증 거절 안내 문구 다듬기

사진 판정이 생기면서 거절 사유가 여러 개입니다(미션 불일치 `MISSION_004`, 재사용 `FILE_004`,
AI 장애 `MISSION_005`). 지금은 서버 메시지를 그대로 띄우고 있으니, 화면에 맞게 다듬을 여지가 있습니다.

### 이미 끝나서 할 일이 아닌 것

- 백엔드 연동 전체(로그인/미션/식단/그룹/상점/랭킹) — 배포된 웹앱에서 실동작 확인 완료
- 그룹원 프로필, 7일 결과 화면, 로그아웃 격리, 다른 기기 로그인 복원
- `VITE_API_BASE_URL` 배포 주소 설정 — Vercel에 등록 완료
- 그룹 나가기·방 설정 서버 반영, 인증 사진 전송, 유령 그룹 정리 — 모두 수정 완료

---

## 남은 일 (우선순위 순)

1. **AI 사진 판정 실사용 확인** — 서버 이전 후 OpenAI 키가 살아 있는 것(`/v1/models` 200)과
   모델 3종 사용 가능은 확인했지만, 실제 사진을 올려 판정까지 도는 것은 아직 못 봤습니다.
2. **프론트 초기 로딩 용량** — `vite-plugin-singlefile`이 이미지까지 HTML 하나에 넣어
   첫 화면이 **14MB(gzip 10MB)** 입니다. 플러그인을 빼면 초기 로딩이 약 100KB로 줄고
   이미지는 필요할 때 받습니다. 웹 배포에는 빼는 편이 낫지만, 단일 파일 데모 용도로
   일부러 넣은 것일 수 있어 프론트 담당자 확인이 필요합니다.
3. **기획 확인 필요** — 그룹 랭킹 기준이 PRD 10에는 '주간 달성률'인데 구현·프론트 모두
   '오늘 달성률'입니다. 기획자 판단이 필요해 임의로 바꾸지 않았습니다.
4. **PRD 대비 남은 소소한 것** — 코인 획득 중 '하루 전체 달성'·'연속 달성' 보너스.
   단 PRD 6.15/8은 코인·상점을 UI 목업으로 규정하므로 우선순위 낮음.

> `main` 병합은 완료했습니다. 두 저장소 모두 `main`에 최신 코드가 올라가 있습니다.

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
