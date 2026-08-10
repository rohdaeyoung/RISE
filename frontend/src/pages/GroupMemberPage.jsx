import { useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import { memberColorIndex, memberLabel, useAppState } from '../context/AppContext';
import CharacterAvatar from '../components/CharacterAvatar';

export default function GroupMemberPage() {
  const { memberId } = useParams();
  const state = useAppState();
  const navigate = useNavigate();
  const member = state.group?.members.find((m) => m.id === memberId);

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

  return (
    <div className="min-h-svh px-5 pt-8 pb-10">
      <button onClick={() => navigate(-1)} className="text-sub text-sm mb-6 text-left w-fit">
        ← 뒤로
      </button>

      <div className="flex flex-col items-center gap-3 mb-8 py-8 rounded-3xl bg-brand-soft/60">
        <CharacterAvatar
          species={member.species}
          expression={member.expression}
          outfit={member.outfit}
          size="lg"
          accentIndex={memberColorIndex(member.id, state.group?.members)}
        />
        <p className="text-sm font-semibold text-brand-dark">{memberLabel(member, false)}</p>
      </div>

      <div className="bg-card rounded-2xl border border-gray-300 shadow-card p-5">
        <p className="text-xs text-sub mb-1">오늘 인증 여부</p>
        <p className={`text-sm font-semibold mb-4 flex items-center gap-1.5 ${member.achievedToday ? 'text-brand-dark' : 'text-ink'}`}>
          {member.achievedToday ? (
            <>
              <CheckCircle2 size={16} /> {member.lastMealLabel ?? ''} 인증 완료
            </>
          ) : (
            '아직 인증 전이에요'
          )}
        </p>
      </div>
    </div>
  );
}
