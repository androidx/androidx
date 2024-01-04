<script lang="ts">
  import type { Remote } from "comlink";
  import { createEventDispatcher } from "svelte";
  import {
      derived,
      writable,
      type Readable,
      type Writable,
  } from "svelte/store";
  import { readBenchmarks } from "../files.js";
  import {
      ChartDataTransforms,
      type Mapper,
  } from "../transforms/data-transforms.js";
  import { Transforms } from "../transforms/metric-transforms.js";
  import { buildMapper } from "../transforms/standard-mappers.js";
  import type { Data, Series } from "../types/chart.js";
  import type { Metrics } from "../types/data.js";
  import type {
      Controls,
      DatasetSelection,
      FileMetadataEvent,
      MetricSelection,
      StatInfo,
  } from "../types/events.js";
  import type { FileMetadata } from "../types/files.js";
  import type { StatService } from "../workers/service.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Chart from "./Chart.svelte";
  import Group from "./Group.svelte";

  export let fileEntries: FileMetadata[];
  export let service: Remote<StatService>;

  // State
  let eventDispatcher = createEventDispatcher<FileMetadataEvent>();
  let session: Session;
  let mapper: Mapper<number>;
  let metrics: Metrics<number>;
  let series: Series[];
  let chartData: Data;
  let classGroups: Record<string, IndexedWrapper[]>;
  let showControls: boolean;
  let size: number;
  let activeSeries: Promise<Series[]>;

  // Stores
  let buckets: Writable<number> = writable(100);
  let normalizeMetrics: Writable<boolean> = writable(false);
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

  let controlsHandler = function (event: CustomEvent<Controls>) {
    const controls: Controls = event.detail;
    $buckets = controls.buckets;
  };

  $: {
    session = new Session(fileEntries);
    mapper = buildMapper($buckets);
    metrics = Transforms.buildMetrics(session, $suppressed, $suppressedMetrics);
    showControls = metrics.sampled && metrics.sampled.length > 0;
    activeSeries = service.pSeries(metrics, $active);
    series = ChartDataTransforms.mapToSeries(
      metrics,
      mapper,
      $normalizeMetrics
    );
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
    if (items) {
      let newFiles = await Promise.all(
        items
          .filter(
            (item) =>
              item.kind === "file" && item.getAsFile().name.endsWith(".json")
          )
          .map(async (item) => {
            const file = item.getAsFile();
            const benchmarks = await readBenchmarks(file);
            const entry: FileMetadata = {
              enabled: true,
              file: file,
              container: benchmarks,
            };
            return entry;
          })
      );
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
    {#if showControls}
      <div class="toolbar">
        <div class="control">
          <label for="normalize">
            <input
              type="checkbox"
              id="normalize"
              name="normalize"
              data-tooltip="Normalize Metrics"
              on:change={(_) => {
                $normalizeMetrics = !$normalizeMetrics;
              }}
            />
            ≃
          </label>
        </div>
      </div>
    {/if}
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
    <Chart
      data={chartData}
      showHistogramControls={showControls}
      on:controls={controlsHandler}
    />
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
  .toolbar {
    padding: 0;
    margin: 2rem;
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
  }
  .active {
    outline: beige;
    outline-style: dashed;
  }
</style>
