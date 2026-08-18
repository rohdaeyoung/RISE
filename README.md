# WITHU

AI가 매일 개인 맞춤 미션을 만들어 주고, 2~4인 그룹이 함께 7일간 실천하는 건강 습관 웹앱입니다.

멋쟁이사자처럼 대학 14기 중앙 해커톤 · 성결대 3팀

| | 주소 |
|---|---|
| 웹앱 | https://rise-client-rohdaeyoungs-projects.vercel.app |
| API | https://rise-server-production.up.railway.app |

## 이 저장소의 구조

프론트와 백엔드를 한 저장소에 담되, 서로의 코드를 가져다 쓰지는 않습니다.
둘을 잇는 것은 **주소 하나**뿐입니다 — `frontend/.env`의 `VITE_API_BASE_URL`.
그래서 빌드도 배포도 각각 따로 돌아갑니다.

```
frontend/   React + Vite   → Vercel   (배포 루트 디렉터리: frontend)
backend/    Spring Boot    → Railway  (배포 루트 디렉터리: backend)
```

각 폴더의 README에 그쪽 이야기가 전부 들어 있습니다.

- [frontend/README.md](frontend/README.md) — 화면 구조, 상태 관리, 고칠 때 주의할 것
- [backend/README.md](backend/README.md) — API, 배포, 심사용 데모 계정, 서버 이전 절차

## 빠르게 띄우기

```bash
# 백엔드 (MySQL 필요)
cd backend && gradle bootRun

# 프론트
cd frontend && npm install && npm run dev
```

`VITE_API_BASE_URL`을 비우면 백엔드 없이 mock 모드로도 전체 화면이 돌아갑니다.

## 심사용 계정

```
아이디   test@withu.app
비밀번호  withu1234
```

로그인하면 7일차까지 진행된 4인 그룹에 들어가 있고, 7일 챌린지 결과를 바로 볼 수 있습니다.
자세한 내용은 [backend/README.md](backend/README.md)의 "심사용 데모 계정"을 보세요.
