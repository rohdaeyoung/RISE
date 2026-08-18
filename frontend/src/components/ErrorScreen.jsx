import { AlertTriangle, Home, RotateCcw } from 'lucide-react';

// 렌더링 중 오류가 나거나(ErrorBoundary) 화면 코드 로딩에 실패했을 때 보여주는 오류 화면.
export default function ErrorScreen({ onRetry, onGoHome }) {
  return (
    <div className="min-h-svh max-w-md mx-auto flex flex-col items-center justify-center bg-cream px-8 text-center gap-4">
      <div className="w-14 h-14 rounded-full bg-warn-soft flex items-center justify-center">
        <AlertTriangle size={26} className="text-warn" />
      </div>
      <div>
        <p className="text-base font-bold text-ink mb-1">문제가 발생했어요</p>
        <p className="text-sm text-sub">화면을 불러오는 중 오류가 났어요. 다시 시도해주세요.</p>
      </div>
      <div className="w-full max-w-xs space-y-2 mt-2">
        <button
          onClick={onRetry}
          className="w-full bg-brand text-white rounded-full py-3 text-sm font-semibold flex items-center justify-center gap-1.5"
        >
          <RotateCcw size={15} /> 다시 시도
        </button>
        <button onClick={onGoHome} className="w-full text-sub text-sm py-2 flex items-center justify-center gap-1.5">
          <Home size={14} /> 처음으로
        </button>
      </div>
    </div>
  );
}
