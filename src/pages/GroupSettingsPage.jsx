import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, ChevronRight, LogOut } from 'lucide-react';
import { DEFAULT_MISSION_HOUR, DEFAULT_MISSION_MINUTE, formatClock } from '../api/missionApi';
import { MAX_GROUP_NAME_LENGTH, useAppDispatch, useAppState } from '../context/AppContext';
import MissionTimePicker from '../components/MissionTimePicker';

export default function GroupSettingsPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const group = state.group;

  const [name, setName] = useState(group?.name ?? '');
  const [missionHour, setMissionHour] = useState(group?.missionHour ?? DEFAULT_MISSION_HOUR);
  const [missionMinute, setMissionMinute] = useState(group?.missionMinute ?? DEFAULT_MISSION_MINUTE);
  const [saved, setSaved] = useState(false);
  const [confirmingLeave, setConfirmingLeave] = useState(false);

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

  const currentLabel = formatClock(group.missionHour * 60 + group.missionMinute).label;

  function handleSave() {
    const trimmedName = name.trim();
    if (trimmedName && trimmedName !== group.name) {
      dispatch({ type: 'SET_GROUP_NAME', name: trimmedName });
    }
    dispatch({ type: 'SET_MISSION_TIME', missionHour, missionMinute });
    setSaved(true);
    setTimeout(() => navigate('/group'), 600);
  }

  function handleLeaveGroup() {
    dispatch({ type: 'LEAVE_GROUP' });
    navigate('/my');
  }

  return (
    <div className="min-h-svh px-5 pt-8 pb-10">
      <button onClick={() => navigate(-1)} className="text-sub text-sm mb-6 text-left w-fit">
        ← 뒤로
      </button>

      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Group Settings</p>
      <h1 className="text-xl font-bold text-ink mb-1">방 설정</h1>
      <p className="text-sm text-sub mb-6">현재 미션 시작 시간: {currentLabel}</p>

      <h2 className="text-sm font-semibold text-ink mb-1">방 이름 변경</h2>
      <p className="text-xs text-sub mb-4">그룹원 누구나 바꿀 수 있어요.</p>
      <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value.slice(0, MAX_GROUP_NAME_LENGTH))}
        placeholder={group.name}
        maxLength={MAX_GROUP_NAME_LENGTH}
        className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand mb-8"
      />

      <h2 className="text-sm font-semibold text-ink mb-1">미션 시작 시간 변경</h2>
      <p className="text-xs text-sub mb-4">
        이 시간부터 24시간이 챌린지 하루예요. 그룹원 누구나 바꿀 수 있고, 바꾸면 다음 미션부터 적용돼요.
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
        onClick={handleSave}
        className="w-full bg-brand text-white rounded-full py-4 text-sm font-semibold shadow-card mb-8 flex items-center justify-center gap-1.5"
      >
        {saved && <CheckCircle2 size={16} />}
        {saved ? '저장됐어요' : '저장하기'}
      </button>

      <div className="rounded-2xl border border-warn/20 bg-warn-soft overflow-hidden">
        <button
          onClick={() => setConfirmingLeave(true)}
          className="w-full flex items-center justify-between px-4 py-4 text-sm font-medium text-warn"
        >
          그룹 나가기
          <ChevronRight size={16} />
        </button>
      </div>

      {confirmingLeave && (
        <div className="fixed inset-0 bg-black/50 flex items-end justify-center z-50">
          <div className="w-full max-w-md bg-card rounded-t-sheet p-6 pb-8 shadow-modal">
            <div className="w-10 h-1.5 rounded-full bg-black/10 mx-auto mb-5" />
            <div className="text-center mb-6">
              <LogOut size={30} className="mx-auto mb-3 text-warn" />
              <h2 className="text-lg font-bold text-ink">정말 그룹을 나가시겠어요?</h2>
              <p className="text-sm text-sub mt-2">
                지금까지의 미션·랭킹 기록이 사라지고
                <br />
                다시 코드로 참여해야 돌아올 수 있어요
              </p>
            </div>

            <div className="space-y-2">
              <button
                onClick={handleLeaveGroup}
                className="w-full bg-warn text-white rounded-full py-3.5 text-sm font-semibold"
              >
                나가기
              </button>
              <button onClick={() => setConfirmingLeave(false)} className="w-full text-sub text-sm py-2">
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
