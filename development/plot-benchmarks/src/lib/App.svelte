<script lang="ts">
  import type { Writable } from "svelte/store";
  import { derived, writable } from "svelte/store";
  import type { FileMetadata } from "../files.js";
  import type { Benchmarks } from "../schema.js";
  import Chart from "./Chart.svelte";
  import Files from "./Files.svelte";

  let entries: Writable<Array<FileMetadata>> = writable([]);
  let containers = derived(entries, ($entries) => {
    const containers: Array<Benchmarks> = [];
    for (let i = 0; i < $entries.length; i += 1) {
      let entry = $entries[i];
      containers.push(entry.benchmarks);
    }
    return containers;
  });

  function onFileMetadataEvent(event) {
    const detail: Array<FileMetadata> = event.detail;
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
  <Files on:entries={onFileMetadataEvent} />
  {#if $containers.length > 0}
    <Chart {containers} />
  {/if}
</div>
