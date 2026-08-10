// 식단 사진 AI 분석 mock. 백엔드 연동 시:
//   analyzeMealPhoto -> POST /api/meals/analyze (multipart) 로 교체.
// 기획 상 사용자에게는 "미션 달성/미달성"만 노출하고, 목표적합도(internalFit)는
// 다음 미션 생성 기준으로만 내부에서 쓰임 — UI에서 internalFit을 표시하지 말 것.

function pickInternalFit() {
  const r = Math.random();
  if (r < 0.6) return 'good';
  if (r < 0.85) return 'normal';
  return 'bad';
}

// input: File (사진). output: { achieved: boolean, internalFit: 'good'|'normal'|'bad' }
export function analyzeMealPhoto() {
  return new Promise((resolve) => {
    setTimeout(() => {
      const internalFit = pickInternalFit();
      resolve({ achieved: internalFit !== 'bad', internalFit });
    }, 1200);
  });
}
