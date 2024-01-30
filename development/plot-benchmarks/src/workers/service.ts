import { datasetName, histogramPoints } from "../transforms/standard-mappers.js";
import type { Series } from "../types/chart.js";
import type { ChartData, Metrics } from "../types/data.js";

export class StatService {
  pSeries(metrics: Metrics<number>, activeDatasets: Set<string>): Series[] {
    if (activeDatasets.size <= 0) {
      return [];
    }

    const series: Series[] = [];
    const standard = metrics.standard;
    const sampled = metrics.sampled;
    if (sampled) {
      for (let i = 0; i < sampled.length; i += 1) {
        const metric = sampled[i];
        const name = datasetName(metric);
        if (activeDatasets.has(name)) {
          const data: Record<string, ChartData<number[]>> = metric.data;
          const entries = Object.entries(data);
          const comparables: ChartData<number[]>[] = entries.map(entry => entry[1]);
          if (comparables.length > 1) {
            const reference = comparables[0];
            for (let j = 1; j < comparables.length; j += 1) {
              const target = comparables[j];
              if (!(reference && target)) {
                continue;
              }
              const [delta, distribution] = this.buildDistribution(reference, target);
              const [points, pPlots, p] = histogramPoints([distribution], /* buckets */ 100, /* target */ delta);
              series.push({
                descriptiveLabel: `${name} { ${metric.label} } - Likelihood`,
                type: "line",
                data: points,
                options: {
                  tension: 0.3
                }
              });
              if (pPlots && pPlots.length > 0) {
                series.push({
                  descriptiveLabel: `${name} { ${metric.label} } - { P = ${p} }`,
                  type: "bar",
                  data: pPlots,
                  options: {
                    tension: 0.01
                  }
                });
              }
            }
          }
        }
      }
    }
    if (standard) {
      for (let i = 0; i < standard.length; i += 1) {
        const metric = standard[i];
        const name = datasetName(metric);
        if (activeDatasets.has(name)) {
          const data: Record<string, ChartData<number>> = metric.data;
          const entries = Object.entries(data);
          const comparables: ChartData<number>[] = entries.map(entry => entry[1]);
          if (comparables.length > 1) {
            const reference = comparables[0];
            for (let j = 1; j < comparables.length; j += 1) {
              const target = comparables[j];
              if (!(reference && target)) {
                continue;
              }
              const [delta, distribution] = this.buildStandardDistribution(reference, target);
              const [points, pPlots, p] = histogramPoints([distribution], /* buckets */ 100, /* target */ delta);
              series.push({
                descriptiveLabel: `${name} { ${metric.label} } - Likelihood`,
                type: "line",
                data: points,
                options: {
                  tension: 0.3
                }
              });
              if (pPlots && pPlots.length > 0) {
                series.push({
                  descriptiveLabel: `${name} { ${metric.label} } - { P = ${p} }`,
                  type: "bar",
                  data: pPlots,
                  options: {
                    tension: 0.01
                  }
                });
              }
            }
          }
        }
      }
    }
    return series;
  }

  private buildStandardDistribution(
    reference: ChartData<number>,
    target: ChartData<number>,
    N: number = 100_000): [number, number[]] {
    // Compute delta mean
    const referenceData = reference.values;
    const targetData = target.values;
    const referenceMean = this.mean(referenceData);
    const targetMean = this.mean(targetData);
    const deltaMean = referenceMean - targetMean;
    // Simulate
    const rs = referenceData.length;
    const ts = targetData.length;
    const combined: number[] = [...referenceData, ...targetData];
    const means: number[] = [];
    for (let i = 0; i < N; i += 1) {
      const [r, t] = this.shuffleSplit(combined, [rs, ts]);
      const mr = this.mean(r);
      const mt = this.mean(t);
      means.push(mr - mt);
    }
    return [deltaMean, means];
  }

  private buildDistribution(
    reference: ChartData<number[]>,
    target: ChartData<number[]>,
    N: number = 1_000
  ): [number, number[]] {
    // Compute delta mean
    const referenceData = reference.values;
    const targetData = target.values;
    const referenceMedian = this.arrayMedian(referenceData);
    const targetMedian = this.arrayMedian(targetData);
    const deltaMedian = referenceMedian - targetMedian;
    // Simulate
    const rs = referenceData.length;
    const ts = targetData.length;
    const combined: number[][] = [...referenceData, ...targetData];
    const medians: number[] = [];
    for (let i = 0; i < N; i += 1) {
      const [r, t] = this.shuffleSplit(combined, [rs, ts]);
      const mr = this.arrayMedian(r);
      const mt = this.arrayMedian(t);
      medians.push(mr - mt);
    }
    return [deltaMedian, medians];
  }

  private shuffleSplit<T>(data: T[], sizes: number[]): T[][] {
    const shuffled = this.shuffle(data);
    const splits: T[][] = [];
    let index = 0;
    for (let i = 0; i < sizes.length; i += 1) {
      const size = sizes[i];
      let split: T[] = [];
      for (let j = 0; j < size; j += 1) {
        const k = index + j;
        if (k < shuffled.length) {
          split.push(shuffled[k]);
        }
      }
      index += size;
      splits.push(split);
    }
    return splits;
  }

  private arrayMedian(data: number[][]): number {
    // We don't want to compute median of medians here.
    // This is because while individual runs are correlated
    // we can still look at the actual metrics in aggregate.
    return this.median(data.flat());
  }

  private mean(data: number[]): number {
    if (data.length <= 0) return 0;
    let sum = 0;
    for (let i = 0; i < data.length; i += 1) {
      sum += data[i];
    }
    return (sum / data.length);
  }

  private median(data: number[]): number {
    const copy = [...data];
    // Default comparator coerces types to string !
    copy.sort((a, b) => a - b); // in-place
    const length = copy.length;
    const index = Math.trunc(length / 2);
    return copy[index];
  }

  private shuffle<T>(data: T[], multiplier: number = 1): T[] {
    if (data.length <= 0) {
      return [];
    }

    let copy = [...data];
    const count = copy.length * multiplier;
    const slots = copy.length - 1;
    for (let i = 0; i < count; i += 1) {
      const sourceIndex = Math.ceil(Math.random() * slots);
      const targetIndex = Math.ceil(Math.random() * slots);
      let source = copy[sourceIndex];
      let target = copy[targetIndex];
      copy[sourceIndex] = target;
      copy[targetIndex] = source;
    }
    return copy;
  }

}
