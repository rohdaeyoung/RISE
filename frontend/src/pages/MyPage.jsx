import { Link } from 'react-router-dom';
import { Clock, Flame, Settings, Sprout } from 'lucide-react';
import { achievementRate, currentStreak, expressionForRanking, MEAL_LABELS, useAppState } from '../context/AppContext';
import { nextUpcomingMission, visibleMissions } from '../api/missionApi';
import CharacterAvatar from '../components/CharacterAvatar';
import CoinIcon from '../components/CoinIcon';
import MissionCard from '../components/MissionCard';

export default function MyPage() {
  const state = useAppState();
  const rate = achievementRate(state.missions);
  const streak = currentStreak(state);
  const nextMealKey = Object.keys(MEAL_LABELS).find((key) => !state.meals[key]);
  const missionsToShow = visibleMissions(state.missions);
  const upcoming = nextUpcomingMission(state.missions);

  return (
    <div className="px-5 pt-8 pb-24">
      <div className="flex items-start justify-between mb-6">
        <div>
          <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">My Page</p>
          <h1 className="text-xl font-bold text-ink">{state.nickname ? `${state.nickname}님 오늘도 화이팅!` : '오늘도 화이팅!'}</h1>
        </div>
        <Link
          to="/settings"
          aria-label="설정"
          className="w-9 h-9 flex items-center justify-center rounded-full bg-card border border-gray-300 shadow-card text-sub flex-shrink-0"
        >
          <Settings size={17} />
        </Link>
      </div>

      {!state.group && (
        <Link
          to="/group-entry"
          className="flex items-center justify-between bg-brand-soft border border-brand/15 rounded-2xl px-4 py-3.5 mb-6 text-sm"
        >
          <span className="text-brand-dark font-semibold">친구들과 함께 해볼까요?</span>
          <span className="text-brand-dark">그룹 만들기 →</span>
        </Link>
      )}

      {/* 캐릭터는 카드/박스에 담기지 않고 페이지 배경 위에 바로 서 있음 */}
      <div className="relative flex flex-col items-center pt-2 pb-5">
        <div aria-hidden className="absolute top-6 w-56 h-56 rounded-full bg-brand-soft blur-2xl -z-10" />
        <CharacterAvatar
          species={state.character.species}
          expression={state.group ? expressionForRanking(state) : 'normal'}
          size="hero"
          outfit={state.character.outfit}
          framed={false}
        />
        <div aria-hidden className="mt-1 w-24 h-4 rounded-full bg-black/10 blur-sm" />
      </div>

      <div className="grid grid-cols-2 gap-3 mb-3">
        <div className="rounded-2xl px-4 py-4 text-white bg-brand shadow-card">
          <p className="text-xs text-white/80 mb-1">오늘 달성률</p>
          <p className="text-xl font-bold mb-2">{rate}%</p>
          <div className="w-full h-1.5 bg-white/25 rounded-full overflow-hidden">
            <div className="h-full bg-white rounded-full transition-all" style={{ width: `${rate}%` }} />
          </div>
        </div>
        <div className="rounded-2xl px-4 py-4 bg-brand-soft border border-brand/15 shadow-card">
          <p className="text-xs text-brand-dark/80 mb-1 flex items-center gap-1">
            <Flame size={13} /> 연속 인증
          </p>
          <p className="text-xl font-bold text-brand-dark mb-2">{streak}일</p>
          <p className="text-[11px] text-brand-dark/70">{streak > 0 ? '오늘도 이어가는 중!' : '오늘부터 시작해봐요'}</p>
        </div>
      </div>

      <Link
        to="/shop"
        className="flex items-center justify-between rounded-2xl px-4 py-3 mb-6 bg-card border border-gray-300 shadow-card"
      >
        <span className="text-sm font-medium text-ink flex items-center gap-2">
          <CoinIcon className="w-5 h-5" /> 내 코인
        </span>
        <span className="text-sm font-bold text-ink">
          {state.coins} <span className="text-sub text-xs">›</span>
        </span>
      </Link>

      {state.group ? (
        <>
          <h2 className="text-sm font-semibold text-ink mb-3">오늘의 미션</h2>
          <div className="space-y-2">
            {missionsToShow.length === 0 && !upcoming && (
              <p className="text-sm text-sub text-center py-6">오늘의 미션을 준비하고 있어요</p>
            )}
            {missionsToShow.map((m) => (
              <MissionCard key={m.id} mission={m} nextMealKey={nextMealKey} />
            ))}
            {upcoming && (
              <div className="flex items-center gap-3 rounded-2xl px-4 py-3.5 border border-dashed border-black/10 text-sub">
                <span className="w-9 h-9 flex items-center justify-center rounded-full bg-cream flex-shrink-0">
                  <Clock size={16} />
                </span>
                <p className="text-sm">
                  다음 미션은 <span className="font-semibold text-ink">{upcoming.unlockLabel}</span>에 도착해요
                </p>
              </div>
            )}
          </div>
        </>
      ) : (
        <div className="rounded-2xl border border-dashed border-black/10 px-4 py-8 text-center">
          <Sprout size={26} className="mx-auto mb-2 text-brand" />
          <p className="text-sm text-sub">그룹을 만들면 오늘의 미션이 시작돼요</p>
        </div>
      )}
    </div>
  );
}
