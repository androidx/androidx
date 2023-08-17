import type { Point } from "chart.js";
import type { Series } from "../types/chart.js";
import type { ChartData, Metric, Metrics, Range } from "../types/data.js";
import type { Mapper } from "./data-transforms.js";

function sampledRanges(metrics: Metrics<number>): Record<string, Range> {
  const ranges: Record<string, Range> = {};
  const sampled = metrics.sampled;
  if (sampled) {
    for (let i = 0; i < sampled.length; i += 1) {
      const metric = sampled[i];
      const label = rangeLabel(metric);
      let range = ranges[label];
      if (!range) {
        range = {
          label: label,
          min: Number.MAX_VALUE,
          max: Number.MIN_VALUE
        };
      }
      const data: Record<string, ChartData<number[]>> = metric.data;
      const chartData: ChartData<number[]>[] = Object.values(data);
      for (let j = 0; j < chartData.length; j++) {
        const values = chartData[j].values.flat();
        for (let k = 0; k < values.length; k++) {
          if (values[k] < range.min) {
            range.min = values[k];
          }
          if (values[k] > range.max) {
            range.max = values[k];
          }
        }
      }
      ranges[label] = range;
    }
  }
  return ranges;
}

function sampledMapper(metric: Metric<number[]>, range: Range | null): Series[] {
  const series: Series[] = [];
  const data: Record<string, ChartData<number[]>> = metric.data;
  const entries = Object.entries(data);
  for (let i = 0; i < entries.length; i += 1) {
    const [source, chartData] = entries[i];
    const label = labelFor(metric, source);
    const [points, _, __] = histogramPoints(chartData.values, /* buckets */ undefined, /* target */ undefined, range);
    series.push({
      label: label,
      type: "line",
      data: points,
      options: {
        tension: 0.3
      }
    });
  }
  return series;
}

function standardMapper(metric: Metric<number>): Series[] {
  const series: Series[] = [];
  const data: Record<string, ChartData<number>> = metric.data;
  const entries = Object.entries(data);
  for (let i = 0; i < entries.length; i += 1) {
    const [source, chartData] = entries[i];
    const label = labelFor(metric, source);
    const points = singlePoints(chartData.values);
    series.push({
      label: label,
      type: "line",
      data: points,
      options: {
        tension: 0.3
      }
    });
  }
  return series;
}

export function histogramPoints(
  runs: number[][],
  buckets: number = 100,
  target: number | null = null,
  range: Range | null = null,
): [Point[], Point[] | null, number | null] {
  const flattened = runs.flat();
  // Actuals
  let min: number;
  let max: number;
  if (range) {
    min = range.min;
    max = range.max;
  } else {
    // Use a custom comparator, given the default coerces numbers
    // to a string type.
    flattened.sort((a, b) => a - b);
    // Natural Ranges
    const nmin = flattened[0];
    const nmax = flattened[flattened.length - 1];
    min = nmin;
    max = nmax;
  }
  let targetPoints: Point[] | null = null;
  let pMin: number = 0;
  let pMax: number = 0;
  let maxFreq: number = 0;
  const histogram = new Array(buckets).fill(0);
  // The actual number of slots in the histogram
  const slots = buckets - 1;
  for (let i = 0; i < flattened.length; i += 1) {
    const value = flattened[i];
    if (target && value < target) {
      pMin += 1;
    }
    if (target && value >= target) {
      pMax += 1;
    }
    const n = normalize(value, min, max);
    const index = Math.ceil(n * slots);
    histogram[index] = histogram[index] + 1;
    if (maxFreq < histogram[index]) {
      maxFreq = histogram[index];
    }
  }
  if (target) {
    const n = normalize(target, min, max);
    const index = Math.ceil(n * slots);
    targetPoints = selectPoints(buckets, index, maxFreq);
  }
  // Pay attention to both sides of the normal distribution.
  let p = Math.min(pMin / flattened.length, pMax / flattened.length);
  return [singlePoints(histogram), targetPoints, p];
}

function selectPoints(buckets: number, index: number, target: number) {
  const points: Point[] = [];
  for (let i = 0; i < buckets; i += 1) {
    const y = i == index ? target : 0;
    points.push({
      x: i + 1, // 1 based index
      y: y
    });
  }
  return points;
}

function singlePoints(runs: number[]): Point[] {
  const points: Point[] = [];
  for (let i = 0; i < runs.length; i += 1) {
    points.push({
      x: i + 1, // 1 based index
      y: runs[i]
    });
  }
  return points;
}

function normalize(n: number, min: number, max: number): number {
  if (n < min || n > max) {
    console.warn(`Warning n(${n}) is not in the range of (${min}, ${max})`);
    if (n < min) {
      n = min;
    }
    if (n > max) {
      n = max;
    }
  }
  return (n - min) / ((max - min) + 1e-9);
}

function interpolate(normalized: number, min: number, max: number) {
  const range = max - min;
  const value = normalized * range;
  return value + min;
}

/**
 * Generates a series label.
 */
function labelFor<T>(metric: Metric<T>, source: string): string {
  return `${source} {${metric.class} ${metric.benchmark}} - ${metric.label}`;
}

export function datasetName(metric: Metric<any>): string {
  return `${metric.class}_${metric.benchmark}`;
}

/**
 * Helps build cache keys for ranges to ensure we are
 * comparing equal distributions.
 */
function rangeLabel(metric: Metric<unknown>): string {
  return `${metric.benchmark}>${metric.label}`;
}

/**
 * The standard mapper.
 */
export const STANDARD_MAPPER: Mapper = {
  rangeLabel: rangeLabel,
  standard: standardMapper,
  sampled: sampledMapper,
  sampledRanges: sampledRanges
};
