import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MAX_NICKNAME_LENGTH, resolveHomeRoute, useAppDispatch, useAppState } from '../context/AppContext';
import { createCharacter, saveNickname } from '../api/profileApi';
import CharacterAvatar, { SPECIES_META } from '../components/CharacterAvatar';

const SPECIES_OPTIONS = Object.keys(SPECIES_META);

export default function CharacterCreatePage() {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [species, setSpecies] = useState(SPECIES_OPTIONS[0]);
  const [nickname, setNickname] = useState('');

  const nicknameValid = nickname.trim().length > 0;
  const existingSpecies = state.character.species;

  // 새 기기에서 로그인하면 캐릭터 정보가 서버에서 한 박자 늦게 도착해, 그 사이 이 화면으로 넘어온다.
  // 이미 만들어둔 캐릭터가 있는데 다시 고르라고 물으면 안 되므로 복원되는 즉시 홈으로 보낸다.
  useEffect(() => {
    if (existingSpecies) navigate(resolveHomeRoute(state), { replace: true });
    // state 전체를 의존성에 넣으면 매 동기화마다 실행되므로 종(species)만 본다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [existingSpecies]);

  // 캐릭터 선택은 회원가입 직후 1회 필수 단계 — 끝나면 그냥 일반 홈으로.
  function handleStart() {
    if (!nicknameValid) return;
    // 백엔드 모드에서는 캐릭터를 서버에 만들어 계정에 귀속시킨다(mock 모드에서는 no-op).
    // 이미 만들어져 있어도(재진입) 로컬 진행은 막지 않는다.
    // 닉네임도 서버에 올려야 그룹 피드·랭킹에서 다른 사람에게 이름이 보인다(mock 모드에서는 no-op).
    Promise.all([createCharacter(species), saveNickname(nickname).catch(() => {})]).finally(() => {
      dispatch({ type: 'SET_CHARACTER', species });
      dispatch({ type: 'SET_NICKNAME', nickname });
      navigate(resolveHomeRoute({ ...state, character: { ...state.character, species } }));
    });
  }

  return (
    <div className="min-h-svh flex flex-col px-6 py-10">
      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-2">내 캐릭터 선택</p>
      <h2 className="text-xl font-bold text-ink mb-1">함께할 캐릭터를 골라 보세요</h2>
      <p className="text-sm text-sub mb-8">챌린지가 끝나도 계속 함께하는 나만의 동물 캐릭터예요</p>

      <div className="flex flex-col items-center gap-3 mb-10 py-8 rounded-3xl bg-brand-soft/60">
        <CharacterAvatar species={species} expression="good" size="lg" />
        <p className="text-sm font-semibold text-ink">{SPECIES_META[species].label}</p>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-6">
        {SPECIES_OPTIONS.map((value) => (
          <button
            key={value}
            onClick={() => setSpecies(value)}
            className={`flex flex-col items-center gap-2 rounded-2xl py-5 border transition-colors ${
              species === value
                ? 'bg-brand-soft border-brand shadow-card'
                : 'bg-card border-gray-300'
            }`}
          >
            <CharacterAvatar species={value} expression="good" size="sm" />
            <span className="text-sm font-medium text-ink">{SPECIES_META[value].label}</span>
          </button>
        ))}
      </div>

      <div className="mb-8">
        <label className="text-xs font-medium text-sub mb-1 block">닉네임</label>
        <input
          type="text"
          value={nickname}
          onChange={(e) => setNickname(e.target.value.slice(0, MAX_NICKNAME_LENGTH))}
          placeholder="그룹원들에게 보여질 닉네임을 정해주세요"
          maxLength={MAX_NICKNAME_LENGTH}
          className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand"
        />
        {!nicknameValid && <p className="text-xs text-sub mt-2 px-1">닉네임을 입력해야 시작할 수 있어요</p>}
      </div>

      <button
        onClick={handleStart}
        disabled={!nicknameValid}
        className="mt-auto w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold shadow-card disabled:shadow-none transition-colors"
      >
        이 캐릭터로 시작하기
      </button>
    </div>
  );
}
