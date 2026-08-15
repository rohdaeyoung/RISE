import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Calendar, Camera, CheckCircle2, Clock, Footprints, Medal, MessageCircle, PartyPopper, Plus, Settings, Smile } from 'lucide-react';
import {
  achievementRate,
  buildRanking,
  dayIndexOf,
  expressionFromRank,
  isChallengeLastDay,
  memberColorIndex,
  memberLabel,
  myRankOf,
  REACTION_EMOJIS,
  useAppDispatch,
  useAppState,
} from '../context/AppContext';
import { endChallenge } from '../api/challengeApi';
import { toggleReaction } from '../api/feedApi';
import { isBackendEnabled } from '../api/client';
import CharacterAvatar from '../components/CharacterAvatar';
import ChallengeSummarySheet from '../components/ChallengeSummarySheet';
import CommentSheet from '../components/CommentSheet';
import heartIcon from '../assets/reactions/heart.png';
import likeIcon from '../assets/reactions/like.png';
import funnyIcon from '../assets/reactions/funny.png';
import wowIcon from '../assets/reactions/wow.png';
import sadIcon from '../assets/reactions/sad.png';

const MAX_SLOTS = 4;
// REACTION_EMOJIS(❤️ 👍 😂 😮 😢) 각 항목에 대응하는 커스텀 반응 아이콘.
const REACTION_ICONS = { '❤️': heartIcon, '👍': likeIcon, '😂': funnyIcon, '😮': wowIcon, '😢': sadIcon };
// 그룹 피드 카드 배경 — 상점(Shop) 카드와 동일한 파스텔 톤(User Signature Color + 회색 테두리),
// 동일한 색상 순서(블루 → 옐로우 → 민트 → 핑크)로 통일. 상점(15%)보다 살짝 진하게(25%) 사용.
const CARD_BG_CLASSES = ['bg-user-4/25', 'bg-user-1/25', 'bg-user-2/25', 'bg-user-3/25'];

export default function GroupFeedPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);
  const [openPickerId, setOpenPickerId] = useState(null);
  const [showComments, setShowComments] = useState(false);
  const unreadCommentCount = Math.max(0, state.comments.length - (state.lastSeenCommentCount || 0));

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

  // 백엔드 모드에서는 서버가 최종 순위를 매기고 순위별 보상까지 지급한 결과를 받아온다.
  // mock 모드에서는 기존처럼 리듀서가 로컬로 결과를 계산한다.
  function handleEndChallenge() {
    if (!isBackendEnabled) {
      dispatch({ type: 'END_CHALLENGE' });
      return;
    }
    endChallenge()
      .then((summary) => dispatch({ type: 'SET_CHALLENGE_SUMMARY', summary }))
      .catch(() => dispatch({ type: 'END_CHALLENGE' }));
  }

  // 화면은 즉시 로컬 리듀서로 반영하고(낙관적 업데이트), 백엔드 모드에서는 서버 응답으로
  // 다시 한번 맞춘다 — 그 사이 다른 그룹원이 남긴 반응까지 함께 받아오기 위해서다.
  function handleReact(memberId, emoji) {
    dispatch({ type: 'TOGGLE_REACTION', memberId, emoji });
    if (!isBackendEnabled) return;
    toggleReaction(memberId, emoji).then((feed) => {
      if (feed) dispatch({ type: 'SET_FEED', reactions: feed.reactions, comments: feed.comments });
    });
  }

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
              onClick={handleEndChallenge}
              className="w-full bg-white text-brand-dark rounded-2xl py-3 text-sm font-semibold mt-4 flex items-center justify-center gap-1.5"
            >
              <PartyPopper size={16} /> 7일 챌린지 결과 보기
            </button>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between mb-3">
        <h2 className="text-sm font-semibold text-ink">오늘의 인증 피드</h2>
        <button
          onClick={() => {
            setShowComments(true);
            dispatch({ type: 'MARK_COMMENTS_SEEN' });
          }}
          aria-label="댓글 보기"
          className="relative inline-flex items-center justify-center text-brand-dark"
        >
          <MessageCircle size={18} />
          {unreadCommentCount > 0 && (
            <span className="absolute -top-1.5 -right-1.5 min-w-[14px] h-3.5 px-0.5 flex items-center justify-center rounded-full bg-red-500 text-white text-[9px] font-bold leading-none">
              {unreadCommentCount > 9 ? '9+' : unreadCommentCount}
            </span>
          )}
        </button>
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
          const goToMember = () => (member.isMe ? navigate('/my') : navigate(`/group/member/${member.id}`));
          const reaction = state.reactions[member.id] || { myEmoji: null, counts: {} };
          const reactionTotal = Object.values(reaction.counts).reduce((sum, n) => sum + n, 0);
          const pickerOpen = openPickerId === member.id;
          return (
            <div
              key={member.id}
              role="button"
              tabIndex={0}
              onClick={goToMember}
              onKeyDown={(e) => e.key === 'Enter' && goToMember()}
              className={`rounded-2xl border border-gray-300 p-2.5 flex flex-col gap-2 shadow-card transition-transform active:scale-95 cursor-pointer ${
                CARD_BG_CLASSES[memberColorIndex(member.id, group.members) % CARD_BG_CLASSES.length]
              }`}
            >
              <div className="relative rounded-xl bg-white/70 aspect-square overflow-hidden flex items-center justify-center">
                {member.photo ? (
                  <img src={member.photo} alt="미션 인증 사진" className="w-full h-full object-cover" />
                ) : (
                  <Camera size={22} className="text-black/20" />
                )}
                {/* 내 사진에는 반응을 남길 수 없음 — 남의 인증 사진에만 이모티콘 반응 버튼을 보여줌. */}
                {member.photo && !member.isMe && (
                  <div className="absolute bottom-1.5 right-1.5" onClick={(e) => e.stopPropagation()}>
                    {pickerOpen && (
                      <div className="absolute bottom-full right-0 mb-1.5 flex items-center gap-0.5 bg-white rounded-full px-1.5 py-1 shadow-card border border-gray-300">
                        {REACTION_EMOJIS.map((emoji) => (
                          <button
                            key={emoji}
                            onClick={() => {
                              handleReact(member.id, emoji);
                              setOpenPickerId(null);
                            }}
                            aria-label={emoji}
                            className={`w-7 h-7 flex items-center justify-center rounded-full p-1 transition-transform active:scale-90 ${
                              reaction.myEmoji === emoji ? 'bg-brand-soft' : ''
                            }`}
                          >
                            <img src={REACTION_ICONS[emoji]} alt={emoji} className="w-full h-full object-contain" />
                          </button>
                        ))}
                      </div>
                    )}
                    <button
                      onClick={() => setOpenPickerId(pickerOpen ? null : member.id)}
                      aria-label="반응 남기기"
                      className={`flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-semibold shadow-card transition-colors ${
                        reaction.myEmoji ? 'bg-brand-soft text-brand-dark' : 'bg-white/90 text-ink'
                      }`}
                    >
                      {reaction.myEmoji ? (
                        <img src={REACTION_ICONS[reaction.myEmoji]} alt={reaction.myEmoji} className="w-3.5 h-3.5 object-contain" />
                      ) : (
                        <Smile size={13} className="text-black/35" />
                      )}
                      {reactionTotal > 0 && reactionTotal}
                    </button>
                  </div>
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
            </div>
          );
        })}
      </div>

      {state.challengeSummary && <ChallengeSummarySheet summary={state.challengeSummary} />}
      {showComments && <CommentSheet onClose={() => setShowComments(false)} />}
    </div>
  );
}
