// 그룹 피드(오늘의 인증 피드) 반응·댓글.
// 세 엔드포인트 모두 갱신된 피드 전체를 돌려주므로, 반응/댓글을 남긴 직후에는 별도로
// 다시 조회하지 않고 응답을 그대로 반영한다 — 그 사이 다른 그룹원이 남긴 것까지 함께 받아온다.
// mock 모드에서는 AppContext의 TOGGLE_REACTION / ADD_COMMENT 리듀서가 로컬로 처리한다.

import { api, isBackendEnabled } from './client';

function toFeed(data) {
  if (!data) return null;
  return {
    reactions: data.reactions || {},
    comments: (data.comments || []).map((c) => ({
      id: c.id,
      text: c.text,
      authorLabel: c.authorNickname?.trim() || (c.me ? '나' : '그룹원'),
      createdAt: c.createdAt,
    })),
  };
}

export function fetchFeed() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/feed').then(toFeed);
}

// 같은 이모지를 다시 보내면 취소, 다른 이모지를 보내면 교체 — AppContext의 TOGGLE_REACTION과 같은 규칙.
export function toggleReaction(targetUserId, emoji) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post('/api/feed/reactions', { targetUserId: Number(targetUserId), emoji }).then(toFeed);
}

export function addComment(text) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post('/api/feed/comments', { text }).then(toFeed);
}
