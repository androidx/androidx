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
    const points = histogramPoints(chartData.values);
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

function histogramPoints(runs: number[][], buckets: number = 10): Point[] {
  const flattened = runs.flat();
  // Default comparator coerces types to string !
  flattened.sort((a, b) => a - b); // in-place
  const min = flattened[0];
  const max = flattened[flattened.length - 1];
  const histogram = new Array(buckets).fill(0);
  const slots = buckets - 1; // The actual number of slots in the histogram
  for (let i = 0; i < flattened.length; i += 1) {
    let n = normalize(flattened[i], min, max);
    let index = Math.ceil(n * slots);
    histogram[index] = histogram[index] + 1;
  }
  return singlePoints(histogram);
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
  return (n - min) / (max - min + 1e-5);
}

/**
 * Generates a series label.
 */
function labelFor<T>(metric: Metric<T>, source: string): string {
  return `${source}[${metric.class} ${metric.benchmark} ${metric.label}]`;
}

/**
 * The standard mapper.
 */
export const STANDARD_MAPPER: Mapper = {
  standard: standardMapper,
  sampled: sampledMapper
};
