type StatusTone = 'positive' | 'warning' | 'negative' | 'neutral';

export function StatusBadge({ label, tone = 'neutral' }: { label: string; tone?: StatusTone }) {
  return <span className={`status status-${tone}`}>{label}</span>;
}
