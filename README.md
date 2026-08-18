# WITHU

AI가 매일 개인 맞춤 미션을 만들어 주고, 2~4인 그룹이 함께 7일간 실천하는 건강 습관 웹앱입니다.

멋쟁이사자처럼 대학 14기 중앙 해커톤 · 성결대 3팀

| | 주소 |
|---|---|
| 웹앱 | https://rise-client-rohdaeyoungs-projects.vercel.app |
| API | https://1-201-117-9.nip.io |
| API 문서 | https://1-201-117-9.nip.io/swagger-ui.html |

**테스트 계정: `test@withu.app` / `withu1234`** (그룹 코드 `TEAM33`, Day 7 상태)

## 이 저장소의 구조

프론트와 백엔드를 한 저장소에 담되, 서로의 코드를 가져다 쓰지는 않습니다.
둘을 잇는 것은 **주소 하나**뿐입니다 — `frontend/.env`의 `VITE_API_BASE_URL`.
그래서 빌드도 배포도 각각 따로 돌아갑니다.

```
frontend/   React + Vite   → Vercel        (배포 루트 디렉터리: frontend)
backend/    Spring Boot    → 가비아 클라우드  (해커톤에서 제공받은 서버)
```

백엔드는 2026-08-18에 Railway에서 해커톤 제공 서버로 옮겼습니다. API 주소가 IP처럼
생긴 것은 도메인을 사지 않고 HTTPS를 붙이기 위해 `nip.io`를 쓴 것입니다
(`1-201-117-9.nip.io` → `1.201.117.9`). 서버에서는 Caddy가 Let's Encrypt 인증서를
받아 앞단을 맡고, 그 뒤에 Spring Boot와 MySQL이 systemd로 떠 있습니다.
자세한 절차는 [backend/README.md](backend/README.md)의 "배포"에 있습니다.

각 폴더의 README에 그쪽 이야기가 전부 들어 있습니다.

- [frontend/README.md](frontend/README.md) — 화면 구조, 상태 관리, 고칠 때 주의할 것
- [backend/README.md](backend/README.md) — API, 배포, 심사용 데모 계정, 서버 이전 절차

## 처음 받은 사람이 실행하는 법

아래 순서 그대로 하면 됩니다. **실제로 새로 clone해서 확인한 절차입니다.**

### 프론트만 띄우기 (백엔드 없이도 전체 화면이 돕니다)

```bash
cd frontend
npm install
npm run dev
```

끝입니다. `.env`를 만들지 않으면 mock 모드로 동작해서 로그인부터 7일 결과까지
전 화면을 그대로 볼 수 있습니다. 백엔드도 MySQL도 필요 없습니다.

### 백엔드까지 띄우기

MySQL이 필요합니다. 없으면 도커로 한 줄이면 됩니다.

```bash
docker run --name withu-mysql -p 3306:3306 \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=withu -d mysql:8
```

이미 MySQL이 있다면 `withu` 데이터베이스만 만들어 두세요.

```sql
CREATE DATABASE withu CHARACTER SET utf8mb4;
```

그다음 **`local` 프로필로** 실행합니다.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local gradle bootRun
```

`http://localhost:8080` 에서 뜹니다. 표는 JPA가 알아서 만듭니다.

> **`SPRING_PROFILES_ACTIVE=local`을 빼면 뜨지 않습니다.** 기본 프로필은 배포용이라
> DB 접속 정보를 환경변수에서 찾고, 없으면 `Access denied for user 'root'`로 실패합니다.
> MySQL이 3306이 아닌 포트에 있으면 `DB_PORT=3307`처럼 함께 넘기세요.

프론트를 이 백엔드에 붙이려면 `frontend/.env.local`을 만드세요.

```
VITE_API_BASE_URL=http://localhost:8080
```

### 알아두면 좋은 것

- **OpenAI 키는 없어도 됩니다.** 키가 없으면 미션 생성이 고정 문구로 대체되어
  앱 전체가 그대로 돌아갑니다. 실제 AI 미션과 사진 판정을 보려면
  `backend/.env`에 `OPENAI_API_KEY=...`를 넣으세요.
- **심사용 데모 계정을 만들려면** `DEMO_SEED=true`를 함께 넘기세요.
  7일차까지 진행된 4인 그룹이 기동할 때 만들어집니다.
- Gradle wrapper 다운로드가 막힌 네트워크에서는 `./gradlew` 대신 시스템 `gradle`을 쓰세요.

## 심사용 계정

```
아이디   test@withu.app
비밀번호  withu1234
```

로그인하면 7일차까지 진행된 4인 그룹에 들어가 있고, 7일 챌린지 결과를 바로 볼 수 있습니다.
자세한 내용은 [backend/README.md](backend/README.md)의 "심사용 데모 계정"을 보세요.
