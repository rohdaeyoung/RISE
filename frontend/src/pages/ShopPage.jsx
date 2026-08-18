import { useState } from 'react';
import { useAppDispatch, useAppState, useAppSync } from '../context/AppContext';
import { buyOutfit, changeSpecies, wearOutfit } from '../api/profileApi';
import CharacterAvatar, { SPECIES_META } from '../components/CharacterAvatar';
import CoinIcon from '../components/CoinIcon';

const SPECIES_OPTIONS = Object.keys(SPECIES_META);

// 실제 캐릭터 아트가 있는 의상 세트만 노출. 기본 모습(everyday)은 캐릭터 아트에 이미 포함돼 있어
// 별도 상품으로 팔지 않음 — 나머지 세트는 코인으로 구매.
// 카드 썸네일은 별도 아이콘 이미지가 아니라 CharacterAvatar를 그대로 써서, 실제 착용 시 보이는
// 모습과 상점 미리보기가 항상 일치하도록 함.
// 구매(BUY_OUTFIT)와 착용(SET_OUTFIT) 모두 AppContext 리듀서에서 실제로 코인/보유 상태를 갱신함.
const OUTFIT_SETS = [
  { id: 'pajama', label: '파자마 세트', price: 30 },
  { id: 'sailor', label: '세일러 세트', price: 35 },
  { id: 'coat', label: '코트 세트', price: 40 },
  { id: 'detective', label: '탐정 세트', price: 50 },
];

const CARD_STYLES = ['bg-user-4/15', 'bg-user-1/15', 'bg-user-2/15', 'bg-user-3/15'];

export default function ShopPage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const sync = useAppSync();
  const species = state.character.species || 'bat';
  const equippedOutfit = state.character.outfit || 'everyday';
  const ownedOutfits = state.character.ownedOutfits || ['everyday'];
  const coins = state.coins || 0;
  const [previewOutfit, setPreviewOutfit] = useState(equippedOutfit);
  const [error, setError] = useState('');

  // 백엔드 모드에서는 코인 차감/보유 상태를 서버가 검증하므로 서버에 먼저 반영하고 동기화한다.
  // mock 모드에서는 아래 API들이 no-op이라 리듀서만으로 기존과 동일하게 동작한다.
  // 서버가 거절하면(코인 부족·미보유 등) 15초 뒤 sync()가 조용히 되돌리는 대신, 그 자리에서
  // 바로 되돌리고 사유를 보여준다.
  function handleSelectSpecies(value) {
    dispatch({ type: 'SET_CHARACTER', species: value });
    setError('');
    changeSpecies(value)
      .then(sync)
      .catch((e) => {
        setError(e?.message || '변경에 실패했어요. 잠시 후 다시 시도해주세요');
        sync();
      });
  }

  function handleEquip(outfitId) {
    setPreviewOutfit(outfitId);
    dispatch({ type: 'SET_OUTFIT', outfit: outfitId });
    setError('');
    wearOutfit(outfitId)
      .then(sync)
      .catch((e) => {
        setError(e?.message || '착용에 실패했어요. 잠시 후 다시 시도해주세요');
        sync();
      });
  }

  function handleBuy(item) {
    if (coins < item.price) return;
    dispatch({ type: 'BUY_OUTFIT', outfitId: item.id, price: item.price });
    setPreviewOutfit(item.id);
    setError('');
    buyOutfit(item.id)
      .then(sync)
      .catch((e) => {
        setError(e?.message || '구매에 실패했어요. 잠시 후 다시 시도해주세요');
        sync();
      });
  }

  return (
    <div className="px-5 pt-8 pb-24">
      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Shop</p>
      <h1 className="text-xl font-bold text-ink mb-5">상점</h1>

      <div className="relative rounded-3xl px-5 py-6 mb-5 text-white bg-brand shadow-card overflow-hidden">
        <div className="absolute -top-8 -right-8 w-32 h-32 rounded-full bg-white/10 blur-xl" />
        <div className="relative flex items-center justify-between">
          <div>
            <p className="text-xs text-white/75 mb-1">내 코인</p>
            <p className="text-2xl font-bold flex items-center gap-1.5">
              <CoinIcon className="w-6 h-6" />
              {coins}
            </p>
          </div>
          <p className="text-xs text-white/75 text-right leading-relaxed">
            미션을 완료하면
            <br />
            코인을 모을 수 있어요
          </p>
        </div>
      </div>

      <div className="flex justify-center mb-6 py-6">
        <CharacterAvatar species={species} expression="good" outfit={previewOutfit} size="xl" framed={false} />
      </div>

      {error && <p className="text-xs text-warn text-center mb-4">{error}</p>}

      <div className="flex items-center justify-between mb-3">
        <h2 className="text-sm font-semibold text-ink">캐릭터 꾸미기</h2>
        <span className="text-[11px] text-sub">캐릭터 변경은 무료예요</span>
      </div>
      <div className="flex gap-2 mb-6 overflow-x-auto pb-1 justify-center">
        {SPECIES_OPTIONS.map((value) => (
          <button
            key={value}
            onClick={() => handleSelectSpecies(value)}
            className={`flex flex-col items-center gap-1 rounded-2xl px-3 py-3 border flex-shrink-0 transition-colors ${
              species === value ? 'bg-brand-soft border-brand' : 'bg-card border-gray-300'
            }`}
          >
            <CharacterAvatar species={value} expression="good" size="sm" />
            <span className="text-xs font-medium text-ink">{SPECIES_META[value].label}</span>
          </button>
        ))}
      </div>

      <div className="flex items-center justify-between mb-3">
        <h2 className="text-sm font-semibold text-ink">캐릭터 의상 세트</h2>
        <span className="text-[11px] text-sub">{OUTFIT_SETS.length}종</span>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {OUTFIT_SETS.map((item, i) => {
          const style = CARD_STYLES[i % CARD_STYLES.length];
          const isEquipped = equippedOutfit === item.id;
          const isOwned = ownedOutfits.includes(item.id);
          const canAfford = coins >= item.price;
          return (
            <div
              key={item.id}
              className={`relative rounded-2xl border border-gray-300 ${style} p-4 flex flex-col items-center gap-2 shadow-card`}
            >
              <div className="w-20 h-20 rounded-2xl bg-white/70 flex items-center justify-center shadow-card mt-1 overflow-hidden">
                <CharacterAvatar species={species} outfit={item.id} expression="good" size="sm" framed={false} breathing={false} />
              </div>
              <p className="text-sm font-semibold text-ink text-center leading-tight">{item.label}</p>
              <p className={`text-xs font-semibold text-brand-dark flex items-center gap-1 ${isOwned ? 'invisible' : ''}`}>
                <CoinIcon className="w-3.5 h-3.5" />
                {item.price}
              </p>
              {isOwned ? (
                <button
                  onClick={() => handleEquip(isEquipped ? 'everyday' : item.id)}
                  className={`w-full mt-1 text-xs font-semibold rounded-full px-3 py-2 transition-colors ${
                    isEquipped
                      ? 'bg-brand-soft text-brand-dark'
                      : 'text-white bg-brand shadow-card'
                  }`}
                >
                  {isEquipped ? '착용 해제' : '착용하기'}
                </button>
              ) : (
                <button
                  onClick={() => handleBuy(item)}
                  disabled={!canAfford}
                  className={`w-full mt-1 text-xs font-semibold rounded-full px-3 py-2 transition-colors ${
                    canAfford
                      ? 'text-white bg-brand shadow-card'
                      : 'bg-black/10 text-sub'
                  }`}
                >
                  {canAfford ? '구매하기' : '코인 부족'}
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
