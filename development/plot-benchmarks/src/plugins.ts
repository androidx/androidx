import type { Chart, ChartType, Plugin, UpdateMode } from "chart.js";

export interface LegendOptions {
  'onUpdate'?: (chart: Chart) => void;
}

export const LegendPlugin: Plugin<ChartType, LegendOptions> = {
  id: 'benchmark',
  afterUpdate: (chart: Chart, args: { mode: UpdateMode }, options: LegendOptions) => {
    if (options.onUpdate) {
      options.onUpdate(chart);
    }
  }
};
