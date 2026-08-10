import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Calendar, Camera, CheckCircle2, Clock, Footprints, Medal, PartyPopper, Plus, Settings } from 'lucide-react';
import {
  achievementRate,
  buildRanking,
  dayIndexOf,
  expressionFromRank,
  isChallengeLastDay,
  memberColorIndex,
  memberLabel,
  myRankOf,
  useAppDispatch,
  useAppState,
} from '../context/AppContext';
import CharacterAvatar from '../components/CharacterAvatar';
import ChallengeSummarySheet from '../components/ChallengeSummarySheet';

const MAX_SLOTS = 4;
// 그룹 피드 카드 배경 — 상점(Shop) 카드와 동일한 파스텔 톤(User Signature Color + 회색 테두리),
// 동일한 색상 순서(블루 → 옐로우 → 민트 → 핑크)로 통일. 상점(15%)보다 살짝 진하게(25%) 사용.
const CARD_BG_CLASSES = ['bg-user-4/25', 'bg-user-1/25', 'bg-user-2/25', 'bg-user-3/25'];

export default function GroupFeedPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  // 그룹 탭을 눌렀는데 아직 그룹이 없으면 그룹 만들기/참여 화면으로 바로 보냄.
  if (!state.group) {
    return <Navigate to="/group-entry" replace />;
  }

  const { group } = state;
  const dayIndex = dayIndexOf(group);
  const lastDay = isChallengeLastDay(group);
  const rate = achievementRate(state.missions);
  const ranking = buildRanking(state);
  const myRank = myRankOf(ranking);
  // "인증 완료" = 오늘 미션을 하나라도 한 인원 수 — 가짜 인원 없이 실제 참여자 기준으로만 계산.
  const achievedMemberCount = ranking.filter((p) => p.rate > 0).length;
  const slots = [
    {
      id: 'me',
      isMe: true,
      photo: state.todayPhoto,
      nickname: state.nickname,
      species: state.character.species,
      expression: expressionFromRank(myRank, ranking.length, rate),
      outfit: state.character.outfit,
      rate,
    },
    ...group.members,
  ];
  while (slots.length < MAX_SLOTS) slots.push(null);

  function copyCode() {
    navigator.clipboard?.writeText(group.code).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }

  return (
    <div className="px-5 pt-8 pb-24">
      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Our Group</p>
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-xl font-bold text-ink">우리 그룹</h1>
        <div className="flex items-center gap-2">
          <button
            onClick={copyCode}
            className="flex items-center gap-1.5 bg-card border border-gray-300 rounded-full pl-3 pr-2 py-1.5 text-xs shadow-card"
          >
            <span className="font-mono font-semibold text-ink tracking-wide">{group.code}</span>
            <span className="bg-brand-soft text-brand-dark rounded-full px-2 py-0.5 font-medium">
              {copied ? '복사됨!' : '복사'}
            </span>
          </button>
          <button
            onClick={() => navigate('/group/settings')}
            aria-label="방 설정"
            className="w-8 h-8 flex items-center justify-center rounded-full bg-card border border-gray-300 shadow-card text-sub flex-shrink-0"
          >
            <Settings size={15} />
          </button>
        </div>
      </div>

      <div className="relative rounded-3xl px-5 py-6 mb-6 text-white bg-brand shadow-card overflow-hidden">
        <div className="absolute -top-8 -right-8 w-32 h-32 rounded-full bg-white/10 blur-xl" />
        <div className="relative">
          <div className="flex items-center justify-between mb-1">
            <h2 className="text-lg font-bold flex items-center gap-1.5">
              {group.name} <Footprints size={16} />
            </h2>
            <span className="text-xs font-semibold bg-white/20 rounded-full px-2.5 py-1">Day {dayIndex}/7</span>
          </div>
          <p className="text-xs text-white/75 mb-4">7일 챌린지 진행 중</p>
          <div className="w-full h-1.5 bg-white/25 rounded-full overflow-hidden mb-2">
            <div className="h-full bg-white rounded-full transition-all" style={{ width: `${rate}%` }} />
          </div>
          <div className="flex items-center justify-between text-xs text-white/85 mb-3">
            <span>
              오늘 {achievedMemberCount} / {ranking.length}명 인증 완료
            </span>
            <span>내 달성률 {rate}%</span>
          </div>
          <Link
            to="/ranking"
            className="flex items-center justify-between bg-white/15 rounded-2xl px-3.5 py-2.5 text-xs font-semibold mb-2"
          >
            <span className="flex items-center gap-1.5">
              <Medal size={14} /> 내 순위
            </span>
            <span>
              {myRank}위 / {ranking.length}명 →
            </span>
          </Link>
          <Link
            to="/group/progress"
            className="flex items-center justify-between bg-white/15 rounded-2xl px-3.5 py-2.5 text-xs font-semibold"
          >
            <span className="flex items-center gap-1.5">
              <Calendar size={14} /> 진행 현황
            </span>
            <span>{dayIndex}/7일차 →</span>
          </Link>
          {lastDay && (
            <button
              onClick={() => dispatch({ type: 'END_CHALLENGE' })}
              className="w-full bg-white text-brand-dark rounded-2xl py-3 text-sm font-semibold mt-4 flex items-center justify-center gap-1.5"
            >
              <PartyPopper size={16} /> 7일 챌린지 결과 보기
            </button>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between mb-3">
        <h2 className="text-sm font-semibold text-ink">오늘의 인증 피드</h2>
      </div>
      <div className="grid grid-cols-2 gap-3">
        {slots.map((member, i) => {
          if (!member) {
            return (
              <div
                key={`empty-${i}`}
                className="rounded-2xl border border-dashed border-brand/20 bg-brand-soft/30 p-4 flex flex-col items-center justify-center gap-1 aspect-square text-brand-dark"
              >
                <Plus size={22} />
                <p className="text-[11px]">참여 대기 중</p>
              </div>
            );
          }
          const memberRate = member.isMe ? rate : (member.achievementRate ?? 0);
          return (
            <button
              key={member.id}
              onClick={() => (member.isMe ? navigate('/my') : navigate(`/group/member/${member.id}`))}
              className={`rounded-2xl border border-gray-300 p-2.5 flex flex-col gap-2 shadow-card transition-transform active:scale-95 ${
                CARD_BG_CLASSES[memberColorIndex(member.id, group.members) % CARD_BG_CLASSES.length]
              }`}
            >
              <div className="rounded-xl bg-white/70 aspect-square overflow-hidden flex items-center justify-center">
                {member.photo ? (
                  <img src={member.photo} alt="미션 인증 사진" className="w-full h-full object-cover" />
                ) : (
                  <Camera size={22} className="text-black/20" />
                )}
              </div>
              <div className="flex items-center gap-1.5 px-0.5 min-w-0">
                <CharacterAvatar species={member.species} expression={member.expression} outfit={member.outfit} size="xs" breathing={false} />
                <p className="text-xs font-semibold text-ink truncate">{memberLabel(member, member.isMe)}</p>
              </div>
              <p className="text-[11px] text-ink/70 px-0.5 pb-0.5 flex items-center gap-1">
                {memberRate > 0 ? <CheckCircle2 size={12} /> : <Clock size={12} />}
                오늘 인증 {memberRate}%
              </p>
            </button>
          );
        })}
      </div>

      {state.challengeSummary && <ChallengeSummarySheet summary={state.challengeSummary} />}
    </div>
  );
}
