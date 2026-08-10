// 이메일 회원가입/로그인 mock. 백엔드 연동 시 아래 함수 내부만
// POST /api/auth/signup, POST /api/auth/login, GET /api/auth/me 호출로 교체하면 됨.
// 지금은 localStorage를 가짜 유저 테이블로 써서 중복 이메일 체크만 프론트에서 흉내냄.

const USERS_KEY = 'withu_users_mock';

function loadUsers() {
  try {
    return JSON.parse(localStorage.getItem(USERS_KEY)) || [];
  } catch {
    return [];
  }
}

function saveUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users));
}

// input: { email, password }. output: { userId, email } | throws { field, message }
export function signUp({ email, password }) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const users = loadUsers();
      if (users.some((u) => u.email === email)) {
        reject({ field: 'email', message: '이미 가입된 이메일이에요' });
        return;
      }
      const user = { userId: `u-${Date.now()}`, email, password };
      saveUsers([...users, user]);
      resolve({ userId: user.userId, email: user.email });
    }, 500);
  });
}

// input: { email, password }. output: { userId, email } | throws { field, message }
export function login({ email, password }) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const users = loadUsers();
      const user = users.find((u) => u.email === email);
      if (!user || user.password !== password) {
        reject({ field: 'password', message: '이메일 또는 비밀번호가 올바르지 않아요' });
        return;
      }
      resolve({ userId: user.userId, email: user.email });
    }, 500);
  });
}
