import type { ChartDataset, ChartType } from "chart.js";
import type { Data, Series } from "../types/chart.js";
import type { Metric, Metrics, Range } from "../types/data.js";

export interface Mapper<T = number> {
  rangeLabel: (metric: Metric<unknown>) => string;
  sampledRanges: (metrics: Metrics<T>) => Record<string, Range>;
  standard: (value: Metric<T>) => Series[];
  sampled: (value: Metric<T[]>, range: Range | null) => Series[];
}

/**
 * Converts `Metrics` to the corresponding chart data structures.
 */
export class ChartDataTransforms {

  static mapToSeries(metrics: Metrics<number>, mapper: Mapper<number>, normalize: boolean = false): Series[] {
    const series: Series[] = [];
    const standard = metrics.standard;
    const sampled = metrics.sampled;
    // Builds ranges for distribution.
    let ranges: Record<string, Range> = {};
    if (normalize) {
      ranges = mapper.sampledRanges(metrics);
    }
    // Builds series.
    if (standard) {
      for (let i = 0; i < standard.length; i += 1) {
        const metric = standard[i];
        const mapped = mapper.standard(metric);
        series.push(...mapped);
      }
    }
    if (sampled) {
      for (let i = 0; i < sampled.length; i += 1) {
        const metric = sampled[i];
        const mapped = mapper.sampled(metric, ranges[mapper.rangeLabel(metric)]);
        series.push(...mapped);
      }
    }
    return series;
  }

  static mapToDataset(series: Series[]): Data {
    let xmax = 0;
    let datasets: ChartDataset[] = [];
    for (let i = 0; i < series.length; i += 1) {
      xmax = Math.max(xmax, series[i].data.length);
      datasets.push(ChartDataTransforms.chartDataset(series[i]));
    }
    const chartData: Data = {
      labels: ChartDataTransforms.xrange(xmax),
      datasets: datasets
    };
    return chartData;
  }

  private static chartDataset<T extends ChartType>(series: Series): ChartDataset {
    return {
      label: series.descriptiveLabel,
      type: series.type,
      data: series.data,
      ...series.options
    };
  }

  private static xrange(xmax: number): number[] {
    let range = [];
    for (let i = 1; i <= xmax; i += 1) {
      range.push(i);
    }
    return range;
  }
}
