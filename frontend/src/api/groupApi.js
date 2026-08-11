// 그룹 생성/참여.
// 백엔드 연동 시 POST /api/groups, POST /api/groups/join 을 호출해 실제 그룹원 목록을 받아오고,
// mock 모드에서는 기기 간 데이터가 연결되지 않아 항상 "나 혼자"인 그룹만 만들어진다.

import { api, isBackendEnabled } from './client';

const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const CODE_LENGTH = 6;
const MIN_MEMBERS = 2;
const MAX_MEMBERS = 4;

function generateCode() {
  let code = '';
  for (let i = 0; i < CODE_LENGTH; i++) {
    code += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)];
  }
  return code;
}

// 백엔드 멤버 응답을 프론트 상태가 쓰는 모양으로 변환.
// 나 자신은 AppContext가 별도로 들고 있으므로 그룹원 목록에서는 제외한다.
function toMembers(data, myUserId) {
  return (data.members || [])
    .filter((m) => String(m.userId) !== String(myUserId))
    .map((m) => ({
      id: String(m.userId),
      nickname: m.nickname,
      species: m.species,
      expression: (m.expression || 'normal').toLowerCase(),
      outfit: m.outfit,
      achievementRate: m.achievementRate ?? 0,
      points: m.points ?? 0,
    }));
}

function toGroup(data, myUserId) {
  return {
    id: String(data.id),
    code: data.code,
    name: data.name,
    members: toMembers(data, myUserId),
    missionHour: data.missionHour,
    missionMinute: data.missionMinute,
    currentDay: data.currentDay,
  };
}

// output: { id, code, members: [] }
export function createGroup({ name, missionHour, missionMinute, myUserId } = {}) {
  if (isBackendEnabled) {
    return api
      .post('/api/groups', { name, missionHour, missionMinute })
      .then((data) => toGroup(data, myUserId));
  }

  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ id: `grp-${Date.now()}`, code: generateCode(), members: [] });
    }, 400);
  });
}

// input: { code }. output: { id, code, members: [] } | throws { message }
export function joinGroup({ code, myUserId }) {
  if (isBackendEnabled) {
    return api
      .post('/api/groups/join', { code: code.trim().toUpperCase() })
      .then((data) => toGroup(data, myUserId));
  }

  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const normalized = code.trim().toUpperCase();
      if (normalized.length !== CODE_LENGTH) {
        reject({ message: '그룹 코드는 6자리예요' });
        return;
      }
      // 백엔드 연동 전에는 해당 코드로 실제 그룹이 존재하는지 확인할 수 없어 항상 성공 처리됨.
      resolve({ id: `grp-${normalized}`, code: normalized, members: [] });
    }, 400);
  });
}

// 그룹 피드/랭킹을 최신 상태로 유지하기 위한 조회 (mock 모드에서는 호출하지 않음).
export function fetchMyGroup({ myUserId } = {}) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/groups/me').then((data) => toGroup(data, myUserId));
}

// 방 이름 / 미션 시작 시간 변경 — 그룹원 누구나 바꿀 수 있다 (PRD 7. 그룹 시스템).
export function updateGroupSettings({ name, missionHour, missionMinute }) {
  if (!isBackendEnabled) return Promise.resolve(null);
  const requests = [];
  if (name) requests.push(api.patch('/api/groups/me/name', { name }));
  if (missionHour != null && missionMinute != null) {
    requests.push(api.patch('/api/groups/me/mission-time', { missionHour, missionMinute }));
  }
  return Promise.all(requests);
}

export function leaveGroup() {
  if (!isBackendEnabled) return Promise.resolve();
  return api.delete('/api/groups/me');
}

export { MIN_MEMBERS, MAX_MEMBERS };
