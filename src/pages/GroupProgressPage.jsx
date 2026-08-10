import { useNavigate } from 'react-router-dom';
import { CheckCircle2, CircleDot, Minus } from 'lucide-react';
import { achievementRate, CHALLENGE_LENGTH_DAYS, dayIndexOf, useAppState } from '../context/AppContext';

const TOTAL_DAYS = CHALLENGE_LENGTH_DAYS;

function dayStatus(rate) {
  if (rate >= 100) return { label: '완료', Icon: CheckCircle2, tone: 'bg-brand-soft/70 text-brand-dark' };
  if (rate <= 0) return { label: '미인증', Icon: Minus, tone: 'bg-black/5 text-sub' };
  return { label: `${rate}%`, Icon: CircleDot, tone: 'bg-cream text-ink' };
}

// 7일 챌린지 중간에도 지금까지 며칠을 어떻게 보냈는지 한눈에 보여주는 화면.
// dailyHistory(지나간 날 스냅샷) + 오늘의 실시간 missions/meals로 1~7일차를 채움.
// 최종 결과(게임 오버 느낌의 요약 화면)는 7일째 END_CHALLENGE 이후 별도 ChallengeSummarySheet가 담당.
export default function GroupProgressPage() {
  const state = useAppState();
  const navigate = useNavigate();
  const { group, dailyHistory } = state;

  if (!group) {
    return (
      <div className="min-h-svh flex flex-col items-center justify-center px-6 text-center">
        <p className="text-sm text-sub mb-4">그룹 정보를 찾을 수 없어요</p>
        <button onClick={() => navigate('/group')} className="text-sm text-brand font-semibold">
          그룹으로 돌아가기
        </button>
      </div>
    );
  }

  const todayIndex = dayIndexOf(group);
  const trackedDay = dailyHistory.length + 1;
  const todayRate = achievementRate(state.missions);

  const days = Array.from({ length: TOTAL_DAYS }, (_, i) => {
    const day = i + 1;
    if (day < trackedDay) {
      const entry = dailyHistory[i];
      return { day, state: 'past', rate: entry.rate };
    }
    if (day === trackedDay && day <= todayIndex) {
      return { day, state: 'today', rate: todayRate };
    }
    return { day, state: 'upcoming', rate: null };
  });

  const completedDays = dailyHistory.filter((d) => d.rate >= 100).length;
  const historyAvg = dailyHistory.length > 0 ? Math.round(dailyHistory.reduce((sum, d) => sum + d.rate, 0) / dailyHistory.length) : null;

  return (
    <div className="min-h-svh px-5 pt-8 pb-10">
      <button onClick={() => navigate(-1)} className="text-sub text-sm mb-4 text-left w-fit">
        ← 뒤로
      </button>

      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Progress</p>
      <h1 className="text-lg font-bold text-ink mb-1">{group.name} 진행 현황</h1>
      <p className="text-sm text-sub mb-6">1~7일차 달성 기록을 확인해보세요</p>

      <div className="rounded-2xl border border-gray-300 bg-cream grid grid-cols-3 divide-x divide-gray-300 mb-6 shadow-card">
        <div className="px-2 py-4 text-center">
          <p className="text-xs text-sub mb-1">진행일</p>
          <p className="text-lg font-bold text-ink">{Math.min(todayIndex, TOTAL_DAYS)}일</p>
        </div>
        <div className="px-2 py-4 text-center">
          <p className="text-xs text-sub mb-1">완주일</p>
          <p className="text-lg font-bold text-ink">{completedDays}일</p>
        </div>
        <div className="px-2 py-4 text-center">
          <p className="text-xs text-sub mb-1">평균 달성률</p>
          <p className="text-lg font-bold text-ink">{historyAvg ?? '-'}{historyAvg != null && '%'}</p>
        </div>
      </div>

      <div className="space-y-2">
        {days.map(({ day, state: dayState, rate }) => {
          if (dayState === 'upcoming') {
            return (
              <div
                key={day}
                className="rounded-2xl border border-dashed border-black/10 px-4 py-3.5 flex items-center gap-3 text-sub"
              >
                <span className="w-9 h-9 flex items-center justify-center rounded-full bg-black/5 text-sm font-semibold flex-shrink-0">
                  {day}
                </span>
                <p className="text-sm flex-1">{day}일차</p>
                <span className="text-xs">예정</span>
              </div>
            );
          }

          if (dayState === 'today') {
            return (
              <div
                key={day}
                className="rounded-2xl px-4 py-3.5 flex items-center gap-3 text-white bg-brand shadow-card"
              >
                <span className="w-9 h-9 flex items-center justify-center rounded-full bg-white/20 text-sm font-semibold flex-shrink-0">
                  {day}
                </span>
                <p className="text-sm font-semibold flex-1">{day}일차 · 진행 중</p>
                <span className="text-sm font-bold">{rate}%</span>
              </div>
            );
          }

          const status = dayStatus(rate);
          return (
            <div key={day} className={`rounded-2xl px-4 py-3.5 flex items-center gap-3 ${status.tone}`}>
              <span className="w-9 h-9 flex items-center justify-center rounded-full bg-white text-sm font-semibold flex-shrink-0">
                {day}
              </span>
              <p className="text-sm font-semibold flex-1">{day}일차</p>
              <span className="text-xs font-medium flex items-center gap-1">
                <status.Icon size={13} /> {status.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
