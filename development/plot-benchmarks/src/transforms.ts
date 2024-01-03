import type { Point } from "chart.js";
import type { Benchmark, Benchmarks, Metrics, MetricsCollection } from "./schema.js";

/**
 * A container for the chart data type.
 */
export interface Data {
  name: string;
  data: Array<Point>;
}

export function benchmarksDataset(containers: Array<Benchmarks>, filterFn: (label: string) => boolean): Array<Data> {
  const datasets: Array<Data> = [];
  for (let i = 0; i < containers.length; i += 1) {
    let container = containers[i];
    for (let j = 0; j < container.benchmarks.length; j += 1) {
      const benchmark = container.benchmarks[j];
      // 1 based index
      const prefix = datasetName(i + 1, benchmark);
      const labels = metricLabels(benchmark);
      for (let k = 0; k < labels.length; k += 1) {
        const name = `${prefix}_${labels[k]}`;
        if (filterFn(name)) {
          const metrics = metricsData(benchmark, labels[k]);
          if (metrics) {
            const sampled = metrics.runs && Array.isArray(metrics.runs[0]);
            if (sampled) {
              // This is always the case for single metrics
              const runs = metrics.runs as number[][];
              datasets.push({
                name: name,
                // Compute histograms
                data: histogramPoints(runs)
              });
            } else {

              const runs = metrics.runs as number[];
              datasets.push({
                name: name,
                // Might want to make this compatible for scatter plots
                data: singlePoints(runs)
              });
            }
          }
        }
      }
    }
  }
  return datasets;
}

function datasetName(index: number, benchmark: Benchmark): string {
  const className = benchmark.className;
  const parts = className.split('.');
  const lastIndex = parts.length - 1;
  return `${index}_${parts[lastIndex]}_${benchmark.name}`;
}

function metricsData(benchmark: Benchmark, label: string): Metrics | undefined {
  let data = metricsDataFor(benchmark.metrics, label);
  if (data) {
    return data;
  }
  return metricsDataFor(benchmark.sampledMetrics, label);
}

function metricsDataFor(collection: MetricsCollection, label: string): Metrics | undefined {
  for (const key in collection) {
    if (collection.hasOwnProperty(key) && key == label) {
      return collection[key];
    }
  }
  return undefined;
}

function metricLabels(benchmark: Benchmark): Array<string> {
  const labels = labelsFor(benchmark.metrics);
  const sampled = labelsFor(benchmark.sampledMetrics);
  return [...labels, ...sampled];
}

function labelsFor(collection: MetricsCollection): Array<string> {
  const labels = [];
  if (collection) {
    for (const key in collection) {
      if (collection.hasOwnProperty(key)) {
        labels.push(key);
      }
    }
  }
  return labels;
}

function histogramPoints(runs: Array<number[]>, buckets: number = 10): Array<Point> {
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

function normalize(n: number, min: number, max: number): number {
  return (n - min) / (max - min + 1e-5);
}

function singlePoints(runs: Array<number>): Array<Point> {
  const points: Array<Point> = [];
  for (let i = 0; i < runs.length; i += 1) {
    points.push({
      x: i + 1, // 1 based index
      y: runs[i]
    });
  }
  return points;
}
