import { createContext, useCallback, useContext, useEffect, useReducer } from 'react';
import {
  DEFAULT_MISSION_HOUR,
  DEFAULT_MISSION_MINUTE,
  fetchOrCreateTodayMissions,
  generateDailyMissions,
} from '../api/missionApi';
import { isBackendEnabled } from '../api/client';
import { fetchCharacter, fetchMe, fetchOnboarding } from '../api/profileApi';
import { fetchMyGroup } from '../api/groupApi';
import { fetchTodayMeals } from '../api/mealApi';
import { fetchFeed } from '../api/feedApi';

const STORAGE_KEY = 'withu_state';
const DAY_MS = 24 * 60 * 60 * 1000;

export const MEAL_LABELS = { breakfast: '아침', lunch: '점심', dinner: '저녁' };

// 그룹 피드 인증 사진에 남길 수 있는 이모티콘 반응 종류.
export const REACTION_EMOJIS = ['❤️', '👍', '😂', '😮', '😢'];

// 나이/키/몸무게 허용 범위 — 마이너스 입력이 저장되지 않도록 온보딩 화면과 리듀서 양쪽에서
// 이 값을 기준으로 clamp함. 키/몸무게는 상한 없이 최소값만 강제(마이너스 방지).
export const AGE_RANGE = { min: 1, max: 100 };
export const HEIGHT_RANGE = { min: 100, max: Infinity };
export const WEIGHT_RANGE = { min: 20, max: Infinity };

function clamp(value, { min, max }) {
  return Math.min(max, Math.max(min, value));
}

// 미션(생활습관/식단) 1개를 완료할 때 지급하는 코인.
const MISSION_COIN_REWARD = 10;
const DEFAULT_OWNED_OUTFITS = ['everyday'];
export const DEFAULT_GROUP_NAME = '건강한 친구들';
export const CHALLENGE_LENGTH_DAYS = 7;
export const MAX_GROUP_NAME_LENGTH = 10;
export const MAX_NICKNAME_LENGTH = 10;
export const MAX_COMMENT_LENGTH = 200;

const initialState = {
  auth: { userId: null, email: null },
  onboarding: { goal: null, gender: null, age: null, height: null, weight: null },
  character: { species: null, expression: 'normal', outfit: 'everyday', ownedOutfits: DEFAULT_OWNED_OUTFITS },
  // 그룹 피드/랭킹 등에서 "나"를 대신해 보여줄 닉네임 — 설정 화면에서 바꿀 수 있고, 없으면 "나"로 표시.
  nickname: null,
  coins: 0,
  // 이번 7일 챌린지 사이클 동안 모은 코인 — 그룹 생성/재시작 시점에 0으로 리셋되고
  // 결과 화면의 "획득 코인"에 쓰임. 평생 누적치인 coins와는 별개.
  challengeCoins: 0,
  // group: null 이면 그룹 미참여 상태. { id, code, members, startedAt, missionHour, missionMinute }
  // members는 실제로 코드로 참여한 사람만 채워짐(백엔드 연동 전에는 항상 빈 배열).
  // 그룹은 필수(2~4인) — 그룹 없이 혼자 진행하는 경로는 없음.
  // missionHour/missionMinute: 방장이 그룹 생성 시 정한 "미션 시작 시간" — 생성 이후에는 그룹원 누구나 바꿀 수 있음.
  group: null,
  missions: [],
  meals: { breakfast: null, lunch: null, dinner: null },
  // 오늘 올린 미션/식단 인증 사진 중 가장 최근 것 — 그룹 피드의 "내 인증 사진" 썸네일로 씀.
  // 새 사이클/새 날짜가 되면 meals와 함께 비워짐.
  todayPhoto: null,
  // 그룹 피드 인증 사진에 대한 이모티콘 반응 — { [memberId]: { myEmoji, counts: { [emoji]: count } } }.
  // 한 사람당 하나의 이모티콘만 선택 가능(다시 누르면 취소, 다른 이모티콘 누르면 교체).
  // todayPhoto와 마찬가지로 "오늘의 인증 피드" 기준이라 새 사이클/새 날짜가 되면 함께 비워짐.
  reactions: {},
  // 오늘의 인증 피드에 남긴 댓글 — [{ id, text, authorLabel, createdAt }]. reactions와 마찬가지로
  // "오늘" 기준이라 새 사이클/새 날짜가 되면 함께 비워짐. createdAt(ms epoch)으로 작성 일시를 표시.
  comments: [],
  // 댓글 시트를 마지막으로 열었을 때의 comments.length — 그 이후 쌓인 댓글 수만큼만
  // "안 읽은 댓글"로 세어 빨간 배지를 띄움. comments와 마찬가지로 새 사이클/새 날짜에 함께 리셋.
  lastSeenCommentCount: 0,
  challengeSummary: null,
  // 지나간 날짜의 달성 기록 스냅샷 — [{ day, rate, missionsDone, missionsTotal }]. 그룹 생성/재시작 시 비움.
  // 오늘(진행 중인 날)은 여기 안 들어있고 missions/meals로 실시간 계산됨 — dailyHistory.length + 1이 오늘.
  dailyHistory: [],
};

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return initialState;
    const saved = JSON.parse(raw);
    return {
      ...initialState,
      ...saved,
      // 예전에 저장된 state에는 coins/ownedOutfits/group.name이 없을 수 있어 얕은 병합만으로는 유실됨 — 깊은 병합으로 보정.
      character: { ...initialState.character, ...saved.character },
      group: saved.group ? { name: DEFAULT_GROUP_NAME, ...saved.group } : saved.group ?? null,
    };
  } catch {
    return initialState;
  }
}

