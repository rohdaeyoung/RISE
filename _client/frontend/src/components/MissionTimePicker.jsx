import { formatClock } from '../api/missionApi';

const PRESETS = [
  { hour: 8, minute: 0 },
  { hour: 12, minute: 0 },
  { hour: 18, minute: 0 },
  { hour: 23, minute: 0 },
];

const ALL_SLOTS = Array.from({ length: 48 }, (_, i) => formatClock(i * 30));

export default function MissionTimePicker({ hour, minute, onChange }) {
  return (
    <div className="space-y-3">
      <div className="grid grid-cols-4 gap-2">
        {PRESETS.map((preset) => {
          const selected = preset.hour === hour && preset.minute === minute;
          return (
            <button
              key={`${preset.hour}-${preset.minute}`}
              type="button"
              onClick={() => onChange(preset.hour, preset.minute)}
              className={`rounded-2xl py-3 text-xs font-semibold border transition-colors ${
                selected
                  ? 'bg-brand text-white border-brand shadow-card'
                  : 'bg-card text-ink border-gray-300'
              }`}
            >
              {formatClock(preset.hour * 60 + preset.minute).label}
            </button>
          );
        })}
      </div>

      <select
        value={`${hour}:${minute}`}
        onChange={(e) => {
          const [h, m] = e.target.value.split(':').map(Number);
          onChange(h, m);
        }}
        className="w-full rounded-2xl bg-card border border-gray-300 px-4 py-3.5 text-sm text-ink outline-none focus:border-brand"
      >
        {ALL_SLOTS.map((slot) => (
          <option key={`${slot.hour}:${slot.minute}`} value={`${slot.hour}:${slot.minute}`}>
            {slot.label}
          </option>
        ))}
      </select>
    </div>
  );
}
