<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { writable, type Writable } from "svelte/store";
  import { readBenchmarks } from "../files.js";
  import { ChartDataTransforms } from "../transforms/data-transforms.js";
  import { Transforms } from "../transforms/metric-transforms.js";
  import { STANDARD_MAPPER } from "../transforms/standard-mappers.js";
  import type { Data, Series } from "../types/chart.js";
  import type { Metrics } from "../types/data.js";
  import type { FileMetadataEvent, Selection } from "../types/events.js";
  import type { FileMetadata } from "../types/files.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Chart from "./Chart.svelte";
  import Group from "./Group.svelte";

  export let fileEntries: FileMetadata[];

  // State
  let eventDispatcher = createEventDispatcher<FileMetadataEvent>();
  let session: Session;
  let metrics: Metrics<number>;
  let series: Series[];
  let chartData: Data;
  let classGroups: Record<string, IndexedWrapper[]>;
  let size: number;

  // Stores
  let activeDragDrop: Writable<boolean> = writable(false);
  let suppressed: Writable<Set<string>> = writable(new Set());

  // Events
  let handler = function (event: CustomEvent<Selection[]>) {
    const selections: Selection[] = event.detail;
    for (let i = 0; i < selections.length; i += 1) {
      const selection = selections[i];
      if (!selection.enabled) {
        $suppressed.add(selection.name);
      } else {
        $suppressed.delete(selection.name);
      }
    }
    $suppressed = $suppressed;
  };

  $: {
    session = new Session(fileEntries);
    metrics = Transforms.buildMetrics(session, $suppressed);
    series = ChartDataTransforms.mapToSeries(metrics, STANDARD_MAPPER);
    chartData = ChartDataTransforms.mapToDataset(series);
    classGroups = session.classGroups;
    size = session.fileNames.size;
  }

  // Helpers

  function onDropFile(event: DragEvent) {
    handleFileDragDrop(event); // async
    $activeDragDrop = false;
    event.preventDefault();
  }

  function onDragOver(event: DragEvent) {
    $activeDragDrop = true;
    event.preventDefault();
  }

  function onDragLeave(event: DragEvent) {
    $activeDragDrop = false;
    event.preventDefault();
  }

  async function handleFileDragDrop(event: DragEvent) {
    const items = [...event.dataTransfer.items];
    const newFiles: FileMetadata[] = [];
    if (items) {
      for (let i = 0; i < items.length; i += 1) {
        if (items[i].kind === "file") {
          const file = items[i].getAsFile();
          if (file.name.endsWith(".json")) {
            const benchmarks = await readBenchmarks(file);
            const entry: FileMetadata = {
              enabled: true,
              file: file,
              container: benchmarks,
            };
            newFiles.push(entry);
          }
        }
      }
      // Deep copy & notify
      eventDispatcher("entries", [...fileEntries, ...newFiles]);
    }
  }
</script>

{#if size <= 0}
  <article
    id="drop"
    class="drop"
    class:active={$activeDragDrop}
    on:drop={onDropFile}
    on:dragover={onDragOver}
    on:dragleave={onDragLeave}
  >
    <h5>Drag and drop benchmark results to get started.</h5>
  </article>
{:else}
  <article
    id="drop"
    class="drop"
    class:active={$activeDragDrop}
    on:drop={onDropFile}
    on:dragover={onDragOver}
    on:dragleave={onDragLeave}
  >
    <h5>Benchmarks</h5>
    {#each Object.entries(classGroups) as [className, wrappers]}
      <Group {className} datasetGroup={wrappers} on:selections={handler} />
    {/each}
  </article>

  {#if series.length > 0}
    <Chart data={chartData} />
  {/if}
{/if}

<style>
  .active {
    outline: beige;
    outline-style: dashed;
  }
</style>
