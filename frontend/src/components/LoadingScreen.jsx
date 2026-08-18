import { Loader2 } from 'lucide-react';

// 라우트 코드가 아직 로딩 중일 때(Suspense fallback) 화면과 화면 사이에 잠깐 보여주는 로딩 화면.
export default function LoadingScreen() {
  return (
    <div className="min-h-svh max-w-md mx-auto flex items-center justify-center bg-cream">
      <Loader2 size={28} className="animate-spin text-brand" />
    </div>
  );
}
