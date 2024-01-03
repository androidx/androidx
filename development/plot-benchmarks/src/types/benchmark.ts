/**
 * The Macrobenchmark Context.
 */
export type BenchmarkContext = {
  "build": {
    'brand': string,
    'device': string,
    'fingerprint': string,
    'model': string,
    'version': {
      "sdk": number
    }
  },
  "cpuCoreCount": number,
  "cpuLocked": boolean,
  "cpuMaxFreqHz": number,
  "memTotalBytes": number,
  "sustainedPerformanceModeEnabled": boolean
};

interface IMetrics {
  'minimum': number;
  'maximum': number;
  'median': number;
};
export interface Standard extends IMetrics {
  'runs': Array<number>;
};
export interface Sampled extends IMetrics {
  'runs': Array<number[]>;
};

export type Metrics = Standard | Sampled;
export type MetricsCollection<T extends Metrics = Metrics> = { readonly [key: string]: T; }

/**
 * The Benchmark.
 */
export type Benchmark = {
  'name': string;
  'params': object;
  'className': string;
  'totalRunTimeNs': number;
  'sampledMetrics'?: MetricsCollection<Sampled>;
  'metrics'?: MetricsCollection<Standard>;
  'warmupIterations': number;
  'repeatIterations': number;
  'thermalThrottleSleepSeconds': number;
};

/**
 * The Benchmarks result.
 */
export type Benchmarks = {
  'context': BenchmarkContext
  'benchmarks': Array<Benchmark>
};
