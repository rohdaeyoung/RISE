import { useEffect, useRef, useState } from 'react';
import { Send, X } from 'lucide-react';
import { MAX_COMMENT_LENGTH, useAppDispatch, useAppState } from '../context/AppContext';

function formatCommentTimestamp(createdAt) {
  const d = new Date(createdAt);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${d.getMonth() + 1}월 ${d.getDate()}일 ${hh}:${mm}`;
}

// "오늘의 인증 피드" 전체에 달리는 댓글 목록 + 입력창을 담은 하단 시트.
export default function CommentSheet({ onClose }) {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const [text, setText] = useState('');
  const listRef = useRef(null);
  const commentCount = state.comments.length;

  // 댓글이 늘어나면(=새로 등록되면) 방금 올라온 댓글이 화면에 보이도록 목록 맨 아래로 스크롤.
  useEffect(() => {
    const list = listRef.current;
    if (list) list.scrollTo({ top: list.scrollHeight, behavior: 'smooth' });
  }, [commentCount]);

  function handleSubmit(e) {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    dispatch({ type: 'ADD_COMMENT', text: trimmed });
    setText('');
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-end justify-center z-50" onClick={onClose}>
      <div
        className="w-full max-w-md bg-card rounded-t-sheet pt-6 max-h-[80svh] flex flex-col shadow-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-10 h-1.5 rounded-full bg-black/10 mx-auto mb-5 flex-shrink-0" />
        <div className="flex items-center justify-between px-6 mb-4 flex-shrink-0">
          <h2 className="text-base font-bold text-ink">
            오늘의 댓글{state.comments.length > 0 ? ` (${state.comments.length})` : ''}
          </h2>
          <button onClick={onClose} aria-label="닫기" className="text-sub">
            <X size={20} />
          </button>
        </div>

        <div ref={listRef} className="flex-1 overflow-y-auto px-6 space-y-2.5">
          {state.comments.length === 0 ? (
            <p className="text-sm text-sub text-center py-10">아직 댓글이 없어요. 첫 댓글을 남겨보세요!</p>
          ) : (
            state.comments.map((c) => (
              <div key={c.id} className="comment-enter rounded-2xl border border-gray-300 bg-cream px-3.5 py-2.5">
                <div className="flex items-center justify-between gap-2 mb-1">
                  <p className="text-xs font-semibold text-ink truncate">{c.authorLabel}</p>
                  <p className="text-[11px] text-sub flex-shrink-0">{formatCommentTimestamp(c.createdAt)}</p>
                </div>
                <p className="text-sm text-ink/90 whitespace-pre-wrap break-words">{c.text}</p>
              </div>
            ))
          )}
        </div>

        <form onSubmit={handleSubmit} className="flex items-center gap-2 px-6 py-4 border-t border-gray-300 flex-shrink-0">
          <input
            value={text}
            onChange={(e) => setText(e.target.value.slice(0, MAX_COMMENT_LENGTH))}
            placeholder="댓글을 남겨보세요"
            className="flex-1 min-w-0 bg-cream rounded-full px-4 py-2.5 text-sm outline-none border border-gray-300"
          />
          <button
            type="submit"
            disabled={!text.trim()}
            aria-label="댓글 등록"
            className="w-10 h-10 flex items-center justify-center rounded-full bg-brand text-white disabled:opacity-40 flex-shrink-0"
          >
            <Send size={16} />
          </button>
        </form>
      </div>
    </div>
  );
}
