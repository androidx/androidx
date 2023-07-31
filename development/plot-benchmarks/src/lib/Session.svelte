<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import {
    writable,
    type Readable,
    type Writable,
    derived,
  } from "svelte/store";
  import { readBenchmarks } from "../files.js";
  import { ChartDataTransforms } from "../transforms/data-transforms.js";
  import { Transforms } from "../transforms/metric-transforms.js";
  import { STANDARD_MAPPER } from "../transforms/standard-mappers.js";
  import type { Data, Series } from "../types/chart.js";
  import type { Metrics } from "../types/data.js";
  import type {
    FileMetadataEvent,
    DatasetSelection,
    StatInfo,
    MetricSelection,
  } from "../types/events.js";
  import type { FileMetadata } from "../types/files.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Chart from "./Chart.svelte";
  import Group from "./Group.svelte";
  import type { StatService } from "../workers/service.js";
  import type { Remote } from "comlink";

  export let fileEntries: FileMetadata[];
  export let service: Remote<StatService>;

  // State
  let eventDispatcher = createEventDispatcher<FileMetadataEvent>();
  let session: Session;
  let metrics: Metrics<number>;
  let series: Series[];
  let chartData: Data;
  let classGroups: Record<string, IndexedWrapper[]>;
  let size: number;
  let activeSeries: Promise<Series[]>;

  // Stores
  let activeDragDrop: Writable<boolean> = writable(false);
  let suppressed: Writable<Set<string>> = writable(new Set());
  let suppressedMetrics: Writable<Set<string>> = writable(new Set());
  let activeStats: Writable<StatInfo[]> = writable([]);
  let active: Readable<Set<string>> = derived(activeStats, ($activeStats) => {
    const datasets = [];
    for (let i = 0; i < $activeStats.length; i += 1) {
      const activeStat = $activeStats[i];
      datasets.push(activeStat.name);
    }
    return new Set(datasets);
  });

  // Events
  let datasetHandler = function (event: CustomEvent<DatasetSelection[]>) {
    const selections: DatasetSelection[] = event.detail;
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

  let metricsHandler = function (event: CustomEvent<MetricSelection[]>) {
    const selections: MetricSelection[] = event.detail;
    for (let i = 0; i < selections.length; i += 1) {
      const selection = selections[i];
      if (!selection.enabled) {
        $suppressedMetrics.add(selection.name);
      } else {
        $suppressedMetrics.delete(selection.name);
      }
    }
    $suppressedMetrics = $suppressedMetrics;
  };

  let statHandler = function (event: CustomEvent<StatInfo[]>) {
    const statistics = event.detail;
    for (let i = 0; i < statistics.length; i += 1) {
      const statInfo = statistics[i];
      if (!statInfo.enabled) {
        const index = $activeStats.findIndex(
          (entry) => entry.name == statInfo.name && entry.type == statInfo.type
        );
        if (index >= 0) {
          $activeStats.splice(index, 1);
        }
      } else {
        $activeStats.push(statInfo);
      }
      $activeStats = $activeStats;
    }
  };

  $: {
    session = new Session(fileEntries);
    metrics = Transforms.buildMetrics(session, $suppressed, $suppressedMetrics);
    activeSeries = service.pSeries(metrics, $active);
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
      <Group
        {className}
        datasetGroup={wrappers}
        suppressedMetrics={$suppressedMetrics}
        on:datasetSelections={datasetHandler}
        on:metricSelections={metricsHandler}
        on:info={statHandler}
      />
    {/each}
  </article>

  {#if series.length > 0}
    <Chart data={chartData} />
  {/if}

  {#await activeSeries}
    <article aria-busy="true" />
  {:then chartData}
    {#if chartData.length > 0}
      <Chart
        data={ChartDataTransforms.mapToDataset(chartData)}
        isExperimental={true}
      />
    {/if}
  {/await}
{/if}

<style>
  .active {
    outline: beige;
    outline-style: dashed;
  }
</style>
