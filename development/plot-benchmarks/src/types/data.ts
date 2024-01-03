/**
 * A container for raw benchmark data.
 *
 * Can be extended to store descriptive statistics about the raw data.
 */
export interface ChartData<T> {
  values: T[];
}

/**
 * A container for a Metric.
 *
 * This metric has all relevant comparables, in the data keyed by the source.
 */
export interface Metric<T> {
  class: string;
  benchmark: string;
  label: string;
  data: Record<string, ChartData<T>>;
}

/**
 * A container for standard and sampled `Metric` instances.
 */
export interface Metrics<T = number> {
  standard?: Metric<T>[];
  sampled?: Metric<T[]>[];
}
