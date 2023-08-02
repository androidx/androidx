import type { Benchmarks } from "./types/benchmark.js";

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
