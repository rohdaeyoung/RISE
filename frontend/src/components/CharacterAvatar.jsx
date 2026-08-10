// 캐릭터는 성별 구분 없이 동물의 숲 주민 같은 "동물" 컨셉으로 통일.
// 이미지 파일명 규칙: {species}-{outfit}-{expr}.png (예: bat-everyday-sad.png)
const IMAGE_MODULES = import.meta.glob('../assets/characters/*.png', { eager: true, import: 'default' });

const IMAGES = {};
for (const path in IMAGE_MODULES) {
  const [species, outfit, expr] = path.split('/').pop().replace('.png', '').split('-');
  ((IMAGES[species] ??= {})[outfit] ??= {})[expr] = IMAGE_MODULES[path];
}

const SPECIES_ORDER = ['bat', 'tit', 'dino', 'hedgehog'];
// 종(species) 이름이 아니라 캐릭터 고유 이름을 라벨로 사용 — 캐릭터 선택/상점 화면 모두 이 라벨을 그대로 씀.
const SPECIES_LABELS = { bat: '밤티', tit: '모쿠', dino: '뇽뇽이', hedgehog: '송이' };

// 표정 3단계: 기쁨(good) / 무표정(normal) / 슬픔(bad). 파일명의 happy/neutral/sad와 매핑.
const EXPRESSION_FILE_KEY = { good: 'happy', normal: 'neutral', bad: 'sad' };

const SPECIES_META = Object.fromEntries(
  SPECIES_ORDER.map((species) => [species, { label: SPECIES_LABELS[species] }]),
);

const SIZE_CLASSES = {
  xs: 'w-9 h-9',
  sm: 'w-14 h-14',
  md: 'w-24 h-24',
  lg: 'w-40 h-40',
  // 상점 프리뷰 등 "크지만 화면을 다 차지할 정도는 아닌" 크기.
  xl: 'w-60 h-60',
  // 마이 페이지 히어로용 — 폰 화면(max-w-md=448px) 폭 대부분을 차지하는 가장 큰 고정 크기.
  hero: 'w-96 h-96',
};

// 픽셀 기준 크기(Tailwind w-9/14/24/40/60/96에 대응) — 숨쉬기 강도 계산에 사용.
const SIZE_PX = { xs: 36, sm: 56, md: 96, lg: 160, xl: 240, hero: 384 };
// 작은 썸네일(종 선택 리스트 등)에는 숨쉬기를 적용하지 않음 — 개수가 많아 산만해지고
// 인계 가이드도 72px 미만은 정지 이미지를 권장함.
const BREATHING_ELIGIBLE_SIZES = new Set(['md', 'lg', 'xl', 'hero']);

// 작은 캐릭터는 픽셀 변화량이 작아 더 강하게, 큰 프로필은 차분하게 호흡한다.
function breathAmountFor(px) {
  if (px <= 80) return 0.035;
  if (px <= 128) return 0.029;
  return 0.024;
}

// User Signature Color 링 — Active Screen(그룹 피드/랭킹/결산 등)에서 사람을 구분할 때만
// accentIndex를 넘김. 안 넘기면(Calm Screen) 기존처럼 중립 흰 링을 유지.
export const ACCENT_RING_CLASSES = ['ring-user-1', 'ring-user-2', 'ring-user-3', 'ring-user-4'];

export default function CharacterAvatar({
  species = 'bat',
  expression = 'normal',
  outfit = 'everyday',
  size = 'md',
  breathing = true,
  breathDelay = 0,
  accentIndex = null,
  // false면 원형 프레임(배경 원+링)을 없애고 캐릭터 이미지만 보여줌 — 마이 페이지 히어로처럼
  // 캐릭터가 배경 위에 바로 서 있어야 하는 화면용.
  framed = true,
}) {
  const exprKey = EXPRESSION_FILE_KEY[expression] || EXPRESSION_FILE_KEY.normal;
  const image =
    IMAGES[species]?.[outfit]?.[exprKey] ||
    IMAGES[species]?.everyday?.[exprKey] ||
    IMAGES.bat.everyday[exprKey];

  const animate = breathing && BREATHING_ELIGIBLE_SIZES.has(size);
  const breathAmount = breathAmountFor(SIZE_PX[size] ?? SIZE_PX.md);
  const breathStyle = animate
    ? {
        '--breath-scale-y': 1 + breathAmount,
        '--breath-scale-x': 1 + breathAmount * 0.24,
        '--breath-delay': `${breathDelay}ms`,
      }
    : undefined;

  if (!framed) {
    return (
      <img
        src={image}
        alt={SPECIES_LABELS[species] || species}
        className={`${SIZE_CLASSES[size]} object-contain ${animate ? 'character-breathe' : ''}`}
        style={breathStyle}
        draggable={false}
      />
    );
  }

  const ringClass = accentIndex == null ? 'ring-white/70' : ACCENT_RING_CLASSES[accentIndex % ACCENT_RING_CLASSES.length];

  return (
    <div
      className={`${SIZE_CLASSES[size]} rounded-full overflow-hidden bg-brand-soft/50 flex items-center justify-center shadow-card ring-4 ${ringClass}`}
    >
      <img
        src={image}
        alt={SPECIES_LABELS[species] || species}
        className={`w-full h-full object-cover ${animate ? 'character-breathe' : ''}`}
        style={breathStyle}
        draggable={false}
      />
    </div>
  );
}

export { SPECIES_META };
