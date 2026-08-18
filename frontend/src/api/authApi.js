// 이메일 회원가입/로그인.
// VITE_API_BASE_URL이 설정되면 실제 백엔드(POST /api/auth/signup, /api/auth/login)를 호출하고,
// 없으면 기존처럼 localStorage를 가짜 유저 테이블로 쓰는 mock으로 동작한다.

import { api, isBackendEnabled, setToken, clearToken } from './client';

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
  if (isBackendEnabled) {
    return api.post('/api/auth/signup', { email, password }).then((data) => {
      setToken(data.accessToken);
      return { userId: data.userId, email: data.email };
    });
  }

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
  if (isBackendEnabled) {
    return api.post('/api/auth/login', { email, password }).then((data) => {
      setToken(data.accessToken);
      return { userId: data.userId, email: data.email };
    });
  }

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

export function logout() {
  clearToken();
}

// 계정 탈퇴 — 서버에서 계정과 모든 기록을 지운다.
// 예전에는 브라우저 저장소만 비웠다. 화면에서만 사라지고 DB에는 그대로 남아,
// 같은 이메일로 다시 가입하면 "이미 가입된 이메일"이 뜨고 전체 랭킹에도 계속 나왔다.
// 토큰은 서버 삭제가 끝난 뒤에 지운다 — 먼저 지우면 삭제 요청이 401로 거절된다.
export function deleteAccount(email) {
  if (!isBackendEnabled) {
    // mock 모드에도 가짜 유저 테이블이 있어서, 여기서 안 지우면 같은 이메일로 재가입이 안 된다.
    saveUsers(loadUsers().filter((u) => u.email !== email));
    clearToken();
    return Promise.resolve();
  }
  return api.delete('/api/auth/me').then(() => clearToken());
}
