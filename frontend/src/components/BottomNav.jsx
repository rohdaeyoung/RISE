import { NavLink } from 'react-router-dom';
import { ShoppingBag, Trophy, User, Users } from 'lucide-react';

const TABS = [
  { to: '/my', label: '마이', Icon: User },
  { to: '/group', label: '그룹', Icon: Users },
  { to: '/ranking', label: '랭킹', Icon: Trophy },
  { to: '/shop', label: '상점', Icon: ShoppingBag },
];

export default function BottomNav() {
  return (
    <nav className="fixed bottom-3 inset-x-0 px-4 max-w-md mx-auto flex">
      <div className="flex-1 flex bg-card/95 backdrop-blur rounded-[28px] px-2 py-2 shadow-card border border-gray-300">
        {TABS.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex-1 flex flex-col items-center gap-0.5 py-2 rounded-2xl text-[11px] font-medium transition-colors ${
                isActive ? 'bg-brand-soft text-brand-dark' : 'text-sub'
              }`
            }
          >
            <Icon size={20} strokeWidth={2.2} />
            {label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
