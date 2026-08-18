import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { ShoppingBag, Trophy, User, Users } from 'lucide-react';

export const TABS = [
  { to: '/my', label: '마이', Icon: User },
  { to: '/group', label: '그룹', Icon: Users },
  { to: '/ranking', label: '랭킹', Icon: Trophy },
  { to: '/shop', label: '상점', Icon: ShoppingBag },
];

export default function BottomNav() {
  const [bouncingTab, setBouncingTab] = useState(null);

  return (
    <nav className="fixed bottom-3 inset-x-0 px-4 max-w-md mx-auto flex">
      <div className="flex-1 flex bg-card/95 backdrop-blur rounded-[28px] px-2 py-2 shadow-card border border-gray-300">
        {TABS.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={() => setBouncingTab(to)}
            className={({ isActive }) =>
              `nav-tab flex-1 flex flex-col items-center gap-0.5 py-2 rounded-2xl text-[11px] font-medium transition-all duration-200 active:scale-90 ${
                isActive ? 'bg-brand-soft text-brand-dark' : 'text-sub'
              }`
            }
          >
            <Icon
              size={20}
              strokeWidth={2.2}
              className={bouncingTab === to ? 'nav-icon-bounce' : ''}
              onAnimationEnd={() => setBouncingTab((current) => (current === to ? null : current))}
            />
            {label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
