import type { ChartData, Metric, Metrics } from "../types/data.js";
import type { Session } from "../wrappers/session.js";

/**
 * Helps with transforming benchmark data into something that can be visualized easily.
 */
export class Transforms {
  private constructor() {
    // static helpers.
  }

  static buildMetrics(session: Session, suppressed: Set<string>, suppressedMetrics: Set<string>): Metrics<number> {
    const classGroups = Object.entries(session.classGroups);
    const standard: Metric<number>[] = [];
    const sampled: Metric<number[]>[] = [];
    for (let i = 0; i < classGroups.length; i += 1) {
      const [className, wrappers] = classGroups[i];
      for (let j = 0; j < wrappers.length; j += 1) {
        const wrapper = wrappers[j];
        const datasetName = wrappers[j].value.datasetName();
        if (suppressed.has(datasetName)) {
          continue;
        }
        const source = wrapper.source;
        const testName = wrapper.value.testName();
        // standard
        let labels = wrapper.value.metricLabels();
        for (let k = 0; k < labels.length; k += 1) {
          const label = labels[k];
          if (suppressedMetrics.has(label)) {
            continue;
          }
          const metric = wrapper.value.metric(label);
          const charData: ChartData<number> = {
            values: metric.runs
          };
          Transforms.add<number>(
            standard,
            className,
            testName,
            label,
            source,
            charData
          );
        }
        // sampled
        labels = wrapper.value.sampledLabels();
        for (let k = 0; k < labels.length; k += 1) {
          const label = labels[k];
          if (suppressedMetrics.has(label)) {
            continue;
          }
          const metric = wrapper.value.sampled(label);
          const charData: ChartData<number[]> = {
            values: metric.runs
          };
          Transforms.add<number[]>(
            sampled,
            className,
            testName,
            label,
            source,
            charData
          );
        }
      }
    }
    const metrics: Metrics<number> = {
      standard: standard,
      sampled: sampled
    };
    return metrics;
  }

  private static add<T>(
    metrics: Metric<T>[],
    className: string,
    testName: string,
    label: string,
    source: string,
    data: ChartData<T>
  ) {
    const metric = Transforms.getOrCreate<T>(metrics, className, testName, label);
    metric.data[source] = data;
  }

  private static getOrCreate<T>(
    metrics: Metric<T>[],
    className: string,
    testName: string,
    label: string
  ): Metric<T> {
    let metric: Metric<T> | null = Transforms.find(metrics, className, testName, label);
    if (metric == null) {
      const data: Record<string, ChartData<T>> = {};
      metric = {
        class: className,
        benchmark: testName,
        label: label,
        data: data
      }
      metrics.push(metric);
    }
    return metric;
  }

  private static find<T>(
    metrics: Metric<T>[],
    className: string,
    testName: string,
    label: string
  ): Metric<T> | null {
    for (let i = 0; i < metrics.length; i += 1) {
      const metric = metrics[i];
      if (
        metric.class === className &&
        metric.benchmark === testName &&
        metric.label === label
      ) {
        return metric;
      }
    }
    return null;
  }
}
