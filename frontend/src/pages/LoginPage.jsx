import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import { resolveHomeRoute, useAppDispatch, useAppState } from '../context/AppContext';

export default function LoginPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // 세션 유지: 이미 로그인된 상태로 재실행되면 로그인 화면을 건너뜀.
  useEffect(() => {
    if (state.auth.userId) navigate(resolveHomeRoute(state), { replace: true });
  }, []);

  const canSubmit = email.trim().length > 0 && password.length > 0 && !loading;

  function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) return;
    setLoading(true);
    setError('');
    login({ email: email.trim(), password })
      .then(({ userId, email: loggedEmail }) => {
        dispatch({ type: 'LOGIN_SUCCESS', userId, email: loggedEmail });
        navigate(resolveHomeRoute({ ...state, auth: { userId, email: loggedEmail } }));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  return (
    <div className="min-h-svh flex flex-col justify-center px-6 py-10">
      <div className="relative rounded-3xl px-7 py-10 mb-8 text-center text-white bg-brand shadow-card overflow-hidden">
        <div className="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-white/10 blur-xl" />
        <div className="absolute -bottom-14 -left-10 w-36 h-36 rounded-full bg-white/10 blur-xl" />
        <div className="relative">
          <p className="text-xs font-bold tracking-wide text-white/75 uppercase mb-3">Welcome to WITHU</p>
          <h1 className="text-3xl font-extrabold tracking-tight mb-3">WITHU</h1>
          <p className="text-sm text-white/85 leading-relaxed">
            혼자가 아닌,
            <br />
            함께하는 건강 습관
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label className="text-xs font-medium text-sub mb-1 block">이메일</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand transition-colors"
          />
        </div>
        <div>
          <label className="text-xs font-medium text-sub mb-1 block">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호"
            className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand transition-colors"
          />
        </div>

        {error && <p className="text-xs text-warn px-1">{error}</p>}

        <button
          type="submit"
          disabled={!canSubmit}
          className="w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold mt-4 shadow-card disabled:shadow-none transition-colors"
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <Link to="/signup" className="mt-6 text-xs text-sub underline underline-offset-2 text-center">
        아직 계정이 없으신가요? 회원가입
      </Link>
    </div>
  );
}
