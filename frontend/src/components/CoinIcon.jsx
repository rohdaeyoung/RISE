import { useId } from 'react';

// 코인 이모지(🪙)가 기기별로 깨져 보이는 문제가 있어 대신 쓰는 작은 금화 아이콘.
export default function CoinIcon({ className = 'w-4 h-4' }) {
  const gradId = useId();

  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <defs>
        <linearGradient id={gradId} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#ffe37e" />
          <stop offset="100%" stopColor="#f2a922" />
        </linearGradient>
      </defs>
      <circle cx="12" cy="12" r="10" fill={`url(#${gradId})`} stroke="#c97f1a" strokeWidth="1.4" />
      <circle cx="12" cy="12" r="7.2" fill="none" stroke="#fff4d6" strokeWidth="1" opacity="0.7" />
      <text
        x="12"
        y="15.8"
        textAnchor="middle"
        fontSize="10.5"
        fontWeight="800"
        fill="#8a5410"
        fontFamily="ui-sans-serif, system-ui, sans-serif"
      >
        ₩
      </text>
    </svg>
  );
}
