import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PartyPopper } from 'lucide-react';
import { createGroup, MIN_MEMBERS, MAX_MEMBERS } from '../api/groupApi';
import { DEFAULT_MISSION_HOUR, DEFAULT_MISSION_MINUTE } from '../api/missionApi';
import { MAX_GROUP_NAME_LENGTH, useAppDispatch } from '../context/AppContext';
import MissionTimePicker from '../components/MissionTimePicker';

export default function GroupCreatePage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [copied, setCopied] = useState(false);
  const [name, setName] = useState('');
  const [missionHour, setMissionHour] = useState(DEFAULT_MISSION_HOUR);
  const [missionMinute, setMissionMinute] = useState(DEFAULT_MISSION_MINUTE);

  useEffect(() => {
    createGroup().then(setGroup);
  }, []);

  function copyCode() {
    navigator.clipboard?.writeText(group.code).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }

  const nameValid = name.trim().length > 0;

  function handleContinue() {
    if (!nameValid) return;
    dispatch({ type: 'SET_GROUP', id: group.id, code: group.code, name, members: group.members, missionHour, missionMinute });
    // 방장은 그룹을 만든 다음에 목표/신체정보 온보딩을 진행함(로그인 직후가 아니라).
    navigate('/onboarding');
  }

  if (!group) {
    return (
      <div className="min-h-svh flex items-center justify-center text-sub text-sm">그룹을 만들고 있어요...</div>
    );
  }

  return (
    <div className="relative min-h-svh flex flex-col justify-center px-6 py-10">
      <button onClick={() => navigate(-1)} className="absolute top-6 left-6 text-sub text-sm">
        ← 뒤로
      </button>

      <div className="text-center mb-8">
        <PartyPopper size={36} className="mx-auto mb-3 text-brand" />
        <h1 className="text-xl font-bold text-ink">그룹이 만들어졌어요</h1>
        <p className="text-sm text-sub mt-2">
          이 코드를 친구에게 공유해서 초대해보세요 ({MIN_MEMBERS}~{MAX_MEMBERS}인)
        </p>
      </div>

      <button
        onClick={copyCode}
        className="flex items-center justify-center gap-3 bg-card border border-gray-300 rounded-2xl py-5 mb-8 shadow-card"
      >
        <span className="font-mono text-2xl font-bold text-ink tracking-widest">{group.code}</span>
        <span className="bg-brand-soft text-brand-dark rounded-full px-3 py-1 text-xs font-medium">
          {copied ? '복사됨!' : '복사'}
        </span>
      </button>

      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Group Name</p>
      <h2 className="text-base font-bold text-ink mb-1">방 이름을 정해주세요</h2>
      <p className="text-xs text-sub mb-4">
        1~{MAX_GROUP_NAME_LENGTH}자로 입력해주세요. 나중에 방 설정에서 바꿀 수 있어요.
      </p>
      <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value.slice(0, MAX_GROUP_NAME_LENGTH))}
        placeholder="예: 건강한 친구들"
        maxLength={MAX_GROUP_NAME_LENGTH}
        className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand mb-8"
      />

      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Mission Start Time</p>
      <h2 className="text-base font-bold text-ink mb-1">미션 시작 시간을 정해주세요</h2>
      <p className="text-xs text-sub mb-4">
        이 시간부터 24시간이 챌린지 하루예요. 그룹 생성 후엔 그룹원 누구나 바꿀 수 있어요.
      </p>
      <div className="mb-8">
        <MissionTimePicker
          hour={missionHour}
          minute={missionMinute}
          onChange={(h, m) => {
            setMissionHour(h);
            setMissionMinute(m);
          }}
        />
      </div>

      <button
        onClick={handleContinue}
        disabled={!nameValid}
        className="w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold shadow-card disabled:shadow-none transition-colors"
      >
        시작하기
      </button>
    </div>
  );
}
