// 그룹 생성/참여 mock. 백엔드 연동 시:
//   createGroup -> POST /api/groups (host 생성, 초대코드 발급)
//   joinGroup   -> POST /api/groups/join { code } (코드 검증 후 참여, 최대 4인, 멤버 목록 응답)
// 로 교체.
// 지금은 기기 간 데이터가 연결되지 않아, 생성/참여 모두 "나" 혼자인 그룹만 만들어짐.
// 실제 그룹원은 백엔드가 붙어 다른 사용자가 같은 코드로 참여해야 채워짐 (가짜 멤버를 만들지 않음).

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

// output: { id, code, members: [] } — 생성 시점엔 나만 있고 그룹원은 아직 없음
export function createGroup() {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ id: `grp-${Date.now()}`, code: generateCode(), members: [] });
    }, 400);
  });
}

// input: { code }. output: { id, code, members: [] } | throws { message }
export function joinGroup({ code }) {
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

export { MIN_MEMBERS, MAX_MEMBERS };