// 오늘 달성률이 0%면 순위와 무관하게 무조건 슬픔 — 아무것도 안 했는데 등수만으로 기쁜 표정을
// 짓는 건 어색함(특히 그룹원이 없어 항상 "나 혼자 1등"인 경우). 뭔가 했으면 그때부터 그룹 내
// "순위"로 세분화: 상위권(1~2등)은 기쁨, 꼴찌 바로 위는 무표정, 꼴찌는 슬픔.
// 그룹 인원은 최대 4인이라 "1~2등 기쁨 / 3등 무표정 / 4등 슬픔" 구성과 맞아떨어짐.
export function expressionFromRank(rank, total, rate = 0) {
  if (rate <= 0) return 'bad';
  if (total <= 1) return rate >= 50 ? 'good' : 'normal';
  if (rank >= total) return 'bad';
  if (total >= 3 && rank === total - 1) return 'normal';
  return 'good';
}

// missions/그룹원 변화를 반영한 state를 넘기면, 그 시점 달성률+순위 기준 표정을 계산.
export function expressionForRanking(state) {
  const ranking = buildRanking(state);
  return expressionFromRank(myRankOf(ranking), ranking.length, achievementRate(state.missions));
}

// 백엔드 연동 모드에서는 미션을 서버가 만든다. 로컬 mock 생성기는 백엔드 없이 데모할 때만 쓴다.
// (자세한 이유는 아래 SET_GROUP 주석 참고)
function newMissionSet(params) {
  return isBackendEnabled ? [] : generateDailyMissions(params);
}

