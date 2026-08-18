import { useMemo, useState } from 'react';
import { Medal, Plus } from 'lucide-react';
import { buildRanking, memberColorIndex, useAppState } from '../context/AppContext';
import CharacterAvatar from '../components/CharacterAvatar';

const MAX_SLOTS = 4;
const MEDAL_COLORS = ['#e8b923', '#a9adb4', '#c17a4a'];

// 전체 랭킹은 아직 백엔드에 전체 유저 데이터가 없어 데모용 목업으로 채움(실제 유저 아님).
// 그룹 랭킹(오늘 달성률 %)과 달리 전체 랭킹은 챌린지 누적 점수 기준 — 값은 고정이라 새로고침해도
// 흔들리지 않고, "나"만 실제 누적 코인(점수)으로 매번 다시 섞임.
const MOCK_ALL_USERS = [
  { id: 'mock-1', label: '건강한아침', species: 'tit', score: 265 },
  { id: 'mock-2', label: '오늘도완주', species: 'dino', score: 250 },
  { id: 'mock-3', label: '헬시라이프', species: 'hedgehog', score: 230 },
  { id: 'mock-4', label: '단단한하루', species: 'bat', score: 205 },
  { id: 'mock-5', label: '아침형인간', species: 'tit', score: 180 },
  { id: 'mock-6', label: '걷기왕', species: 'dino', score: 150 },
  { id: 'mock-7', label: '식단챌린저', species: 'hedgehog', score: 120 },
  { id: 'mock-8', label: '느긋하게', species: 'bat', score: 90 },
  { id: 'mock-9', label: '초심자', species: 'tit', score: 55 },
  { id: 'mock-10', label: '천천히꾸준히', species: 'dino', score: 20 },
];

function expressionFromMockScore(score) {
  if (score >= 200) return 'good';
  if (score <= 60) return 'bad';
  return 'normal';
}

const TABS = [
  { key: 'group', label: '그룹 랭킹' },
  { key: 'all', label: '전체 랭킹' },
];

// User Signature Color 배경 톤 — memberColorIndex(0~3)에 대응하는 완전한 클래스 문자열을
// 배열로 나열해야 Tailwind가 정적으로 인식한다(동적 템플릿 문자열은 스캔 안 됨).
const USER_SOFT_STYLE = [
  'bg-user-1/25 border border-user-1',
  'bg-user-2/25 border border-user-2',
  'bg-user-3/25 border border-user-3',
  'bg-user-4/25 border border-user-4',
];

// 나 + 실제로 코드로 참여한 그룹원의 오늘 달성률을 기준으로 실시간 순위를 보여줌.
// 아직 그룹원이 없으면(백엔드 연동 전 기본 상태) 나 혼자 1등으로 표시되는 게 정상.
export default function RankingPage() {
  const state = useAppState();
  const [tab, setTab] = useState('group');
  const groupRanking = buildRanking(state);

  const allRanking = useMemo(() => {
    const me = {
      id: 'me',
      isMe: true,
      label: '나',
      species: state.character.species,
      expression: state.character.expression,
      outfit: state.character.outfit,
      score: state.challengeCoins,
    };
    const mocked = MOCK_ALL_USERS.map((u) => ({ ...u, isMe: false, expression: expressionFromMockScore(u.score) }));
    return [...mocked, me].sort((a, b) => b.score - a.score);
  }, [state.character.species, state.character.expression, state.character.outfit, state.challengeCoins]);

  const ranking = tab === 'group' ? groupRanking : allRanking;
  const emptySlots = tab === 'group' ? Math.max(0, MAX_SLOTS - ranking.length) : 0;

  return (
    <div className="px-5 pt-8 pb-24">
      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Ranking</p>
      <h1 className="text-xl font-bold text-ink mb-1">랭킹</h1>
      <p className="text-sm text-sub mb-4">
        {tab === 'group' ? '오늘 미션 달성률 기준, 실시간으로 바뀌어요' : '챌린지 누적 점수 기준이에요'}
      </p>

      <div className="inline-flex rounded-full bg-black/5 p-1 mb-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-colors ${
              tab === t.key ? 'bg-white text-ink shadow-sm' : 'text-sub'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'all' && <p className="text-[11px] text-sub mb-4">* 데모용 예시 데이터예요</p>}
      {tab === 'group' && <div className="mb-4" />}

      {ranking.map((p, i) => {
        const rank = i + 1;
        const isTop = rank <= 3;
        // 전체 랭킹은 100명 넘게 있을 수도 있어 4위부터는 한 줄짜리 컴팩트 행으로 축소.
        // 그룹 랭킹은 최대 4인이라 항상 큰 카드 그대로 유지.
        const compact = tab === 'all' && !isTop;
        const valueText = tab === 'all' ? `${p.score}점` : `${p.rate}%`;
        const valueLabel = tab === 'all' ? '누적 점수' : '오늘 미션 달성률';
        // User Signature Color는 실제 "우리 그룹" 사람에게만 배정 — 전체 랭킹의 목업 유저는 대상 아님.
        const colorIdx = tab === 'group' ? memberColorIndex(p.id, state.group?.members) : null;

        if (compact) {
          return (
            <div
              key={p.id}
              className={`rounded-xl px-3 py-2 flex items-center gap-3 mb-2 ${
                p.isMe ? 'bg-brand-soft border border-brand/25 text-ink' : 'bg-card border border-gray-300 text-ink'
              }`}
            >
              <span className="w-8 flex-shrink-0 text-center whitespace-nowrap text-xs font-semibold text-sub">
                {rank}위
              </span>
              <CharacterAvatar species={p.species} expression={p.expression} outfit={p.outfit} size="xs" breathing={false} accentIndex={colorIdx} />
              <p className="text-sm font-medium flex-1 truncate">{p.label}</p>
              <p className="text-xs font-bold text-sub">{valueText}</p>
            </div>
          );
        }

        return (
          <div
            key={p.id}
            className={`rounded-2xl px-4 py-4 flex items-center gap-4 mb-3 text-ink shadow-card ${
              p.isMe ? USER_SOFT_STYLE[colorIdx] : 'bg-card border border-gray-300'
            }`}
          >
            <span className="w-9 flex-shrink-0 flex items-center justify-center whitespace-nowrap">
              {isTop ? (
                <Medal size={22} color={MEDAL_COLORS[rank - 1]} fill={MEDAL_COLORS[rank - 1]} fillOpacity={0.25} />
              ) : (
                <span className="text-sm font-semibold">{rank}위</span>
              )}
            </span>
            <CharacterAvatar species={p.species} expression={p.expression} outfit={p.outfit} size="sm" breathing={false} accentIndex={colorIdx} />
            <div className="flex-1">
              <p className="text-sm font-semibold">{p.label}</p>
              <p className="text-[11px] mt-0.5 text-sub">{valueLabel}</p>
            </div>
            <p className="text-sm font-bold">{valueText}</p>
          </div>
        );
      })}

      {Array.from({ length: emptySlots }).map((_, i) => (
        <div
          key={`empty-${i}`}
          className="rounded-2xl border border-dashed border-black/10 px-4 py-4 flex items-center gap-4 mb-3 text-sub"
        >
          <span className="text-sm font-semibold w-6 text-center">{ranking.length + i + 1}위</span>
          <div className="w-14 h-14 rounded-full bg-black/5 flex items-center justify-center text-sub">
            <Plus size={20} />
          </div>
          <p className="text-sm flex-1">그룹원이 모이면 표시돼요</p>
        </div>
      ))}
    </div>
  );
}
