import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, ChevronRight, LogOut } from 'lucide-react';
import { DEFAULT_MISSION_HOUR, DEFAULT_MISSION_MINUTE, formatClock } from '../api/missionApi';
import { MAX_GROUP_NAME_LENGTH, useAppDispatch, useAppState } from '../context/AppContext';
import { leaveGroup, updateGroupSettings } from '../api/groupApi';
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
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [confirmingLeave, setConfirmingLeave] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [leaveError, setLeaveError] = useState('');

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

  // 방 설정은 그룹원 전체가 공유하는 값이라 서버에 반드시 보내야 한다.
  // 화면 상태만 바꾸면 나에게만 잠깐 반영됐다가 다음 동기화 때 서버 값으로 되돌아간다.
  // 실패를 그냥 삼키면 "저장됐어요"가 뜬 뒤 15초 뒤 조용히 원래 값으로 되돌아가므로, 성공했을 때만
  // 반영하고 실패하면 사유를 보여준다.
  function handleSave() {
    const trimmedName = name.trim();
    const changedName = trimmedName && trimmedName !== group.name ? trimmedName : null;

    setSaving(true);
    setSaveError('');
    updateGroupSettings({ name: changedName, missionHour, missionMinute })
      .then(() => {
        if (changedName) dispatch({ type: 'SET_GROUP_NAME', name: changedName });
        dispatch({ type: 'SET_MISSION_TIME', missionHour, missionMinute });
        setSaved(true);
        setTimeout(() => navigate('/group'), 600);
      })
      .catch((e) => {
        setSaving(false);
        setSaveError(e?.message || '저장에 실패했어요. 잠시 후 다시 시도해주세요');
      });
  }

  // 서버에서도 탈퇴시키지 않으면 그룹 소속이 그대로 남아, 다음 동기화가 그룹을 도로 불러온다.
  // 실패했는데도 나간 것처럼 화면을 바꾸면, 잠시 뒤 sync()가 그룹을 도로 불러와 "나갔는데 다시
  // 들어와 있다"로 보인다. 성공했을 때만 화면을 바꾼다.
  function handleLeaveGroup() {
    setLeaving(true);
    setLeaveError('');
    leaveGroup()
      .then(() => {
        dispatch({ type: 'LEAVE_GROUP' });
        navigate('/my');
      })
      .catch((e) => {
        setLeaving(false);
        setLeaveError(e?.message || '나가기에 실패했어요. 잠시 후 다시 시도해주세요');
      });
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

      {saveError && <p className="text-xs text-warn text-center mb-3">{saveError}</p>}

      <button
        onClick={handleSave}
        disabled={saving || saved}
        className="w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold shadow-card mb-8 flex items-center justify-center gap-1.5 transition-colors"
      >
        {saved && <CheckCircle2 size={16} />}
        {saved ? '저장됐어요' : saving ? '저장하는 중...' : '저장하기'}
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

            {leaveError && (
              <p className="text-center text-sm text-warn bg-warn-soft rounded-2xl px-4 py-3 mb-4">{leaveError}</p>
            )}

            <div className="space-y-2">
              <button
                onClick={handleLeaveGroup}
                disabled={leaving}
                className="w-full bg-warn disabled:bg-warn/50 text-white rounded-full py-3.5 text-sm font-semibold"
              >
                {leaving ? '나가는 중...' : '나가기'}
              </button>
              <button
                onClick={() => setConfirmingLeave(false)}
                disabled={leaving}
                className="w-full text-sub text-sm py-2"
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
