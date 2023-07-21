<script lang="ts">
  import type { Writable } from "svelte/store";
  import { writable } from "svelte/store";
  import type { FileMetadata } from "../types/files.js";
  import Session from "./Session.svelte";

  // Stores
  let entries: Writable<FileMetadata[]> = writable([]);

  function onFilesChanged(event) {
    const detail: FileMetadata[] = event.detail;
    if (detail) {
      const enabled = detail.filter((metadata) => metadata.enabled === true);
      $entries = [...enabled];
    }
  }
</script>

<details class="heading">
  <summary>Plot Benchmarks</summary>
  <p>Just drag and drop the output JSON file to visualize Benchmark results.</p>
</details>

<div class="container">
  <Session fileEntries={$entries} on:entries={onFilesChanged} />
</div>
