// 캐릭터 / 온보딩 / 상점 / 랭킹 — 백엔드 연동 전용 모듈.
// mock 모드에서는 이 함수들이 호출되지 않고 AppContext의 로컬 상태만으로 동작한다.

import { api, isBackendEnabled } from './client';

export function createCharacter(species) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post('/api/characters', { species });
}

export function changeSpecies(species) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.patch('/api/characters/me/species', { species });
}

export function submitOnboarding({ goal, gender, age, height, weight }) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post('/api/onboarding', {
    goal: goal?.toUpperCase(),
    gender: gender?.toUpperCase(),
    age,
    height,
    weight,
  });
}

export function fetchMe() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/auth/me');
}

// 닉네임은 랭킹·그룹 피드에 표시되는 이름이라 서버에 저장해야 다른 그룹원에게도 보인다.
export function saveNickname(nickname) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.patch('/api/auth/me/nickname', { nickname: nickname.trim() });
}

// 캐릭터의 종/의상/보유 의상은 서버가 원본이다. 이걸 안 받아오면 상점에서 산 의상이
// 새로고침하거나 다른 기기에서 열었을 때 없어진 것처럼 보인다.
export function fetchCharacter() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/characters/me').then((c) => ({
    species: c.species,
    outfit: c.outfit,
    ownedOutfits: c.ownedOutfits,
  }));
}

export function buyOutfit(outfitId) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post(`/api/shop/outfits/${outfitId}/buy`);
}

export function wearOutfit(outfitId) {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.post(`/api/shop/outfits/${outfitId}/wear`);
}

export function fetchGroupRanking() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/rankings/group').then((d) => d.ranking);
}

export function fetchGlobalRanking() {
  if (!isBackendEnabled) return Promise.resolve(null);
  return api.get('/api/rankings/global').then((d) => d.ranking);
}
