import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Bot, Camera, CheckCircle2 } from 'lucide-react';
import { MEAL_LABELS, resolveHomeRoute, useAppDispatch, useAppState, useAppSync } from '../context/AppContext';
import { analyzeMealPhoto } from '../api/mealApi';
import { resizeImageFile } from '../utils/resizeImage';
import CharacterAvatar from '../components/CharacterAvatar';

export default function MealUploadPage() {
  const { mealKey } = useParams();
  const state = useAppState();
  const dispatch = useAppDispatch();
  const sync = useAppSync();
  const navigate = useNavigate();

  const [preview, setPreview] = useState(null);
  const [status, setStatus] = useState('idle'); // idle | loading | done
  const [achieved, setAchieved] = useState(null);
  const [error, setError] = useState('');

  const label = MEAL_LABELS[mealKey] || '식사';

  function handleFile(e) {
    const file = e.target.files?.[0];
    if (!file) return;

    setStatus('loading');
    setError('');

    Promise.all([resizeImageFile(file), analyzeMealPhoto(file, mealKey)])
      .then(([thumbnail, result]) => {
        setPreview(thumbnail);
        setAchieved(result.achieved);
        setStatus('done');
        // 사용자에게는 달성/미달성만 노출. result.internalFit은 다음 미션 생성용 내부 데이터라 화면에 쓰지 않음.
        dispatch({ type: 'LOG_MEAL', mealKey, achieved: result.achieved, photo: thumbnail });
        // 백엔드 모드에서는 서버가 식단 미션 완료와 코인 지급까지 처리하므로 최신 상태를 다시 받아온다.
        sync();
      })
      .catch((err) => {
        setStatus('idle');
        setError(err?.message || '분석에 실패했어요. 잠시 후 다시 시도해주세요');
      });
  }

  return (
    <div className="min-h-svh px-5 pt-8 pb-10 flex flex-col">
      <button onClick={() => navigate(-1)} className="text-sub text-sm mb-4 text-left w-fit">
        ← 뒤로
      </button>

      <p className="text-xs font-bold tracking-wide text-brand-dark uppercase mb-1">Meal Check</p>
      <h1 className="text-lg font-bold text-ink mb-1">{label} 사진 업로드</h1>
      <p className="text-sm text-sub mb-6">AI가 오늘 미션 달성 여부를 확인해요</p>

      <label className="block rounded-2xl border-2 border-dashed border-brand/20 aspect-square flex items-center justify-center overflow-hidden mb-6 cursor-pointer bg-brand-soft/40">
        <input type="file" accept="image/*" capture="environment" className="hidden" onChange={handleFile} />
        {preview ? (
          <img src={preview} alt="업로드한 식사 사진" className="w-full h-full object-cover" />
        ) : (
          <span className="text-sub text-sm flex flex-col items-center gap-2">
            <Camera size={30} />
            {label} 사진을 선택해주세요
          </span>
        )}
      </label>

      {error && <p className="text-xs text-warn text-center mb-4">{error}</p>}

      {status === 'loading' && (
        <div className="text-center text-sub text-sm py-4">
          <Bot size={26} className="mx-auto mb-2 animate-pulse" />
          AI가 식단을 분석하고 있어요...
        </div>
      )}

      {status === 'done' && (
        <div className="bg-card rounded-2xl border border-gray-300 shadow-card p-5 text-center">
          <p className="text-xs text-sub mb-3">분석 결과, 내 캐릭터가 이렇게 반응해요</p>
          <div className="flex justify-center mb-3">
            <CharacterAvatar
              expression={achieved ? 'good' : 'bad'}
              species={state.character.species}
              size="md"
            />
          </div>
          <p className={`text-sm font-semibold mb-6 flex items-center justify-center gap-1.5 ${achieved ? 'text-brand-dark' : 'text-warn'}`}>
            {achieved && <CheckCircle2 size={18} />}
            {achieved ? '오늘 식단 미션 달성!' : '조금 아쉬워요, 다음 끼니에 다시 도전해봐요'}
          </p>

          <button
            onClick={() => navigate(resolveHomeRoute(state))}
            className="w-full bg-brand text-white rounded-full py-3.5 text-sm font-semibold"
          >
            돌아가기
          </button>
        </div>
      )}
    </div>
  );
}
