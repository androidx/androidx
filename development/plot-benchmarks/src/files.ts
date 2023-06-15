import type { Benchmarks } from "./schema.js";

export interface FileMetadata {
  file: File;
  enabled: boolean;
  benchmarks: Benchmarks;
}

export interface FileMetadataEvent {
  entries: Array<FileMetadata>;
}

export async function readBenchmarks(file: File): Promise<Benchmarks> {
  const contents = await readFile(file);
  return JSON.parse(contents);
}

async function readFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (reader.error) {
        reject(reader.error);
      } else {
        resolve(reader.result as string);
      }
    }
    reader.readAsText(file, 'UTF-8');
  });
}
