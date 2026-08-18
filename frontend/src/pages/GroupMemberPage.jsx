import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Check, CheckCircle2, Circle } from 'lucide-react';
import { memberColorIndex, memberLabel, useAppState } from '../context/AppContext';
import { fetchGroupMember } from '../api/groupApi';
import CharacterAvatar from '../components/CharacterAvatar';

// 미션 목록 한 덩어리 — 식단/생활습관 두 번 쓰인다 (PRD 6.12).
function MissionList({ title, missions }) {
  if (!missions?.length) return null;
  return (
    <div className="mb-5 last:mb-0">
      <p className="text-xs text-sub mb-2">{title}</p>
      <ul className="space-y-2">
        {missions.map((m, i) => (
          <li key={i} className="flex items-start gap-2 text-sm">
            {m.done ? (
              <Check size={16} className="text-brand-dark mt-0.5 flex-shrink-0" />
            ) : (
              <Circle size={16} className="text-sub/40 mt-0.5 flex-shrink-0" />
            )}
            <span className={m.done ? 'text-ink' : 'text-sub'}>{m.title}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function GroupMemberPage() {
  const { memberId } = useParams();
  const state = useAppState();
  const navigate = useNavigate();
  const member = state.group?.members.find((m) => m.id === memberId);
  const [detail, setDetail] = useState(null);

  // 미션 수행 결과는 그룹 목록 응답에 없어서 그룹원 프로필 API로 따로 받아온다.
  // 실패하면 아래 기본 화면(캐릭터 + 인증 여부)만 보여주면 되므로 에러는 삼킨다.
  useEffect(() => {
    let alive = true;
    fetchGroupMember(memberId)
      .then((d) => {
        if (alive) setDetail(d);
      })
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, [memberId]);

  if (!member) {
    return (
      <div className="min-h-svh flex flex-col items-center justify-center px-6 text-center">
        <p className="text-sm text-sub mb-4">그룹원 정보를 찾을 수 없어요</p>
        <button onClick={() => navigate('/group')} className="text-sm text-brand font-semibold">
          그룹으로 돌아가기
        </button>
      </div>
    );
  }

  const rate = detail?.achievementRate ?? member.achievementRate ?? 0;
  const hasMissions = (detail?.dietMissions?.length ?? 0) + (detail?.lifestyleMissions?.length ?? 0) > 0;
  // 서버가 오늘 인증 사진을 주면 그게 사실이다. mock 모드에서만 로컬 상태로 판단한다.
  const verifiedToday = detail ? Boolean(detail.photo) : Boolean(member.achievedToday);

  return (
    <div className="min-h-svh px-5 pt-8 pb-10">
      <button onClick={() => navigate(-1)} className="text-sub text-sm mb-6 text-left w-fit">
        ← 뒤로
      </button>

      <div className="flex flex-col items-center gap-3 mb-6 py-8 rounded-3xl bg-brand-soft/60">
        <CharacterAvatar
          species={detail?.species ?? member.species}
          expression={detail?.expression ?? member.expression}
          outfit={detail?.outfit ?? member.outfit}
          size="lg"
          accentIndex={memberColorIndex(member.id, state.group?.members)}
        />
        <p className="text-sm font-semibold text-brand-dark">{memberLabel(member, false)}</p>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-4">
        <div className="bg-card rounded-2xl border border-gray-300 shadow-card px-4 py-4">
          <p className="text-xs text-sub mb-1">오늘 달성률</p>
          <p className="text-lg font-bold text-ink">{rate}%</p>
        </div>
        <div className="bg-card rounded-2xl border border-gray-300 shadow-card px-4 py-4">
          <p className="text-xs text-sub mb-1">이번 챌린지 점수</p>
          <p className="text-lg font-bold text-ink">{detail?.points ?? member.points ?? 0}점</p>
        </div>
      </div>

      <div className="bg-card rounded-2xl border border-gray-300 shadow-card p-5">
        <p className="text-xs text-sub mb-1">오늘 인증 여부</p>
        <p
          className={`text-sm font-semibold mb-5 flex items-center gap-1.5 ${
            verifiedToday ? 'text-brand-dark' : 'text-ink'
          }`}
        >
          {verifiedToday ? (
            <>
              <CheckCircle2 size={16} /> {member.lastMealLabel ?? ''} 인증 완료
            </>
          ) : (
            '아직 인증 전이에요'
          )}
        </p>

        {hasMissions ? (
          <>
            <MissionList title="식단 미션" missions={detail.dietMissions} />
            <MissionList title="건강 미션" missions={detail.lifestyleMissions} />
          </>
        ) : (
          <p className="text-xs text-sub">아직 오늘의 미션이 없어요</p>
        )}
      </div>
    </div>
  );
}
