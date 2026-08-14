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
  // 이 사진으로 인증하게 될 식단 미션. 서버도 "오늘 미완료 식단 미션 중 첫 번째"를 기준으로
  // 판정하므로(MissionService.pendingDietMissionTitle) 같은 규칙으로 골라야 화면과 판정이 맞는다.
  // 미션 제목을 안 보여주면 모든 미션에서 "아침 사진 업로드"만 떠서 무엇을 찍어야 할지 알 수 없다.
  // 페이지 진입 시점에 한 번만 계산해 고정한다 — 매 렌더마다 state.missions에서 다시 찾으면,
  // 인증 성공으로 LOG_MEAL이 이 미션을 done 처리하는 순간 "다음 미완료 식단 미션"으로 넘어가
  // 완료 화면을 보는 도중에 제목이 다음 미션 걸로 바뀌어 버린다.
  const [dietMission] = useState(() => state.missions.find((m) => m.type === 'diet' && !m.done));

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
      <h1 className="text-lg font-bold text-ink mb-1">{dietMission ? dietMission.title : `${label} 사진 업로드`}</h1>
      <p className="text-sm text-sub mb-6">인증 사진을 올리면 AI가 확인해요</p>

      <label className="block rounded-2xl border-2 border-dashed border-brand/20 aspect-square flex items-center justify-center overflow-hidden mb-6 cursor-pointer bg-brand-soft/40">
        <input type="file" accept="image/*" capture="environment" className="hidden" onChange={handleFile} />
        {preview ? (
          <img src={preview} alt="업로드한 식사 사진" className="w-full h-full object-cover" />
        ) : (
          <span className="text-sub text-sm flex flex-col items-center gap-2">
            <Camera size={30} />
            인증 사진을 선택해주세요
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
