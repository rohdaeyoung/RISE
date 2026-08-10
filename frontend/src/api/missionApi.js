// AI 일일 미션 생성 mock. 백엔드 연동 시 GPT-4o 기반 생성 API로 교체될 자리.
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

// 미션은 한 번에 다 열리지 않고, 그룹에서 설정한 "미션 시작 시간"부터 하루 동안 시간대별로
// 하나씩 도착함 (식단 2개 + 생활습관 1개, 총 3개). 오프셋은 시작 시간 기준 +0h/+3.5h/+7h.
const UNLOCK_OFFSET_MINUTES = [0, 210, 420, 660];

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

// input: { goal, missionHour, missionMinute, firstUnlocksNow }. 그룹에서 설정한 미션 시작 시간이 기준.
// firstUnlocksNow: 그룹을 막 만든 시점처럼, 미션 시작 시간이 아직 안 됐어도 첫 미션 하나는 바로
// 보여주고 싶을 때 true로 넘김 — 나머지는 그대로 시간대별로 도착.
// output: [{ id, type: 'diet'|'lifestyle', title, done, unlockHour, unlockMinute, unlockLabel }]
export function generateDailyMissions({
  goal,
  missionHour = DEFAULT_MISSION_HOUR,
  missionMinute = DEFAULT_MISSION_MINUTE,
  firstUnlocksNow = false,
}) {
  const dietPool = DIET_POOL_BY_GOAL[goal] || MISSION_POOL.health;
  const dietTitles = pickRandom(dietPool, Math.min(2, dietPool.length));
  const lifestyleTitles = pickRandom(LIFESTYLE_POOL, Math.min(1, LIFESTYLE_POOL.length));

  const missions = [
    ...dietTitles.map((title, i) => ({ id: `d${i}-${Date.now()}`, type: 'diet', title, done: false })),
    ...lifestyleTitles.map((title, i) => ({ id: `l${i}-${Date.now()}`, type: 'lifestyle', title, done: false })),
  ];

  const baseMinutes = missionHour * 60 + missionMinute;
  return missions.map((mission, i) => {
    if (firstUnlocksNow && i === 0) {
      return { ...mission, unlockHour: null, unlockMinute: null, unlockLabel: '지금' };
    }
    const offset = UNLOCK_OFFSET_MINUTES[i] ?? UNLOCK_OFFSET_MINUTES[UNLOCK_OFFSET_MINUTES.length - 1];
    const slot = formatClock(baseMinutes + offset);
    return { ...mission, unlockHour: slot.hour, unlockMinute: slot.minute, unlockLabel: slot.label };
  });
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

// 생활습관 미션 인증 사진 AI 확인 mock. 백엔드 연동 시 POST /api/missions/:id/verify (multipart)로 교체.
// 식단 미션과 달리 목표적합도 판단이 필요 없는 단순 완료 인증이라 항상 성공 처리.
export function analyzeMissionPhoto() {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ verified: true }), 900);
  });
}
