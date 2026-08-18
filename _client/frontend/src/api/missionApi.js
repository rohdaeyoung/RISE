import { api } from './client';

// AI 일일 미션 생성 mock. 백엔드가 연결되면 아래 fetchOrCreateTodayMissions()가 대신 쓰인다.
//   generateDailyMissions -> POST /api/missions/today { goal, gender, age, height, weight, history }
// 지금은 목표(goal) 하나만 보고 정해진 풀에서 식단 2개 + 생활습관 1개를 뽑는 규칙표로 대체함.
// 신체정보(gender/age/height/weight)는 실제 AI 연동 시 kcal·영양 기준을 세분화하는 데 쓰임.

const MISSION_POOL = {
  diet: [
    '아침 든든하게 챙겨 먹기',
    '점심 탄단지 균형 맞추기',
    '저녁 과식하지 않기',
    '야식 대신 물 마시기',
    '채소 반찬 한 가지 이상 먹기',
  ],
  bulk: [
    '고단백 식단 챙겨 먹기',
    '운동 후 30분 내 단백질 섭취하기',
    '삼시세끼 거르지 않기',
  ],
  health: [
    '채소 3가지 이상 포함해서 먹기',
    '삼시세끼 챙겨먹기',
    '가공식품 대신 자연식 선택하기',
  ],
};

const LIFESTYLE_POOL = [
  '물 8잔 마시기',
  '엘리베이터 대신 계단 이용하기',
  '자기 전 스트레칭 5분 하기',
];

const DIET_POOL_BY_GOAL = { diet: MISSION_POOL.diet, bulk: MISSION_POOL.bulk, health: MISSION_POOL.health };

export const DEFAULT_MISSION_HOUR = 9;
export const DEFAULT_MISSION_MINUTE = 0;

// 하루치 미션(식단 2개 + 생활습관 1개)은 그룹에서 설정한 "미션 시작 시간"에 한꺼번에 도착한다
// (PRD 8 — "설정된 시간에 모든 그룹원의 개인 맞춤 미션이 동시에 생성된다").
// 예전에는 +3.5h/+7h로 나눠 열어서 미션 하나만 보이고 "다음 미션은 오후 12:30에 도착해요"가 떴다.

export function formatClock(totalMinutes) {
  const wrapped = ((totalMinutes % (24 * 60)) + 24 * 60) % (24 * 60);
  const hour = Math.floor(wrapped / 60);
  const minute = wrapped % 60;
  const period = hour < 12 ? '오전' : '오후';
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  return { hour, minute, label: `${period} ${displayHour}:${String(minute).padStart(2, '0')}` };
}

function pickRandom(pool, count) {
  return [...pool].sort(() => 0.5 - Math.random()).slice(0, count);
}

// input: { goal }. 세트를 만드는 시점 = 미션이 도착한 시점이므로 전부 열린 상태로 만든다.
// (missionHour/missionMinute는 백엔드 스케줄러가 "언제 세트를 만들지"를 정할 때 쓰고,
//  mock 모드에는 스케줄러가 없어 호출 시점에 바로 만든다.)
// output: [{ id, type: 'diet'|'lifestyle', title, done, unlockHour, unlockMinute, unlockLabel }]
export function generateDailyMissions({ goal }) {
  const dietPool = DIET_POOL_BY_GOAL[goal] || MISSION_POOL.health;
  const dietTitles = pickRandom(dietPool, Math.min(2, dietPool.length));
  const lifestyleTitles = pickRandom(LIFESTYLE_POOL, Math.min(1, LIFESTYLE_POOL.length));

  const open = { unlockHour: null, unlockMinute: null, unlockLabel: '지금' };
  return [
    ...dietTitles.map((title, i) => ({ id: `d${i}-${Date.now()}`, type: 'diet', title, done: false, ...open })),
    ...lifestyleTitles.map((title, i) => ({ id: `l${i}-${Date.now()}`, type: 'lifestyle', title, done: false, ...open })),
  ];
}

