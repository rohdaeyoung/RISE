import { useNavigate } from 'react-router-dom';
import { Users } from 'lucide-react';

export default function GroupEntryPage() {
  const navigate = useNavigate();

  return (
    <div className="relative min-h-svh flex flex-col justify-center px-6 py-10">
      <button onClick={() => navigate(-1)} className="absolute top-6 left-6 text-sub text-sm">
        ← 뒤로
      </button>

      <div className="text-center mb-10">
        <div className="w-16 h-16 mx-auto mb-4 rounded-2xl flex items-center justify-center text-white bg-brand shadow-card">
          <Users size={28} />
        </div>
        <h1 className="text-xl font-bold text-ink">친구들과 함께 시작해요</h1>
        <p className="text-sm text-sub mt-2">2~4인 그룹으로만 진행할 수 있어요. 그룹을 만들거나 코드로 참여해주세요</p>
      </div>

      <div className="space-y-3">
        <button
          onClick={() => navigate('/group-entry/create')}
          className="w-full bg-brand text-white rounded-full py-4 text-sm font-semibold shadow-card"
        >
          그룹 만들기
        </button>
        <button
          onClick={() => navigate('/group-entry/join')}
          className="w-full bg-card border border-gray-300 text-ink rounded-full py-4 text-sm font-semibold"
        >
          코드로 참여하기
        </button>
      </div>
    </div>
  );
}
