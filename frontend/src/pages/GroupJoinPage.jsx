import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound } from 'lucide-react';
import { joinGroup } from '../api/groupApi';
import { useAppDispatch, useAppState } from '../context/AppContext';

export default function GroupJoinPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function handleJoin() {
    if (!code.trim()) return;
    setLoading(true);
    setError('');
    joinGroup({ code, myUserId: state.auth.userId })
      .then((group) => {
        dispatch({
          type: 'SET_GROUP',
          id: group.id,
          code: group.code,
          name: group.name,
          members: group.members,
          missionHour: group.missionHour,
          missionMinute: group.missionMinute,
        });
        // 팀원은 코드로 그룹에 참여한 다음에 목표/신체정보 온보딩을 진행함.
        navigate('/onboarding');
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  return (
    <div className="relative min-h-svh flex flex-col justify-center px-6 py-10">
      <button onClick={() => navigate(-1)} className="absolute top-6 left-6 text-sub text-sm">
        ← 뒤로
      </button>

      <div className="text-center mb-10">
        <KeyRound size={36} className="mx-auto mb-3 text-brand" />
        <h1 className="text-xl font-bold text-ink">그룹 코드를 입력해주세요</h1>
        <p className="text-sm text-sub mt-2">친구에게 받은 6자리 코드를 입력하면 참여돼요</p>
      </div>

      <input
        value={code}
        onChange={(e) => setCode(e.target.value.toUpperCase())}
        placeholder="예: AB12CD"
        maxLength={6}
        className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink text-center font-mono tracking-widest outline-none focus:border-brand transition-colors"
      />
      {error && <p className="text-xs text-warn mt-2 px-1">{error}</p>}

      <button
        onClick={handleJoin}
        disabled={!code.trim() || loading}
        className="mt-6 w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold shadow-card disabled:shadow-none transition-colors"
      >
        {loading ? '참여 중...' : '참여하기'}
      </button>
    </div>
  );
}
