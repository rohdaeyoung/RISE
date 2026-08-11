// 백엔드 연동 스위치.
//
// VITE_API_BASE_URL이 설정돼 있으면 실제 백엔드(Spring)를 호출하고,
// 비어 있으면 기존 mock 구현이 그대로 동작한다 — 프론트만 단독으로 띄우거나
// (Netlify 정적 배포처럼) 백엔드 없이 데모할 때를 위해 분리해 둔 것.
//
// 사용법:
//   .env.local 에  VITE_API_BASE_URL=http://localhost:8080  → 백엔드 연동
//   해당 값이 없으면                                        → mock 모드

const BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') || '';
const TOKEN_KEY = 'withu_token';

export const isBackendEnabled = Boolean(BASE_URL);

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// 백엔드는 모든 응답을 { success, data, error } 로 감싸므로 여기서 벗겨내고,
// 실패 시에는 mock이 던지던 것과 같은 모양({ field, message })으로 맞춰 던진다.
async function request(path, { method = 'GET', body, isForm = false } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (!isForm) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: isForm ? body : body ? JSON.stringify(body) : undefined,
  });

  let payload = null;
  try {
    payload = await res.json();
  } catch {
    payload = null;
  }

  if (!res.ok || payload?.success === false) {
    const error = payload?.error;
    throw { field: error?.field ?? null, message: error?.message ?? '요청을 처리하지 못했어요' };
  }
  return payload?.data ?? null;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
  postForm: (path, formData) => request(path, { method: 'POST', body: formData, isForm: true }),
};
