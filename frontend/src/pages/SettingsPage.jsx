import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, ChevronRight, Frown } from 'lucide-react';
import { MAX_NICKNAME_LENGTH, useAppDispatch, useAppState } from '../context/AppContext';
import { saveNickname } from '../api/profileApi';
import { deleteAccount, logout } from '../api/authApi';

export default function SettingsPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [nickname, setNickname] = useState(state.nickname ?? '');
  const [nicknameSaved, setNicknameSaved] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  function handleLogout() {
    // 로컬 상태만 지우면 접근 토큰이 기기에 그대로 남는다. 공용 기기에서 다음 사람이
    // 그 토큰으로 계정에 접근할 수 있으므로 반드시 함께 지운다.
    logout();
    dispatch({ type: 'LOGOUT' });
    navigate('/');
  }

  // 서버에서 지워야 진짜로 지워진다. 로컬만 비우면 화면에서만 사라지고 DB에는 계정이 남아,
  // 같은 이메일로 재가입이 막히고 전체 랭킹에도 계속 나온다.
  function handleDeleteAccount() {
    if (deleting) return;
    setDeleting(true);
    setDeleteError('');
    deleteAccount(state.auth.email)
      .then(() => {
        dispatch({ type: 'DELETE_ACCOUNT' });
        navigate('/');
      })
      .catch((e) => {
        // 삭제에 실패했는데 화면만 로그아웃시키면 지워진 줄 알게 된다. 실패를 그대로 보여준다.
        setDeleting(false);
        setDeleteError(e?.message || '탈퇴에 실패했어요. 잠시 후 다시 시도해주세요');
      });
  }

  function handleSaveNickname() {
    saveNickname(nickname).catch(() => {});
    dispatch({ type: 'SET_NICKNAME', nickname });
    setNicknameSaved(true);
    setTimeout(() => setNicknameSaved(false), 1200);
  }

  return (
    <div className="min-h-svh px-5 pt-8 pb-10">
      <button onClick={() => navigate(-1)} className="text-sub text-sm mb-6 text-left w-fit">
        ← 뒤로
      </button>

      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Settings</p>
      <h1 className="text-xl font-bold text-ink mb-6">설정</h1>

      <div className="rounded-2xl border border-gray-300 bg-card shadow-card px-4 py-4 mb-4">
        <p className="text-xs text-sub mb-1">계정</p>
        <p className="text-sm font-semibold text-ink">{state.auth.email}</p>
      </div>

      <div className="rounded-2xl border border-gray-300 bg-card shadow-card px-4 py-4 mb-6">
        <p className="text-xs text-sub mb-2">닉네임</p>
        <p className="text-[11px] text-sub mb-3">그룹 피드·랭킹에서 나를 대신 표시해요</p>
        <div className="flex gap-2">
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value.slice(0, MAX_NICKNAME_LENGTH))}
            placeholder="닉네임을 입력해주세요"
            maxLength={MAX_NICKNAME_LENGTH}
            className="flex-1 min-w-0 rounded-xl bg-cream border border-gray-300 px-3.5 py-2.5 text-sm text-ink outline-none focus:border-brand"
          />
          <button
            onClick={handleSaveNickname}
            disabled={!nickname.trim()}
            className="flex-shrink-0 flex items-center gap-1 px-4 rounded-xl bg-brand disabled:bg-black/10 disabled:text-sub text-white text-sm font-semibold transition-colors"
          >
            {nicknameSaved && <Check size={14} />}
            {nicknameSaved ? '저장됨' : '저장'}
          </button>
        </div>
      </div>

      <div className="rounded-2xl border border-gray-300 bg-card shadow-card overflow-hidden mb-3">
        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-between px-4 py-4 text-sm font-medium text-ink"
        >
          로그아웃
          <ChevronRight size={16} className="text-sub" />
        </button>
      </div>

      <div className="rounded-2xl border border-warn/20 bg-warn-soft overflow-hidden">
        <button
          onClick={() => setConfirmingDelete(true)}
          className="w-full flex items-center justify-between px-4 py-4 text-sm font-medium text-warn"
        >
          계정 탈퇴
          <ChevronRight size={16} />
        </button>
      </div>

      {confirmingDelete && (
        <div className="fixed inset-0 bg-black/50 flex items-end justify-center z-50">
          <div className="w-full max-w-md bg-card rounded-t-sheet p-6 pb-8 shadow-modal">
            <div className="w-10 h-1.5 rounded-full bg-black/10 mx-auto mb-5" />
            <div className="text-center mb-6">
              <Frown size={30} className="mx-auto mb-3 text-warn" />
              <h2 className="text-lg font-bold text-ink">정말 탈퇴하시겠어요?</h2>
              <p className="text-sm text-sub mt-2">
                캐릭터, 그룹, 미션 기록을 포함한 모든 데이터가
                <br />
                삭제되고 되돌릴 수 없어요
              </p>
            </div>

            {deleteError && (
              <p className="text-center text-sm text-warn bg-warn-soft rounded-2xl px-4 py-3 mb-4">{deleteError}</p>
            )}

            <div className="space-y-2">
              <button
                onClick={handleDeleteAccount}
                disabled={deleting}
                className="w-full bg-warn disabled:bg-warn/50 text-white rounded-full py-3.5 text-sm font-semibold"
              >
                {deleting ? '탈퇴하는 중...' : '탈퇴하기'}
              </button>
              <button
                onClick={() => setConfirmingDelete(false)}
                disabled={deleting}
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
