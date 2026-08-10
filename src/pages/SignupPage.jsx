import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signUp } from '../api/authApi';
import { useAppDispatch } from '../context/AppContext';

const MIN_PASSWORD_LENGTH = 8;

export default function SignupPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const passwordTooShort = password.length > 0 && password.length < MIN_PASSWORD_LENGTH;
  const passwordMismatch = passwordConfirm.length > 0 && password !== passwordConfirm;
  const canSubmit =
    email.trim().length > 0 &&
    password.length >= MIN_PASSWORD_LENGTH &&
    password === passwordConfirm &&
    !loading;

  function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) return;
    setLoading(true);
    setError('');
    signUp({ email: email.trim(), password })
      .then(({ userId, email: signedEmail }) => {
        dispatch({ type: 'LOGIN_SUCCESS', userId, email: signedEmail });
        // 새로 가입한 계정은 항상 캐릭터가 없는 상태이므로 바로 캐릭터 선택으로.
        navigate('/character');
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  return (
    <div className="min-h-svh flex flex-col justify-center px-6 py-10">
      <div className="text-center mb-10">
        <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-2">Join WITHU</p>
        <h1 className="text-2xl font-bold text-ink">회원가입</h1>
        <p className="text-sm text-sub mt-2">이메일로 가입하고 바로 시작해요</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label className="text-xs font-medium text-sub mb-1 block">이메일</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand"
          />
        </div>
        <div>
          <label className="text-xs font-medium text-sub mb-1 block">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={`${MIN_PASSWORD_LENGTH}자 이상`}
            className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand"
          />
          {passwordTooShort && (
            <p className="text-xs text-warn mt-1 px-1">비밀번호는 {MIN_PASSWORD_LENGTH}자 이상이어야 해요</p>
          )}
        </div>
        <div>
          <label className="text-xs font-medium text-sub mb-1 block">비밀번호 확인</label>
          <input
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            placeholder="비밀번호를 다시 입력해주세요"
            className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand"
          />
          {passwordMismatch && <p className="text-xs text-warn mt-1 px-1">비밀번호가 일치하지 않아요</p>}
        </div>

        {error && <p className="text-xs text-warn px-1">{error}</p>}

        <button
          type="submit"
          disabled={!canSubmit}
          className="w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold mt-4 shadow-card disabled:shadow-none transition-colors"
        >
          {loading ? '가입 중...' : '회원가입'}
        </button>
      </form>

      <Link to="/" className="mt-6 text-xs text-sub underline underline-offset-2 text-center">
        이미 계정이 있으신가요? 로그인
      </Link>
    </div>
  );
}
