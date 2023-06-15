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

/**
 * The Metrics Payload.
 */
export type Metrics = {
  'minimum': number;
  'maximum': number;
  'median': number;
  'runs': Array<number>;
};

/**
 * The Benchmark.
 */
export type Benchmark = {
  'name': string;
  'params': object;
  'className': string;
  'totalRunTimeNs': number;
  'metrics': {
    readonly [key: string]: Metrics;
  };
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
