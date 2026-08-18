import { Link } from 'react-router-dom';
import { CheckCircle2, Sprout, Utensils } from 'lucide-react';

export default function MissionCard({ mission, nextMealKey, isPendingDiet }) {
  const isDiet = mission.type === 'diet';

  return (
    <div
      className={`flex items-center gap-3 rounded-2xl px-4 py-3.5 border transition-colors ${
        mission.done
          ? 'bg-brand-soft border-brand/20'
          : 'bg-card border-gray-300 shadow-card'
      }`}
    >
      <span
        className={`w-9 h-9 flex items-center justify-center rounded-full flex-shrink-0 ${
          mission.done ? 'bg-white text-brand-dark' : 'bg-cream text-sub'
        }`}
      >
        {isDiet ? <Utensils size={17} /> : <Sprout size={17} />}
      </span>
      <div className="flex-1 text-left min-w-0">
        <p className={`text-sm font-medium truncate ${mission.done ? 'text-brand-dark' : 'text-ink'}`}>
          {mission.title}
        </p>
        <p className="text-xs text-sub">{mission.done ? '완료' : isDiet ? '식단 미션' : '생활습관 미션'}</p>
      </div>

      {mission.done ? (
        <CheckCircle2 size={20} className="text-brand flex-shrink-0" />
      ) : isDiet ? (
        !isPendingDiet ? (
          // 식단 미션은 "미완료 중 첫 번째"만 실제로 판정 대상이라(서버 규칙과 동일),
          // 차례가 안 된 미션은 눌러도 앞 미션이 인증되어 제목이 어긋나 보인다 — 아예 못 누르게 막는다.
          <span className="text-xs text-sub whitespace-nowrap">차례를 기다리는 중</span>
        ) : nextMealKey ? (
          <Link
            to={`/meal/${nextMealKey}`}
            className="text-xs font-semibold text-brand bg-brand/10 rounded-full px-3 py-1.5 whitespace-nowrap"
          >
            사진 인증
          </Link>
        ) : (
          <span className="text-xs text-sub whitespace-nowrap">오늘 식사 완료</span>
        )
      ) : (
        <Link
          to={`/mission/${mission.id}`}
          className="text-xs font-semibold text-brand bg-brand/10 rounded-full px-3 py-1.5 whitespace-nowrap"
        >
          사진 인증
        </Link>
      )}
    </div>
  );
}