// 미션이 도착 시간이 지나 지금 인증 가능한 상태인지 여부.
export function isMissionUnlocked(mission, now = new Date()) {
  if (mission.unlockHour == null) return true;
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const unlockMinutes = mission.unlockHour * 60 + (mission.unlockMinute || 0);
  return nowMinutes >= unlockMinutes;
}

// 아직 도착 안 한 미션은 화면에 아예 안 보이고, 도착 시간이 된 것만 하나씩 나타남.
export function visibleMissions(missions) {
  return missions.filter((m) => m.done || isMissionUnlocked(m));
}

// 다음으로 도착할 미션(가장 이른 도착 예정 시각) — "다음 미션은 O시에 도착해요" 안내용.
export function nextUpcomingMission(missions) {
  const upcoming = missions.filter((m) => !m.done && !isMissionUnlocked(m));
  if (upcoming.length === 0) return null;
  return upcoming.reduce((soonest, m) => {
    const mMinutes = m.unlockHour * 60 + (m.unlockMinute || 0);
    const soonestMinutes = soonest.unlockHour * 60 + (soonest.unlockMinute || 0);
    return mMinutes < soonestMinutes ? m : soonest;
  });
}

// 달성률에 따른 다음날 난이도 조정 (기획서: 90%↑ 상승 / 50~89% 유지 / 50%↓ 하향, 3일 연속 실패 시 미션 1개로 축소)
export function nextDifficulty(achievementRate) {
  if (achievementRate >= 90) return 'up';
  if (achievementRate >= 50) return 'keep';
  return 'down';
}

// 생활습관 미션 인증 사진 AI 확인.
// 식단 미션과 달리 목표적합도 판단이 필요 없는 단순 완료 인증이라 항상 성공 처리.
// 실제 미션 완료(코인 지급 포함)는 백엔드 모드에서 completeMission()이 담당한다.
export function analyzeMissionPhoto() {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ verified: true }), 900);
  });
}

// ---------------------------------------------------------------------------
// 백엔드 연동 (VITE_API_BASE_URL이 설정된 경우에만 사용됨)
// ---------------------------------------------------------------------------

// 백엔드 미션 응답을 프론트 상태가 쓰는 모양으로 변환.
// 백엔드는 unlockTime을 "12:30:00" 형태로 주고, null이면 "지금 바로 열림"을 뜻한다.
function toMission(m) {
  if (!m.unlockTime) {
    return {
      id: String(m.id),
      type: m.type.toLowerCase(),
      title: m.title,
      done: m.done,
      unlockHour: null,
      unlockMinute: null,
      unlockLabel: '지금',
    };
  }
  const [hour, minute] = m.unlockTime.split(':').map(Number);
  return {
    id: String(m.id),
    type: m.type.toLowerCase(),
    title: m.title,
    done: m.done,
    unlockHour: hour,
    unlockMinute: minute,
    unlockLabel: formatClock(hour * 60 + minute).label,
  };
}

// 오늘의 AI 미션을 생성(없으면)하고 받아온다 — 백엔드가 GPT로 실제 생성.
export function fetchOrCreateTodayMissions() {
  return api.post('/api/missions/today').then((data) => data.missions.map(toMission));
}

export function fetchTodayMissions() {
  return api.get('/api/missions/today').then((data) => data.missions.map(toMission));
}

// 생활습관 미션 인증 (판정·완료·코인 지급 모두 서버가 담당).
// 사진을 반드시 함께 보내야 한다 — 예전에는 사진 없이 호출해서 서버가 무조건 완료 처리했고,
// 걷기 미션에 음식 사진을 올려도 인증되어 인증 자체가 의미가 없었다.
// 사진이 미션과 맞지 않으면 서버가 MISSION_004로 거절하고, 그 메시지를 화면에 그대로 보여준다.
export function completeMission(missionId, photo) {
  const form = new FormData();
  form.append('photo', photo);
  return api.postForm(`/api/missions/${missionId}/verify`, form);
}
