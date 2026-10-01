export type MetricTrend = 'up' | 'down' | 'neutral';

export interface MetricCard {
  title: string;
  value: string;
  change: string;
  trend: MetricTrend;
  icon: string;
  colorClass: string;
  description: string;
}
