// 식단 사진 AI 분석.
// 백엔드 연동 시 POST /api/meals/{slot}/analyze (multipart) — GPT-4o Vision이 실제로 분석한다.
// 기획 상 사용자에게는 "미션 달성/미달성"만 노출하고, 목표적합도(internalFit)는
// 다음 미션 생성 기준으로만 내부에서 쓰임 — UI에서 internalFit을 표시하지 말 것.

import { api, isBackendEnabled } from './client';

function pickInternalFit() {
  const r = Math.random();
  if (r < 0.6) return 'good';
  if (r < 0.85) return 'normal';
  return 'bad';
}

// input: (File, mealKey). output: { achieved: boolean, internalFit?: 'good'|'normal'|'bad' }
export function analyzeMealPhoto(file, mealKey, { foodName, portion } = {}) {
  if (isBackendEnabled) {
    const form = new FormData();
    form.append('photo', file);
    if (foodName) form.append('foodName', foodName);
    if (portion) form.append('portion', portion);
    // 백엔드는 internalFit을 응답에 내려주지 않는다(내부 전용) — 프론트는 achieved만 쓴다.
    return api.postForm(`/api/meals/${mealKey.toUpperCase()}/analyze`, form);
  }

  return new Promise((resolve) => {
    setTimeout(() => {
      const internalFit = pickInternalFit();
      resolve({ achieved: internalFit !== 'bad', internalFit });
    }, 1200);
  });
}
