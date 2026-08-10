import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import BottomNav from './components/BottomNav';
import { resolveHomeRoute, useAppState } from './context/AppContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import GroupEntryPage from './pages/GroupEntryPage';
import GroupCreatePage from './pages/GroupCreatePage';
import GroupJoinPage from './pages/GroupJoinPage';
import OnboardingPage from './pages/OnboardingPage';
import CharacterCreatePage from './pages/CharacterCreatePage';
import MealUploadPage from './pages/MealUploadPage';
import MissionVerifyPage from './pages/MissionVerifyPage';
import MyPage from './pages/MyPage';
import GroupFeedPage from './pages/GroupFeedPage';
import GroupMemberPage from './pages/GroupMemberPage';
import GroupSettingsPage from './pages/GroupSettingsPage';
import GroupProgressPage from './pages/GroupProgressPage';
import RankingPage from './pages/RankingPage';
import ShopPage from './pages/ShopPage';
import SettingsPage from './pages/SettingsPage';

function PhoneShell() {
  return (
    <div className="max-w-md mx-auto min-h-svh bg-cream relative">
      <Outlet />
    </div>
  );
}

function MainLayout() {
  return (
    <>
      <Outlet />
      <BottomNav />
    </>
  );
}

export default function App() {
  const state = useAppState();

  return (
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
  );
}