function reducer(state, action) {
  switch (action.type) {
    // 로그인/회원가입 시 auth를 제외한 나머지는 초기화한다. 이걸 안 하면 로그아웃 없이 다른 계정으로
    // 들어왔을 때 앞 사람의 그룹·캐릭터·코인이 그대로 남아 보인다(한 기기를 나눠 쓰는 경우).
    // 백엔드 연동 시에는 로그인 직후 동기화가 서버 값으로 채우고, mock 모드에서도 계정이 바뀌면
    // 처음부터 시작하는 게 맞다.
    case 'LOGIN_SUCCESS':
      return { ...initialState, auth: { userId: action.userId, email: action.email } };

    case 'LOGOUT':
      return initialState;

    // 백엔드 연동 시 DELETE /api/account 호출 후 처리할 자리. 지금은 로그아웃과 동일하게 로컬 상태만 초기화.
    case 'DELETE_ACCOUNT':
      return initialState;

    // 미션은 그룹(챌린지)이 있어야 의미가 있으므로 그룹이 만들어지는 시점에 처음 생성함 —
    // 이때 그룹에서 정한 미션 시작 시간을 바로 반영해서 도착 스케줄을 잡음.
    //
    // 단 백엔드 연동 모드에서는 미션의 주인이 서버다(AI가 목표·신체정보를 보고 만든다).
    // 여기서 로컬 미션을 만들어 넣으면 화면에 잠깐 보였다가 sync()가 서버 세트로 갈아끼우면서
    // 미션이 저절로 바뀐 것처럼 보이고, 그 사이 다른 그룹원에게는 서버 세트가 보여
    // 같은 사람의 오늘 미션이 서로 다르게 표시된다. 미션이 바뀌는 건 날짜가 바뀔 때뿐이어야 한다.
    case 'SET_GROUP': {
      const missionHour = action.missionHour ?? DEFAULT_MISSION_HOUR;
      const missionMinute = action.missionMinute ?? DEFAULT_MISSION_MINUTE;
      const nextState = {
        ...state,
        group: {
          id: action.id,
          code: action.code,
          name: action.name?.trim().slice(0, MAX_GROUP_NAME_LENGTH) || DEFAULT_GROUP_NAME,
          members: action.members,
          startedAt: Date.now(),
          missionHour,
          missionMinute,
        },
        missions: newMissionSet({ goal: state.onboarding.goal }),
        meals: { breakfast: null, lunch: null, dinner: null },
        todayPhoto: null,
        reactions: {},
        comments: [],
        lastSeenCommentCount: 0,
        challengeSummary: null,
        challengeCoins: 0,
        dailyHistory: [],
      };
      // 아직 아무 미션도 안 한 시점(0%)이므로 곧바로 슬픈 표정으로 시작.
      return { ...nextState, character: { ...nextState.character, expression: expressionForRanking(nextState) } };
    }

    // 그룹 생성 이후에는 방장뿐 아니라 그룹원 누구나 미션 시작 시간을 바꿀 수 있음.
    case 'SET_MISSION_TIME':
      return {
        ...state,
        group: state.group ? { ...state.group, missionHour: action.missionHour, missionMinute: action.missionMinute } : state.group,
      };

    // 방 이름도 미션 시작 시간과 동일하게 그룹원 누구나 바꿀 수 있음.
    case 'SET_GROUP_NAME': {
      const name = action.name?.trim().slice(0, MAX_GROUP_NAME_LENGTH);
      if (!state.group || !name) return state;
      return { ...state, group: { ...state.group, name } };
    }

    // 미션과 식단은 그룹 사이클에 속한 기록이라 그룹을 떠나면 같이 비운다.
    // 남겨두면 그룹이 없는데도 MY 화면에 지난 그룹의 미션이 그대로 떠 있고,
    // 그걸 인증하려 하면 서버는 그룹원이 아니라며 거절해 아무 반응이 없는 것처럼 보인다.
    // 반응/댓글도 "오늘의 인증 피드" 기준이라 그룹이 없어지면 함께 비운다.
    case 'LEAVE_GROUP':
      return {
        ...state,
        group: null,
        challengeSummary: null,
        missions: [],
        meals: { breakfast: null, lunch: null, dinner: null },
        todayPhoto: null,
        reactions: {},
        comments: [],
        lastSeenCommentCount: 0,
      };

    case 'SET_ONBOARDING': {
      const incoming = { ...action.onboarding };
      if (incoming.age != null) incoming.age = clamp(Math.round(incoming.age), AGE_RANGE);
      if (incoming.height != null) incoming.height = clamp(Math.round(incoming.height), HEIGHT_RANGE);
      if (incoming.weight != null) incoming.weight = clamp(Math.round(incoming.weight), WEIGHT_RANGE);
      const onboarding = { ...state.onboarding, ...incoming };
      // 온보딩은 이제 그룹 생성/참여 "이후"에 진행됨 — 그룹이 이미 있으면 그때까지 목표(goal)를
      // 몰라서 fallback 풀로 만들어둔 미션을, 방금 알게 된 goal 기준으로 다시 만든다.
      const missions = state.group
        ? newMissionSet({ goal: onboarding.goal })
        : state.missions;
      return {
        ...state,
        onboarding,
        missions,
        meals: { breakfast: null, lunch: null, dinner: null },
        // 온보딩 직후는 아직 순위를 매길 활동이 없는 시점이라 expressionForRanking을 쓰면 안 됨
        // (전원 0%라 정렬 순서상 우연히 꼴찌가 될 수도 있음) — 항상 무표정으로 시작.
        character: { ...state.character, expression: 'normal' },
      };
    }

    case 'SET_CHARACTER':
      return { ...state, character: { ...state.character, species: action.species } };

    case 'SET_NICKNAME':
      return { ...state, nickname: action.nickname?.trim().slice(0, MAX_NICKNAME_LENGTH) || null };

    // 이미 구매한(또는 기본 무료) 의상만 착용할 수 있음.
    case 'SET_OUTFIT': {
      const owned = state.character.ownedOutfits || DEFAULT_OWNED_OUTFITS;
      if (!owned.includes(action.outfit)) return state;
      return { ...state, character: { ...state.character, outfit: action.outfit } };
    }

    // 상점에서 코인으로 의상 구매. 이미 보유했거나 코인이 부족하면 아무 변화도 없음.
    case 'BUY_OUTFIT': {
      const owned = state.character.ownedOutfits || DEFAULT_OWNED_OUTFITS;
      if (owned.includes(action.outfitId) || state.coins < action.price) return state;
      return {
        ...state,
        coins: state.coins - action.price,
        character: { ...state.character, ownedOutfits: [...owned, action.outfitId] },
      };
    }

    case 'LOG_MEAL': {
      const meals = { ...state.meals, [action.mealKey]: { achieved: action.achieved, photo: action.photo } };

      let missions = state.missions;
      let coins = state.coins;
      let challengeCoins = state.challengeCoins;
      if (action.achieved) {
        const idx = missions.findIndex((m) => m.type === 'diet' && !m.done);
        if (idx !== -1) {
          missions = missions.map((m, i) => (i === idx ? { ...m, done: true } : m));
          coins += MISSION_COIN_REWARD;
          challengeCoins += MISSION_COIN_REWARD;
        }
      }

      const nextState = { ...state, meals, missions };
      return {
        ...nextState,
        coins,
        challengeCoins,
        todayPhoto: action.photo ?? state.todayPhoto,
        character: { ...state.character, expression: expressionForRanking(nextState) },
      };
    }

    // 서버에 기록된 오늘의 식단으로 맞춘다. LOG_MEAL이 방금 반영한 것과 같은 내용이지만,
    // 새로고침·기기 변경 후에도 인증 표시가 남고 그룹원이 보는 것과 어긋나지 않게 하는 건 이쪽이다.
    case 'SET_MEALS': {
      const latestPhoto = ['dinner', 'lunch', 'breakfast']
        .map((key) => action.meals[key]?.photo)
        .find(Boolean);
      return { ...state, meals: action.meals, todayPhoto: latestPhoto ?? state.todayPhoto };
    }

    case 'COMPLETE_MISSION': {
      const target = state.missions.find((m) => m.id === action.missionId);
      if (!target || target.done) return state;
      const missions = state.missions.map((m) => (m.id === action.missionId ? { ...m, done: true } : m));
      const nextState = { ...state, missions };
      return {
        ...nextState,
        coins: state.coins + MISSION_COIN_REWARD,
        challengeCoins: state.challengeCoins + MISSION_COIN_REWARD,
        todayPhoto: action.photo ?? state.todayPhoto,
        character: { ...state.character, expression: expressionForRanking(nextState) },
      };
    }

    // 그룹 피드에서 남의 인증 사진에 이모티콘 반응을 토글. 사진마다(memberId 기준) 내가 고른 이모티콘 1개와
    // 이모티콘별 카운트를 들고 있음 — 같은 이모티콘을 다시 누르면 취소, 다른 이모티콘을 누르면 교체.
    case 'TOGGLE_REACTION': {
      const current = state.reactions[action.memberId] || { myEmoji: null, counts: {} };
      const counts = { ...current.counts };
      if (current.myEmoji) {
        counts[current.myEmoji] = Math.max(0, (counts[current.myEmoji] || 0) - 1);
        if (counts[current.myEmoji] === 0) delete counts[current.myEmoji];
      }
      const myEmoji = current.myEmoji === action.emoji ? null : action.emoji;
      if (myEmoji) counts[myEmoji] = (counts[myEmoji] || 0) + 1;
      return { ...state, reactions: { ...state.reactions, [action.memberId]: { myEmoji, counts } } };
    }

    // 오늘의 인증 피드에 댓글 남기기. 작성자는 항상 나(닉네임 미설정 시 "나")이고,
    // createdAt은 몇일 몇시 몇분에 남겼는지 표시하는 데 씀.
    case 'ADD_COMMENT': {
      const text = action.text?.trim().slice(0, MAX_COMMENT_LENGTH);
      if (!text) return state;
      const comment = {
        id: `c-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        text,
        authorLabel: memberLabel({ nickname: state.nickname }, true),
        createdAt: Date.now(),
      };
      return { ...state, comments: [...state.comments, comment] };
    }

    // 댓글 시트를 열어서 지금까지의 댓글을 다 봤다는 표시 — 안 읽은 댓글 배지를 지움.
    case 'MARK_COMMENTS_SEEN':
      return { ...state, lastSeenCommentCount: state.comments.length };

    case 'END_CHALLENGE': {
      const rate = achievementRate(state.missions);
      // 최종 결과의 "최종 순위"는 오늘 하루 %가 아니라 이번 7일간 모은 포인트 기준으로 다시 정렬.
      const ranking = buildRanking(state).sort((a, b) => b.points - a.points);
      const rank = myRankOf(ranking);
      // 캐릭터 표정은 항상 이번에 계산한 최종 순위와 일치하도록 다시 계산 — state.character.expression을
      // 그대로 두면 마지막 활동 시점의 표정(예: 이전 사이클의 잔여 표정)과 어긋날 수 있음.
      // ChallengeSummarySheet는 이 값이 아니라 ranking의 "나" 항목(expression)을 그려서 카드에 반영하므로
      // ranking 쪽도 함께 갱신해야 한다.
      const expression = expressionFromRank(rank, ranking.length, rate);
      const finalRanking = ranking.map((p) => (p.isMe ? { ...p, expression } : p));
      return {
        ...state,
        challengeSummary: {
          achievementRate: rate,
          // "완주"는 며칠째 끝냈는지가 아니라 7일 챌린지를 다 돌았다는 뜻이라 항상 챌린지 길이 그대로.
          days: CHALLENGE_LENGTH_DAYS,
          coinsEarned: state.challengeCoins,
          ranking: finalRanking,
          rank,
          totalParticipants: finalRanking.length,
          mealPhotos: Object.entries(state.meals)
            .filter(([, v]) => v?.photo)
            .map(([key, v]) => ({ mealKey: key, mealLabel: MEAL_LABELS[key], photo: v.photo })),
          character: { ...state.character, expression },
          groupName: state.group?.name ?? DEFAULT_GROUP_NAME,
          members: state.group?.members ?? [],
        },
      };
    }

    case 'CONTINUE_CHALLENGE': {
      // 새 사이클은 목표·신체정보를 다시 받는다(PRD: 온보딩은 그룹 사이클마다 갱신).
      // 7일 동안 몸이나 목표가 달라졌을 수 있고, 그래야 AI가 새 기준으로 미션을 만든다.
      // 온보딩을 비우면 resolveHomeRoute가 온보딩 화면으로 보낸다.
      const group = state.group ? { ...state.group, startedAt: Date.now() } : null;
      const missions = newMissionSet({ goal: null });
      return {
        ...state,
        group,
        onboarding: { goal: null, gender: null, age: null, height: null, weight: null },
        missions,
        meals: { breakfast: null, lunch: null, dinner: null },
        todayPhoto: null,
        reactions: {},
        comments: [],
        lastSeenCommentCount: 0,
        challengeSummary: null,
        challengeCoins: 0,
        dailyHistory: [],
        // 새 사이클은 아직 순위를 매길 활동이 없는 시점이라 무조건 슬픔으로 두던 기존 로직은 오류 —
        // 온보딩 직후와 동일하게 무표정으로 시작.
        character: { ...state.character, expression: 'normal' },
      };
    }

    // 실제 날짜가 넘어갔는지 주기적으로 확인해서(SYNC_DAY는 앱이 켜져 있는 동안 반복 dispatch됨)
    // 지나간 날을 dailyHistory에 스냅샷하고 오늘의 미션/식단을 새로 채움.
    // 진행 현황(GroupProgressPage)이 1~7일차 히스토리를 보여줄 수 있는 건 이 스냅샷 덕분.
    case 'SYNC_DAY': {
      if (!state.group) return state;
      const today = dayIndexOf(state.group);
      const trackedDay = state.dailyHistory.length + 1;
      if (today <= trackedDay) return state;

      const rate = achievementRate(state.missions);
      const missionsDone = state.missions.filter((m) => m.done).length;
      const missionsTotal = state.missions.length;
      const history = [...state.dailyHistory];
      // trackedDay는 실제 활동이 기록된 마지막 날 — 그대로 스냅샷.
      // 그 뒤로 앱을 안 켜서 통째로 건너뛴 날은 활동이 전혀 없었으므로 가짜 진행률 대신 정직하게 0%로 기록.
      for (let d = trackedDay; d < today; d++) {
        history.push(
          d === trackedDay ? { day: d, rate, missionsDone, missionsTotal } : { day: d, rate: 0, missionsDone: 0, missionsTotal },
        );
      }

      const missions = newMissionSet({ goal: state.onboarding.goal });

      return {
        ...state,
        dailyHistory: history,
        missions,
        meals: { breakfast: null, lunch: null, dinner: null },
        todayPhoto: null,
        reactions: {},
        comments: [],
        lastSeenCommentCount: 0,
        character: { ...state.character, expression: 'normal' },
      };
    }

    // ── 아래 3개는 백엔드 연동 모드 전용 ──────────────────────────────────
    // 서버가 진실의 원천이므로 로컬에서 계산한 값을 서버 값으로 덮어쓴다.
    // mock 모드에서는 이 액션들이 아예 디스패치되지 않아 기존 동작이 그대로 유지된다.

    case 'SET_MISSIONS': {
      const nextState = { ...state, missions: action.missions };
      return {
        ...nextState,
        character: { ...nextState.character, expression: expressionForRanking(nextState) },
      };
    }

    // 코인과 캐릭터(종/의상/보유 의상)의 원본은 서버다. 특히 보유 의상을 안 받아오면
    // 상점에서 산 의상이 새로고침 후 사라진 것처럼 보인다. 표정만은 예외로 로컬 계산값을 유지한다
    // (달성률이 바뀌는 즉시 반영돼야 하는데 서버 값은 다음 동기화까지 한 박자 늦기 때문).
    case 'SET_ACCOUNT':
      return {
        ...state,
        coins: action.coins ?? state.coins,
        nickname: action.nickname ?? state.nickname,
        challengeCoins: action.challengeCoins ?? state.challengeCoins,
        character: action.character
          ? { ...state.character, ...action.character, expression: state.character.expression }
          : state.character,
      };

    // 서버에 저장된 그룹·온보딩을 로컬에 복원한다. 다른 기기나 새 브라우저에서 로그인하면
    // 로컬에는 아무것도 없어서, 이게 없으면 이미 그룹에 속해 있는데도 "그룹 만들기" 화면이 뜬다.
    // 네트워크 오류와 구분이 안 되므로 여기서는 채우기만 하고 지우지는 않는다.
    case 'RESTORE_SESSION': {
      const next = { ...state };
      if (action.group) next.group = { ...(state.group ?? {}), ...action.group };
      if (action.onboarding?.goal) next.onboarding = { ...state.onboarding, ...action.onboarding };
      return next;
    }

    // 서버에서 받은 그룹 정보를 반영한다. 진행일(currentDay)은 서버가 계산한 값을 그대로 써야
    // 그룹원 모두가 같은 날짜를 보고, 7일차 종료 시트도 동시에 뜬다.
    //
    // 방 이름과 미션 시작 시간도 함께 갱신한다. 이걸 안 하면 참여할 때 받은 값이 그대로 굳는다.
    // 방을 만들 때는 초대 코드를 먼저 보여주려고 이름 없이 방을 만든 뒤("건강한 친구들") 나중에
    // 이름을 정하므로, 그 사이에 참여한 사람은 방장이 지은 이름을 영영 못 보게 된다.
    // 나중에 누가 방 이름이나 미션 시간을 바꿔도 마찬가지다 — 그룹 설정은 모두에게 같아야 한다.
    case 'SET_GROUP_MEMBERS':
      return {
        ...state,
        group: state.group
          ? {
              ...state.group,
              members: action.members,
              currentDay: action.currentDay ?? state.group.currentDay,
              name: action.name ?? state.group.name,
              missionHour: action.missionHour ?? state.group.missionHour,
              missionMinute: action.missionMinute ?? state.group.missionMinute,
            }
          : state.group,
      };

    // 서버가 정산한 챌린지 결과를 그대로 결과 화면에 넘긴다.
    case 'SET_CHALLENGE_SUMMARY':
      return { ...state, challengeSummary: action.summary };

    // 서버가 돌려준 오늘의 피드(반응·댓글) 전체로 맞춘다. 반응/댓글 액션 응답과 주기 동기화 양쪽에서
    // 쓰인다 — 서버가 원본이므로 그 사이 다른 그룹원이 남긴 것까지 여기서 함께 반영된다.
    case 'SET_FEED': {
      // 서버는 반응을 실제 userId로 묶어 주는데, 화면은 나를 'me'로 부른다(buildRanking).
      // 그대로 넣으면 내가 받은 반응이 내 카드에서만 안 보인다 — 키가 서로 달라 못 찾기 때문.
      const myKey = state.auth.userId == null ? null : String(state.auth.userId);
      const reactions = {};
      for (const [userId, value] of Object.entries(action.reactions || {})) {
        reactions[userId === myKey ? 'me' : userId] = value;
      }
      return { ...state, reactions, comments: action.comments };
    }

    case 'RESET':
      return initialState;

    default:
      return state;
  }
}

const AppStateContext = createContext(null);
const AppDispatchContext = createContext(null);
const AppSyncContext = createContext(() => Promise.resolve());

// 백엔드 연동 모드에서 서버 상태(미션/코인/그룹원)를 주기적으로 가져와 로컬 상태에 반영한다.
// mock 모드(VITE_API_BASE_URL 미설정)에서는 아무 일도 하지 않으므로 프론트 단독 실행에 영향이 없다.
const SYNC_INTERVAL_MS = 15_000;

function useBackendSync(state, dispatch) {
  const loggedIn = Boolean(state.auth.userId);
  const inGroup = Boolean(state.group);
  const hasGoal = Boolean(state.onboarding.goal);
  const myUserId = state.auth.userId;

  const sync = useCallback(async () => {
    if (!isBackendEnabled || !loggedIn) return;
    try {
      const [me, character] = await Promise.all([fetchMe(), fetchCharacter().catch(() => null)]);
      if (me || character) {
        dispatch({ type: 'SET_ACCOUNT', coins: me?.coins, nickname: me?.nickname, character });
      }

      // 로컬에 그룹이 없어도 항상 물어본다 — 서버가 소속의 원본이라, 로컬 상태를 조건으로 걸면
      // 새 기기에서 로그인했을 때 이미 속한 그룹을 영영 못 찾는다.
      let group = null;
      let notInGroup = false;
      try {
        group = await fetchMyGroup({ myUserId });
      } catch (e) {
        // 403은 "당신은 그룹원이 아니다"라는 서버의 확답이다. 네트워크 오류(status 없음)와 달리
        // 이때는 로컬에 남은 그룹을 지워야 한다. 안 지우면 서버에서는 이미 나갔는데 화면에는
        // 그룹이 계속 보이고, 그 안의 기능은 전부 403이 나면서 아무것도 안 되는 상태가 된다.
        notInGroup = e?.status === 403;
      }

      if (!group && notInGroup && inGroup) {
        dispatch({ type: 'LEAVE_GROUP' });
      }

      if (group) {
        let goalKnown = hasGoal;
        if (!inGroup) {
          const onboarding = await fetchOnboarding().catch(() => null);
          goalKnown = goalKnown || Boolean(onboarding?.goal);
          dispatch({ type: 'RESTORE_SESSION', group, onboarding });
        } else {
          dispatch({
            type: 'SET_GROUP_MEMBERS',
            members: group.members,
            currentDay: group.currentDay,
            name: group.name,
            missionHour: group.missionHour,
            missionMinute: group.missionMinute,
          });
        }

        // 온보딩을 마친 뒤에만 미션이 생성될 수 있다(AI가 목표/신체정보를 입력으로 받음).
        if (goalKnown) {
          const missions = await fetchOrCreateTodayMissions();
          dispatch({ type: 'SET_MISSIONS', missions });
        }

        // 식단 인증도 서버 기록을 따른다 — 새로고침이나 기기 변경으로 인증 표시가 사라지지 않게.
        const meals = await fetchTodayMeals().catch(() => null);
        if (meals) dispatch({ type: 'SET_MEALS', meals });

        // 오늘의 인증 피드(반응·댓글)도 서버 기록을 따른다 — 다른 그룹원이 남긴 것이 여기서 반영된다.
        const feed = await fetchFeed().catch(() => null);
        if (feed) dispatch({ type: 'SET_FEED', reactions: feed.reactions, comments: feed.comments });
      }
    } catch {
      // 네트워크 오류 시에는 마지막으로 받은 로컬 상태를 그대로 유지한다.
    }
  }, [dispatch, loggedIn, inGroup, hasGoal, myUserId]);

  useEffect(() => {
    if (!isBackendEnabled || !loggedIn) return undefined;
    sync();
    const id = setInterval(sync, SYNC_INTERVAL_MS);
    return () => clearInterval(id);
  }, [sync, loggedIn]);

  return sync;
}

export function AppProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, undefined, loadState);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }, [state]);

  // 실제 시간 기준으로 날짜가 넘어갔는지 주기적으로 확인 — 앱을 계속 켜둔 채로도 자정을 넘기면
  // 반영되도록 마운트 시 1회 + 1분 간격으로 재확인. 그룹이 없거나 날짜가 안 바뀌었으면 리듀서가 그대로 반환.
  // 백엔드 모드에서는 서버가 미션/날짜를 관리하므로 로컬 재생성을 돌리지 않는다.
  useEffect(() => {
    if (isBackendEnabled) return undefined;
    dispatch({ type: 'SYNC_DAY' });
    const id = setInterval(() => dispatch({ type: 'SYNC_DAY' }), 60_000);
    return () => clearInterval(id);
  }, []);

  const sync = useBackendSync(state, dispatch);

  return (
    <AppStateContext.Provider value={state}>
      <AppDispatchContext.Provider value={dispatch}>
        <AppSyncContext.Provider value={sync}>{children}</AppSyncContext.Provider>
      </AppDispatchContext.Provider>
    </AppStateContext.Provider>
  );
}

export function useAppState() {
  const ctx = useContext(AppStateContext);
  if (!ctx) throw new Error('useAppState must be used within AppProvider');
  return ctx;
}

export function useAppDispatch() {
  const ctx = useContext(AppDispatchContext);
  if (!ctx) throw new Error('useAppDispatch must be used within AppProvider');
  return ctx;
}

// 서버 상태를 즉시 다시 받아오고 싶을 때 쓴다(미션 인증 직후 등).
// mock 모드에서는 아무 일도 하지 않는 no-op.
export function useAppSync() {
  return useContext(AppSyncContext);
}

export function achievementRate(missions) {
  if (!missions || missions.length === 0) return 0;
  const done = missions.filter((m) => m.done).length;
  return Math.round((done / missions.length) * 100);
}

// 닉네임을 설정 안 했으면 "나"/"그룹원"으로 폴백 — 그룹 피드/랭킹/결산 시트에서 공통으로 씀.
export function memberLabel(member, isMe) {
  return member?.nickname?.trim() || (isMe ? '나' : '그룹원');
}

// 나 + 실제로 코드로 참여한 그룹원의 오늘 달성률을 기준으로 순위를 매김.
// 그룹원은 실제 참여자만 채워지므로(가짜 데이터 없음), 백엔드 연동 전에는 대부분 "나 혼자 1등"으로 보임 — 정상.
export function buildRanking(state) {
  const me = {
    id: 'me',
    isMe: true,
    label: memberLabel({ nickname: state.nickname }, true),
    species: state.character.species,
    expression: state.character.expression,
    outfit: state.character.outfit,
    rate: achievementRate(state.missions),
    // 챌린지 종료 결과 화면의 "최종 순위"는 오늘 하루 %가 아니라 이번 7일 동안 모은 포인트(코인) 기준.
    points: state.challengeCoins,
  };
  const members = (state.group?.members || []).map((m) => ({
    id: m.id,
    isMe: false,
    label: memberLabel(m, false),
    species: m.species,
    expression: m.expression,
    outfit: m.outfit,
    rate: m.achievementRate ?? 0,
    points: m.points ?? 0,
  }));
  return [me, ...members].sort((a, b) => b.rate - a.rate);
}

export function myRankOf(ranking) {
  return ranking.findIndex((p) => p.isMe) + 1;
}

// 오늘까지 며칠 연속으로 미션을 하나 이상 했는지(오늘 0%면 스트릭 0, 과거 기록 중 0%가 나온
// 시점에서 끊김) — dailyHistory(지나간 날 스냅샷) + 오늘 실시간 달성률로 계산하는 파생값.
export function currentStreak(state) {
  const todayRate = achievementRate(state.missions);
  if (todayRate <= 0) return 0;
  let streak = 1;
  for (let i = state.dailyHistory.length - 1; i >= 0; i--) {
    if (state.dailyHistory[i].rate <= 0) break;
    streak += 1;
  }
  return streak;
}

// User Signature Color 배정 — 참여 순서 기준(나=0), CharacterAvatar의 accentIndex로 넘겨서
// 링/배지 색을 정함. 브랜드 그린이 아니라 이 색으로 "사람"을 구분한다(디자인 시스템 13번 규칙).
export function memberColorIndex(memberId, members = []) {
  if (memberId === 'me') return 0;
  const idx = members.findIndex((m) => m.id === memberId);
  return idx === -1 ? 0 : idx + 1;
}

// 7일 챌린지 진행일(1~7). 실제 경과 시간 기준으로 계산 — 임의로 넘길 수 없음.
// 백엔드 연동 시에는 서버가 계산한 currentDay를 그대로 쓴다. 진행일은 그룹원 모두에게 같아야 하는
// 값이라 기기별 시계로 따로 계산하면 사람마다 다른 날짜가 보일 수 있기 때문.
export function dayIndexOf(group) {
  if (!group) return 0;
  if (group.currentDay != null) return group.currentDay;
  return Math.min(CHALLENGE_LENGTH_DAYS, Math.floor((Date.now() - group.startedAt) / DAY_MS) + 1);
}

export function isChallengeLastDay(group) {
  return dayIndexOf(group) >= CHALLENGE_LENGTH_DAYS;
}

// 로그인 완료 후 보여줄 기본 화면 결정.
// 캐릭터 선택은 회원가입 직후 1회 필수 단계라 여기서 게이팅함. 목표/신체정보 온보딩은
// 이것과 별개로 그룹 생성(방장) 또는 참여(팀원) "이후"에 진행되므로 여기서 보지 않는다.
export function resolveHomeRoute(state) {
  if (!state.auth.userId) return '/';
  if (!state.character.species) return '/character';
  return '/my';
}
