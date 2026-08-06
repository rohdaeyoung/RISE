# RISE API 명세서

> 백엔드 ↔ 프론트엔드 계약 문서
> **변경 시 반드시 양쪽 팀 공유 후 반영**

Base URL: `https://api.rise.io/v1`
Auth Header: `Authorization: Bearer {ACCESS_TOKEN}`

---

## Auth

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/auth/signup` | 회원가입 |
| POST | `/auth/login` | 로그인 |
| POST | `/auth/refresh` | 토큰 갱신 |

### POST /auth/signup
```json
Request:  { "email": "string", "password": "string", "nickname": "string" }
Response: { "userId": "uuid", "accessToken": "string", "refreshToken": "string" }
```

### POST /auth/login
```json
Request:  { "email": "string", "password": "string" }
Response: { "userId": "uuid", "accessToken": "string", "refreshToken": "string" }
```

---

## Onboarding

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/onboarding` | 온보딩 정보 저장 |

```json
Request: {
  "groupId": "uuid",
  "goal": "DIET | BULK | HEALTH",
  "gender": "MALE | FEMALE",
  "age": 24,
  "height": 165.0,
  "weight": 60.0
}
```

---

## Group

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/groups` | 그룹 생성 |
| POST | `/groups/join` | 코드로 참여 |
| GET | `/groups/{groupId}` | 그룹 정보 |
| GET | `/groups/{groupId}/members` | 그룹원 목록 |
| DELETE | `/groups/{groupId}/leave` | 방 나가기 |

```json
POST /groups Request:  { "missionTime": "08:00" }
POST /groups Response: { "groupId": "uuid", "inviteCode": "ABC123" }
POST /groups/join:     { "inviteCode": "ABC123" }
```

---

## Mission

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/missions/today` | 오늘 내 미션 |
| PATCH | `/missions/{missionId}/complete` | 미션 완료 |
| GET | `/missions/history` | 수행 기록 |

```json
GET /missions/today Response:
{
  "date": "2026-08-06",
  "missions": [
    { "missionId": "uuid", "title": "아침 식사 챙겨먹기", "type": "DIET", "completed": false },
    { "missionId": "uuid", "title": "물 2L 마시기", "type": "HABIT", "completed": true }
  ]
}
```

---

## Feed

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/groups/{groupId}/feed` | 피드 조회 |
| POST | `/groups/{groupId}/feed` | 식단 사진 업로드 |

```
POST multipart/form-data:
  photo: File
  mealType: BREAKFAST | LUNCH | DINNER
  foodName: string (optional)
  servingSize: HALF | ONE | ONE_HALF

Response 201:
{
  "feedId": "uuid",
  "aiAnalysis": {
    "missionSuccess": true,
    "feedback": "단백질 섭취가 충분해요! 채소를 조금 더 추가하면 좋을 것 같아요.",
    "nutritionBalance": "GOOD | NORMAL | BAD"
  }
}
```

---

## Character

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/characters/me` | 내 캐릭터 |
| PATCH | `/characters/me/outfit` | 의상 변경 |

```json
Response: {
  "characterId": "uuid",
  "outfitId": "string",
  "expression": "HAPPY | NORMAL | SAD",
  "level": 1
}
```

---

## Ranking

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/groups/{groupId}/ranking` | 방 내 랭킹 |
| GET | `/ranking/global` | 전체 랭킹 |

---

## Coin

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/coins/me` | 내 코인 잔액 |
| POST | `/coins/purchase` | 아이템 구매 |

