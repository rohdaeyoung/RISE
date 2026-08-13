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

const SWIPE_THRESHOLD = 60;

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
        className={`page-transition min-h-svh ${stage === 'exit' ? 'page-transition-exit' : 'page-transition-enter'}`}
        onAnimationEnd={() => {
          if (stage === 'exit') {
            setDisplayLocation(location);
            setDisplayOutlet(outlet);
            setStage('enter');
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

          <Route path="*" element={<Navigate to={resolveHomeRoute(state)} replace />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
