// 7일 챌린지 종료 / 결과 조회 / 계속하기.
// 백엔드가 사이클 점수 기준으로 최종 순위를 매기고 순위별 보상(뱃지·코인·점수)을 지급한다.
// mock 모드에서는 AppContext의 END_CHALLENGE / CONTINUE_CHALLENGE 리듀서가 로컬로 계산한다.

import { api, fileUrl, isBackendEnabled } from './client';

// 백엔드 결과를 ChallengeSummarySheet가 쓰는 모양으로 변환.
function toSummary(data) {
  return {
    groupName: data.groupName,
    days: data.days,
    achievementRate: data.achievementRate,
    coinsEarned: data.coinsEarned,
    rank: data.rank,
    totalParticipants: data.totalParticipants,
    badgeAwarded: data.badgeAwarded,
    ranking: (data.ranking || []).map((p) => ({
      id: p.me ? 'me' : String(p.userId),
      isMe: p.me,
      label: p.label,
      species: p.species,
      expression: (p.expression || 'normal').toLowerCase(),
      outfit: p.outfit,
      rate: p.achievementRate,
      points: p.points,
      bonusCoins: p.bonusCoins,
      badgeAwarded: p.badgeAwarded,
    })),
    members: (data.ranking || []).filter((p) => !p.me).map((p) => ({ id: String(p.userId) })),
    mealPhotos: (data.mealPhotos || []).map((m) => ({
      mealKey: m.mealKey,
      mealLabel: m.mealLabel,
      photo: fileUrl(m.photoUrl),
    })),
  };
}

export function endChallenge() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post('/api/challenges/end').then(toSummary);
}

export function fetchChallengeSummary() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/challenges/summary').then(toSummary);
}

export function continueChallenge() {
  if (!isBackendEnabled) return Promise.resolve();
  return api.post('/api/challenges/continue');
}
