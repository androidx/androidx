import type { Benchmark, MetricsCollection, Sampled, Standard } from "../types/benchmark.js";

export class BenchmarkWrapper {
  constructor(
    private benchmark: Benchmark,
    private separator: string = '_'
  ) {

  }

  datasetName(): string {
    return `${this.className()}${this.separator}${this.benchmark.name}`;
  }

  metric(label: string): Standard | undefined {
    return this.benchmark?.metrics[label];
  }

  sampled(label: string): Sampled | undefined {
    return this.benchmark?.sampledMetrics[label];
  }

  metricLabels(): string[] {
    return BenchmarkWrapper.labels(this.benchmark.metrics);
  }

  sampledLabels(): string[] {
    return BenchmarkWrapper.labels(this.benchmark.sampledMetrics);
  }

  className(): string {
    const className = this.benchmark.className;
    const parts = className.split('.');
    const lastIndex = parts.length - 1;
    return parts[lastIndex];
  }

  testName(): string {
    return this.benchmark.name;
  }

  private static labels(collection: MetricsCollection | null): string[] {
    const labels: string[] = [];
    if (collection) {
      for (const key in collection) {
        if (collection.hasOwnProperty(key)) {
          labels.push(key);
        }
      }
    }
    return labels;
  }

}
