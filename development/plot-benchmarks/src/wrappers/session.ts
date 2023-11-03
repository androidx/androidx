import { type FileMetadata } from "../types/files.js";
import { BenchmarkWrapper } from "./benchmarks.js";

export interface Indexed<T> {
  // The source of the benchmark.
  source: string;
  // `source` index.
  index: number;
  // The underlying type.
  value: T;
}

export type IndexedWrapper = Indexed<BenchmarkWrapper>;

/**
 * A Benchmarking plotting session.
 */
export class Session {
  // className -> BenchmarkWrapper[]
  public classGroups: Record<string, IndexedWrapper[]>;
  // BenchmarkWrapper[]
  public benchmarks: IndexedWrapper[];
  public fileNames: Set<string>;

  constructor(public files: FileMetadata[]) {
    this.initialize();
  }

  private initialize() {
    this.classGroups = {};
    this.fileNames = new Set();
    this.benchmarks = [];

    for (let i = 0; i < this.files.length; i += 1) {
      const fileIndex = i;
      const fileMeta = this.files[fileIndex];
      for (let j = 0; j < fileMeta.container.benchmarks.length; j += 1) {
        const wrapper = new BenchmarkWrapper(fileMeta.container.benchmarks[j]);
        const fileGroupKey = wrapper.className();
        const classGroup = this.classGroups[fileGroupKey] || [];
        const item = {
          source: fileMeta.file.name,
          index: fileIndex,
          value: wrapper
        };
        classGroup.push(item);
        // Update
        this.classGroups[fileGroupKey] = classGroup;
        this.fileNames.add(fileMeta.file.name);
        this.benchmarks.push(item);
      }
    }
  }

  static datasetNames(wrappers: IndexedWrapper[]): Set<string> {
    const names = new Set<string>();
    for (let i = 0; i < wrappers.length; i += 1) {
      const wrapper = wrappers[i];
      names.add(wrapper.value.datasetName());
    }
    return names;
  }

  static sources(wrappers: IndexedWrapper[]): Set<string> {
    const sources = new Set<string>();
    for (let i = 0; i < wrappers.length; i += 1) {
      sources.add(wrappers[i].source);
    }
    return sources;
  }
}
