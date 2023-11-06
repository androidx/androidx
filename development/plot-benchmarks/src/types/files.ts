import type { Benchmarks } from "./benchmark.js";

/**
 * File information + associated benchmarks.
 */
export interface FileMetadata {
  file: File;
  enabled: boolean;
  container: Benchmarks;
}
