import type { Point } from "chart.js";
import type { Series } from "../types/chart.js";
import type { ChartData, Metric } from "../types/data.js";
import type { Mapper } from "./data-transforms.js";

function sampledMapper(metric: Metric<number[]>): Series[] {
  const series: Series[] = [];
  const data: Record<string, ChartData<number[]>> = metric.data;
  const entries = Object.entries(data);
  for (let i = 0; i < entries.length; i += 1) {
    const [source, chartData] = entries[i];
    const label = labelFor(metric, source);
    const [points, _, __] = histogramPoints(chartData.values);
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
  buckets: number = 10,
  target: number | null = null
): [Point[], Point[] | null, number | null] {
  const flattened = runs.flat();
  // Default comparator coerces types to string !
  flattened.sort((a, b) => a - b); // in-place
  const min = flattened[0];
  const max = flattened[flattened.length - 1];
  let targetPoints: Point[] | null = null;
  let pN: number = 0;
  let maxFreq: number = 0;
  const histogram = new Array(buckets).fill(0);
  const slots = buckets - 1; // The actual number of slots in the histogram
  for (let i = 0; i < flattened.length; i += 1) {
    const value = flattened[i];
    if (value >= target) {
      pN += 1;
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
  return [singlePoints(histogram), targetPoints, (pN / flattened.length)];
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
  return (n - min) / (max - min + 1e-5);
}

/**
 * Generates a series label.
 */
function labelFor<T>(metric: Metric<T>, source: string): string {
  return `${source} {${metric.class}${metric.benchmark}} - ${metric.label}`;
}

export function datasetName(metric: Metric<any>): string {
  return `${metric.class}_${metric.benchmark}`;
}

/**
 * The standard mapper.
 */
export const STANDARD_MAPPER: Mapper = {
  standard: standardMapper,
  sampled: sampledMapper
};
