import type { Benchmark, Benchmarks, Metrics } from "./schema.js";

/**
 * A container for the chart data type.
 */
export interface Data {
  name: string;
  data: Array<number>;
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
            datasets.push({
              name: name,
              // Might want to make this compatible for scatter plots
              data: metrics.runs
            });
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
  if (benchmark.metrics) {
    for (const key in benchmark.metrics) {
      if (benchmark.metrics.hasOwnProperty(key) && key == label) {
        return benchmark.metrics[key];
      }
    }
  }
  return undefined;
}

function metricLabels(benchmark: Benchmark): Array<string> {
  const labels = [];
  if (benchmark.metrics) {
    for (const key in benchmark.metrics) {
      if (benchmark.metrics.hasOwnProperty(key)) {
        labels.push(key);
      }
    }
  }
  return labels;
}
