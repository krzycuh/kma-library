/** One hue per chart (tailwind.config.js `chart`): calm in the light theme, a neon tube each in the dark one. */
export type ChartColor = 1 | 2 | 3 | 4 | 5;

// Full class names, so Tailwind finds them in the source
export const chartBarClasses: Record<ChartColor, string> = {
  1: 'bg-chart-1',
  2: 'bg-chart-2',
  3: 'bg-chart-3',
  4: 'bg-chart-4',
  5: 'bg-chart-5',
};

/** The columns that step back when one of them is highlighted */
export const chartMutedBarClass = 'bg-chart-muted';
