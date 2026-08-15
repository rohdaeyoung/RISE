import { Navigate, Outlet, Route, Routes, useLocation, useNavigate, useOutlet } from 'react-router-dom';
import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import BottomNav, { TABS } from './components/BottomNav';
import LoadingScreen from './components/LoadingScreen';
import { resolveHomeRoute, useAppState } from './context/AppContext';

// 화면과 화면 사이 전환에서 로딩 화면(Suspense fallback)이 뜨도록 페이지를 전부 지연 로딩한다.
const LoginPage = lazy(() => import('./pages/LoginPage'));
const SignupPage = lazy(() => import('./pages/SignupPage'));
const GroupEntryPage = lazy(() => import('./pages/GroupEntryPage'));
const GroupCreatePage = lazy(() => import('./pages/GroupCreatePage'));
const GroupJoinPage = lazy(() => import('./pages/GroupJoinPage'));
const OnboardingPage = lazy(() => import('./pages/OnboardingPage'));
const CharacterCreatePage = lazy(() => import('./pages/CharacterCreatePage'));
const MealUploadPage = lazy(() => import('./pages/MealUploadPage'));
const MissionVerifyPage = lazy(() => import('./pages/MissionVerifyPage'));
const MyPage = lazy(() => import('./pages/MyPage'));
const GroupFeedPage = lazy(() => import('./pages/GroupFeedPage'));
const GroupMemberPage = lazy(() => import('./pages/GroupMemberPage'));
const GroupSettingsPage = lazy(() => import('./pages/GroupSettingsPage'));
const GroupProgressPage = lazy(() => import('./pages/GroupProgressPage'));
const RankingPage = lazy(() => import('./pages/RankingPage'));
const ShopPage = lazy(() => import('./pages/ShopPage'));
const SettingsPage = lazy(() => import('./pages/SettingsPage'));

function PhoneShell() {
  return (
    <div className="max-w-md mx-auto min-h-svh bg-cream relative">
      <Outlet />
    </div>
  );
}

// 로그인 없이 주소를 직접 쳐서 내부 화면에 들어가는 걸 막는다. "/"와 "/signup"을 제외한
// 모든 라우트가 이 가드를 거친다.
function RequireAuth() {
  const state = useAppState();
  if (!state.auth.userId) return <Navigate to="/" replace />;
  return <Outlet />;
}

const SWIPE_THRESHOLD = 60;
// index.css의 .page-transition-exit 재생 시간과 맞춘다 (여기가 더 짧으면 fade-out이 잘린다).
const EXIT_ANIMATION_MS = 120;

function MainLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const outlet = useOutlet();
  const touchStart = useRef(null);

  // 탭 전환 시 토스 스타일 크로스페이드(사라짐 → 나타남)를 위해 실제 라우트(location/outlet)와
  // 화면에 그리는 값(displayLocation/displayOutlet)을 한 박자 늦게 동기화한다 — React Router는
  // 라우트가 바뀌면 이전 컴포넌트를 즉시 unmount하므로, exit 애니메이션이 끝날 때까지는
  // 이전 화면을 그대로 붙들고 있어야 fade-out이 재생될 시간이 생긴다.
  const [displayLocation, setDisplayLocation] = useState(location);
  const [displayOutlet, setDisplayOutlet] = useState(outlet);
  const [stage, setStage] = useState('enter');

  useEffect(() => {
    if (location.pathname !== displayLocation.pathname) {
      setStage('exit');
    }
  }, [location, displayLocation]);

  // 화면 교체는 onAnimationEnd에서 일어나는데, 애니메이션이 아예 재생되지 않으면 그 이벤트도
  // 오지 않는다. 그러면 주소만 바뀌고 화면은 이전 탭에 멈춰 하단 탭이 통째로 먹통이 된다.
  // 재생되지 않는 경우가 실제로 있다.
  //   - 사용자가 "동작 줄이기"를 켠 경우 (index.css에서 animation: none 처리)
  //   - 브라우저가 백그라운드 탭의 애니메이션을 멈춘 경우
  // 그래서 애니메이션은 연출로만 두고, 화면 교체 자체는 시간이 지나면 반드시 일어나게 한다.
  useEffect(() => {
    if (stage !== 'exit') return undefined;
    const timer = setTimeout(() => {
      setDisplayLocation(location);
      setDisplayOutlet(outlet);
      setStage('enter');
    }, EXIT_ANIMATION_MS + 60);
    return () => clearTimeout(timer);
  }, [stage, location, outlet]);

  const handleTouchStart = (e) => {
    const t = e.touches[0];
    touchStart.current = { x: t.clientX, y: t.clientY };
  };

  const handleTouchEnd = (e) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start) return;

    const t = e.changedTouches[0];
    const dx = t.clientX - start.x;
    const dy = t.clientY - start.y;
    if (Math.abs(dx) < SWIPE_THRESHOLD || Math.abs(dx) < Math.abs(dy)) return;

    const currentIndex = TABS.findIndex((tab) => tab.to === location.pathname);
    if (currentIndex === -1) return;

    const nextIndex = dx < 0 ? currentIndex + 1 : currentIndex - 1;
    if (nextIndex < 0 || nextIndex >= TABS.length) return;

    navigate(TABS[nextIndex].to);
  };

  return (
    <>
      <div
        key={displayLocation.pathname}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        className={`page-transition min-h-svh ${
          stage === 'exit' ? 'page-transition-exit' : stage === 'enter' ? 'page-transition-enter' : ''
        }`}
        onAnimationEnd={() => {
          if (stage === 'exit') {
            setDisplayLocation(location);
            setDisplayOutlet(outlet);
            setStage('enter');
          } else if (stage === 'enter') {
            // 애니메이션이 끝나도 transform: scale(1)이 그대로 남아있으면(animation-fill-mode: forwards)
            // 이 div가 새로운 stacking context를 만들어서, 그 안의 fixed 모달(댓글 시트 등)이
            // 바깥의 BottomNav 뒤로 가려진다. 애니메이션이 끝나면 클래스를 완전히 떼어내 transform을 지운다.
            setStage('idle');
          }
        }}
      >
        {displayOutlet}
      </div>
      <BottomNav />
    </>
  );
}

export default function App() {
  const state = useAppState();

  return (
    <Suspense fallback={<LoadingScreen />}>
      <Routes>
        <Route element={<PhoneShell />}>
          <Route path="/" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route element={<RequireAuth />}>
            <Route path="/group-entry" element={<GroupEntryPage />} />
            <Route path="/group-entry/create" element={<GroupCreatePage />} />
            <Route path="/group-entry/join" element={<GroupJoinPage />} />
            <Route path="/onboarding" element={<OnboardingPage />} />
            <Route path="/character" element={<CharacterCreatePage />} />
            <Route path="/meal/:mealKey" element={<MealUploadPage />} />
            <Route path="/mission/:missionId" element={<MissionVerifyPage />} />
            <Route path="/group/member/:memberId" element={<GroupMemberPage />} />
            <Route path="/group/settings" element={<GroupSettingsPage />} />
            <Route path="/group/progress" element={<GroupProgressPage />} />
            <Route path="/settings" element={<SettingsPage />} />

            <Route element={<MainLayout />}>
              <Route path="/my" element={<MyPage />} />
              <Route path="/group" element={<GroupFeedPage />} />
              <Route path="/ranking" element={<RankingPage />} />
              <Route path="/shop" element={<ShopPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to={resolveHomeRoute(state)} replace />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
