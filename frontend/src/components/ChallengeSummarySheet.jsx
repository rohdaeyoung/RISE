import { useNavigate } from 'react-router-dom';
import { Heart, Medal, PartyPopper, Plus } from 'lucide-react';
import { memberColorIndex, useAppDispatch } from '../context/AppContext';
import { continueChallenge } from '../api/challengeApi';
import { leaveGroup } from '../api/groupApi';
import CharacterAvatar from './CharacterAvatar';
import CoinIcon from './CoinIcon';

const MAX_SLOTS = 4;
const MEDAL_COLORS = ['#e8b923', '#a9adb4', '#c17a4a'];

// 그룹 슬롯 카드 배경 — memberColorIndex(0~3)로 뽑는 User Signature Color를, 상점 의상 카드와
// 톤을 맞춰 옅은 파스텔(15% 불투명도)로 씀. 완전한 클래스 문자열로 나열해야 Tailwind가 정적으로 인식한다.
const CARD_BG_CLASSES = ['bg-user-1/15', 'bg-user-2/15', 'bg-user-3/15', 'bg-user-4/15'];

// 7일 챌린지 종료 결과. 나 + 실제로 코드로 참여한 그룹원의 데이터만 사용하고,
// 아직 그룹원이 없으면 "참여 대기 중" 빈 슬롯으로 정직하게 표시함(가짜 이름/데이터 없음).
export default function ChallengeSummarySheet({ summary }) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  // 새 사이클은 목표·신체정보부터 다시 받는다(PRD: 온보딩은 그룹 사이클마다 갱신).
  // 서버가 지난 사이클의 미션·식단·온보딩을 지우므로, 여기서 바로 그룹으로 보내면
  // 온보딩이 없어 미션이 만들어지지 않는다. 온보딩 화면으로 보내야 한다.
  //
  // sync()는 부르지 않는다. 온보딩을 지운 직후라 서버에서 받아올 것이 없고,
  // 온보딩을 마치는 시점에 OnboardingPage가 동기화하면서 새 미션을 받아온다.
  function handleContinue() {
    continueChallenge()
      .catch(() => {})
      .finally(() => {
        dispatch({ type: 'CONTINUE_CHALLENGE' });
        navigate('/onboarding');
      });
  }

  function handleLeave() {
    leaveGroup()
      .catch(() => {})
      .finally(() => {
        dispatch({ type: 'LEAVE_GROUP' });
        navigate('/my');
      });
  }

  // summary.ranking은 END_CHALLENGE에서 이미 포인트 기준으로 정렬해둔 전체 참가자 목록 —
  // "그룹원 카드"와 "최종 순위"를 따로 만들지 않고 이거 하나로 통일해서 씀.
  const rankingSlots = [...(summary.ranking || [])];
  while (rankingSlots.length < MAX_SLOTS) rankingSlots.push(null);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-end justify-center z-50">
      <div className="w-full max-w-md bg-card rounded-t-sheet p-6 pb-8 max-h-[85svh] overflow-y-auto shadow-modal">
        <div className="w-10 h-1.5 rounded-full bg-black/10 mx-auto mb-5" />
        <div className="text-center mb-5">
          <PartyPopper size={32} className="mx-auto mb-2 text-brand" />
          <h2 className="text-lg font-bold text-ink">7일 챌린지 결과</h2>
          <p className="text-sm text-sub mt-1">함께여서 완주할 수 있었어요</p>
        </div>

        <div className="rounded-2xl border border-gray-300 bg-cream grid grid-cols-3 divide-x divide-gray-300 mb-6 shadow-card">
          <div className="px-2 py-4 text-center">
            <p className="text-xs text-sub mb-1">완주</p>
            <p className="text-lg font-bold text-ink">{summary.days}일</p>
          </div>
          <div className="px-2 py-4 text-center">
            <p className="text-xs text-sub mb-1">달성률</p>
            <p className="text-lg font-bold text-ink">{summary.achievementRate}%</p>
          </div>
          <div className="px-2 py-4 text-center">
            <p className="text-xs text-sub mb-1">획득</p>
            <p className="text-lg font-bold text-ink flex items-center justify-center gap-1">
              <CoinIcon className="w-4 h-4" />
              {summary.coinsEarned}
            </p>
          </div>
        </div>

        <p className="text-sm font-semibold text-ink mb-2">최종 순위</p>
        <div className="grid grid-cols-2 gap-3 mb-6">
          {rankingSlots.map((p, i) =>
            p ? (
              <div
                key={p.id}
                className={`relative rounded-2xl overflow-hidden shadow-card flex flex-col items-center py-4 gap-1.5 ${
                  CARD_BG_CLASSES[memberColorIndex(p.id, summary.members) % CARD_BG_CLASSES.length]
                }`}
              >
                <span className="absolute top-2 left-2 w-6 h-6 rounded-full bg-white/80 flex items-center justify-center">
                  {i < 3 ? (
                    <Medal size={14} color={MEDAL_COLORS[i]} fill={MEDAL_COLORS[i]} fillOpacity={0.3} />
                  ) : (
                    <span className="text-[10px] font-bold text-ink">{i + 1}</span>
                  )}
                </span>
                {/* outfit을 빼먹으면 상점에서 산 옷을 입고 있어도 결산에서만 기본 옷으로 보인다.
                    랭킹·그룹 피드는 넘기고 있어서 결산 화면만 캐릭터가 달라 보였다. */}
                <CharacterAvatar
                  species={p.species}
                  expression={p.expression}
                  outfit={p.outfit}
                  size="md"
                  breathing={false}
                />
                <p className="text-xs font-semibold text-center text-ink">{p.label}</p>
                <p className="text-sm font-bold text-ink">{p.points}점</p>
                {/* 점수만 보면 서로 얼마나 해냈는지 감이 안 와서, 7일 전체 달성률을 함께 보여준다. */}
                <p className="text-[11px] text-sub">7일 달성률 {p.rate ?? 0}%</p>
              </div>
            ) : (
              <div
                key={`empty-${i}`}
                className="rounded-2xl border border-dashed border-brand/20 bg-brand-soft/30 flex flex-col items-center justify-center gap-1 py-8 text-brand-dark"
              >
                <Plus size={22} />
                <p className="text-[11px]">참여 대기 중</p>
              </div>
            ),
          )}
        </div>

        <div className="rounded-2xl bg-brand-soft/60 px-4 py-5 flex flex-col items-center gap-1 mb-6">
          <div className="flex items-center justify-center -space-x-4">
            {(summary.ranking || []).map((p) => (
              <CharacterAvatar
                key={p.id}
                species={p.species}
                expression={p.expression}
                outfit={p.outfit}
                size="md"
                breathing={false}
                framed={false}
              />
            ))}
          </div>
          <p className="text-sm font-semibold text-brand-dark flex items-center justify-center gap-1">
            우리의 {summary.days}일, 함께 완주했어요 <Heart size={14} fill="currentColor" />
          </p>
        </div>

        <div className="space-y-2">
          <button
            onClick={handleContinue}
            className="w-full bg-brand text-white rounded-full py-3.5 text-sm font-semibold"
          >
            같은 그룹으로 계속하기
          </button>
          <button onClick={handleLeave} className="w-full text-sub text-sm py-2">
            방 나가기
          </button>
        </div>
      </div>
    </div>
  );
}
