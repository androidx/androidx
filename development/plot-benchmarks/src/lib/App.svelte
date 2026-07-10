<script lang="ts">
  import type { Writable } from "svelte/store";
  import { writable } from "svelte/store";
  import type { FileMetadata } from "../types/files.js";
  import Session from "./Session.svelte";
  import { wrap } from "comlink";
  import { StatService } from "../workers/service.js";
  // Inline the web worker for ease of serving
  import Worker from '../workers/worker.ts?worker&inline';

  // Stores
  let entries: Writable<FileMetadata[]> = writable([]);
  let worker = new Worker();
  const service = wrap<StatService>(worker);

  function onFilesChanged(event: CustomEvent) {
    const detail: FileMetadata[] = event.detail;
    if (detail) {
      const enabled = detail.filter((metadata) => metadata.enabled === true);
      $entries = [...enabled];
    }
  }
</script>

<details class="heading">
  <summary>
    <h4>Plot Benchmarks</h4>
  </summary>
  <p>Just drag and drop the output JSON file to visualize Benchmark results.</p>
</details>

<div class="container">
  <Session fileEntries={$entries} {service} on:entries={onFilesChanged} />
</div>
