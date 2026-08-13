import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Dumbbell, Salad, Sprout } from 'lucide-react';
import { AGE_RANGE, HEIGHT_RANGE, WEIGHT_RANGE, useAppDispatch, useAppSync } from '../context/AppContext';
import { submitOnboarding } from '../api/profileApi';

const GOAL_OPTIONS = [
  { value: 'diet', label: '다이어트', Icon: Salad },
  { value: 'bulk', label: '벌크업', Icon: Dumbbell },
  { value: 'health', label: '건강관리', Icon: Sprout },
];

const GENDER_OPTIONS = [
  { value: 'female', label: '여성' },
  { value: 'male', label: '남성' },
];

const TOTAL_STEPS = 3;
const MIN_AGE = AGE_RANGE.min;
const MAX_AGE = AGE_RANGE.max;
const MIN_HEIGHT = HEIGHT_RANGE.min;
const MAX_HEIGHT = HEIGHT_RANGE.max;
const MIN_WEIGHT = WEIGHT_RANGE.min;
const MAX_WEIGHT = WEIGHT_RANGE.max;

export default function OnboardingPage() {
  const dispatch = useAppDispatch();
  const sync = useAppSync();
  const navigate = useNavigate();
  const [stepIdx, setStepIdx] = useState(0);
  const [form, setForm] = useState({ goal: null, gender: null, age: '', height: '', weight: '' });

  function goNext() {
    if (stepIdx < TOTAL_STEPS - 1) {
      setStepIdx(stepIdx + 1);
      return;
    }
    const onboarding = {
      goal: form.goal,
      gender: form.gender,
      age: Number(form.age),
      height: Number(form.height),
      weight: Number(form.weight),
    };
    dispatch({ type: 'SET_ONBOARDING', onboarding });
    // 백엔드 모드에서는 이 정보가 AI 미션 생성의 입력값이 되므로 서버에 먼저 저장한다.
    // 저장 직후 곧바로 동기화해서 서버가 만든 미션을 받아온다 — 이걸 안 부르면 다음 주기(15초)까지
    // 미션이 빈 채로 보인다.
    submitOnboarding(onboarding).then(() => sync()).finally(() => {
      // 캐릭터는 이미 회원가입 직후에 골랐고, 온보딩은 그룹 생성/참여 다음 단계라
      // 끝나면 곧장 그룹 피드로 간다.
      navigate('/group');
    });
  }

  // 입력을 강제로 고쳐쓰지 않음 — 사용자는 범위 밖의 숫자(예: 몸무게 18)도 자유롭게 입력할 수 있고,
  // 대신 범위를 벗어나면 다음 단계로 못 넘어가게(버튼 비활성화 + 안내 문구) 막는 방식.
  function handleNumberFieldChange(field, rawValue) {
    const digitsOnly = rawValue.replace(/[^0-9]/g, '');
    setForm({ ...form, [field]: digitsOnly });
  }

  // 숫자 입력에서 소수점(.)과 지수 표기(e), 부호(+/-) 키 자체를 막아 정수만 입력되게 함.
  function blockNonIntegerKeys(e) {
    if (['.', ',', 'e', 'E', '+', '-'].includes(e.key)) {
      e.preventDefault();
    }
  }

  function isInRange(rawValue, min, max) {
    if (rawValue === '') return false;
    const n = Number(rawValue);
    return n >= min && n <= max;
  }

  const ageValid = isInRange(form.age, MIN_AGE, MAX_AGE);
  const heightValid = isInRange(form.height, MIN_HEIGHT, MAX_HEIGHT);
  const weightValid = isInRange(form.weight, MIN_WEIGHT, MAX_WEIGHT);

  const canProceed =
    (stepIdx === 0 && form.goal) ||
    (stepIdx === 1 && form.gender && ageValid) ||
    (stepIdx === 2 && heightValid && weightValid);

  return (
    <div className="min-h-svh flex flex-col px-6 py-10">
      <div className="flex gap-2 mb-10">
        {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
          <div key={i} className={`h-1.5 flex-1 rounded-full ${i <= stepIdx ? 'bg-brand' : 'bg-black/10'}`} />
        ))}
      </div>
      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-2">
        Step {stepIdx + 1} of {TOTAL_STEPS}
      </p>

      {stepIdx === 0 && (
        <>
          <h2 className="text-xl font-bold text-ink mb-8">어떤 목표를 원하시나요?</h2>
          <div className="grid grid-cols-3 gap-3">
            {GOAL_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setForm({ ...form, goal: opt.value })}
                className={`flex flex-col items-center justify-center gap-2 rounded-2xl py-7 border text-sm font-medium transition-colors ${
                  form.goal === opt.value
                    ? 'bg-brand text-white border-brand shadow-card'
                    : 'bg-card text-ink border-gray-300 shadow-card'
                }`}
              >
                <opt.Icon size={26} />
                {opt.label}
              </button>
            ))}
          </div>
        </>
      )}

      {stepIdx === 1 && (
        <>
          <h2 className="text-xl font-bold text-ink mb-8">성별과 나이를 알려주세요</h2>
          <p className="text-xs text-sub -mt-6 mb-6">그룹 피드에는 공개되지 않아요</p>
          <div className="grid grid-cols-2 gap-3 mb-6">
            {GENDER_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setForm({ ...form, gender: opt.value })}
                className={`rounded-2xl py-5 border text-sm font-medium transition-colors ${
                  form.gender === opt.value
                    ? 'bg-brand text-white border-brand shadow-card'
                    : 'bg-card text-ink border-gray-300 shadow-card'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
          <input
            type="number"
            inputMode="numeric"
            step="1"
            min={MIN_AGE}
            max={MAX_AGE}
            value={form.age}
            onChange={(e) => handleNumberFieldChange('age', e.target.value)}
            onKeyDown={blockNonIntegerKeys}
            placeholder="나이"
            className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand"
          />
          {form.age !== '' && !ageValid && (
            <p className="text-xs text-warn mt-2">
              나이는 {MIN_AGE}~{MAX_AGE}세 사이로 입력해주세요
            </p>
          )}
        </>
      )}

      {stepIdx === 2 && (
        <>
          <h2 className="text-xl font-bold text-ink mb-8">키와 몸무게를 알려주세요</h2>
          <div className="space-y-1">
            <div>
              <input
                type="number"
                inputMode="numeric"
                step="1"
                min={MIN_HEIGHT}
                value={form.height}
                onChange={(e) => handleNumberFieldChange('height', e.target.value)}
                onKeyDown={blockNonIntegerKeys}
                placeholder="키 (cm)"
                className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand transition-colors"
              />
              {form.height !== '' && !heightValid && (
                <p className="text-xs text-warn mt-2">키는 {MIN_HEIGHT}cm 이상으로 입력해주세요</p>
              )}
            </div>
            <div>
              <input
                type="number"
                inputMode="numeric"
                step="1"
                min={MIN_WEIGHT}
                value={form.weight}
                onChange={(e) => handleNumberFieldChange('weight', e.target.value)}
                onKeyDown={blockNonIntegerKeys}
                placeholder="몸무게 (kg)"
                className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand transition-colors"
              />
              {form.weight !== '' && !weightValid && (
                <p className="text-xs text-warn mt-2">몸무게는 {MIN_WEIGHT}kg 이상으로 입력해주세요</p>
              )}
            </div>
          </div>
        </>
      )}

      <button
        onClick={goNext}
        disabled={!canProceed}
        className="mt-auto w-full bg-brand disabled:bg-black/10 disabled:text-sub text-white rounded-full py-4 text-sm font-semibold shadow-card disabled:shadow-none transition-colors"
      >
        {stepIdx < TOTAL_STEPS - 1 ? '다음' : '완료'}
      </button>
    </div>
  );
}
