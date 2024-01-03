<script lang="ts">
  import type { LegendItem } from "chart.js";
  import { Chart } from "chart.js/auto";
  import { onMount } from "svelte";
  import type { Readable, Writable } from "svelte/store";
  import { derived, writable } from "svelte/store";
  import { chartData } from "../chart-transforms.js";
  import { saveToClipboard } from "../clipboard.js";
  import { LegendPlugin } from "../plugins.js";
  import { expressionFilter } from "../regexp.js";
  import type { Benchmarks } from "../schema.js";
  import { benchmarksDataset } from "../transforms.js";
  type FilterFn = (label: string) => boolean;

  export let containers: Readable<Array<Benchmarks>>;

  let canvas: HTMLCanvasElement;
  let filter: Writable<FilterFn> = writable((_: string) => true);

  let data = derived([containers, filter], ([$containers, $filter]) => {
    return chartData(benchmarksDataset($containers, $filter));
  });

  let chart: Writable<Chart | undefined> = writable(null);
  let legendLabels: Writable<Array<LegendItem> | undefined> = writable(null);

  data.subscribe(($data) => {
    if ($chart) {
      $chart.data = $data;
      $chart.update();
    }
  });

  onMount(() => {
    const onUpdate = (updated: Chart) => {
      chart.set(updated);
      legendLabels.set(
        updated.options.plugins.legend.labels.generateLabels(updated)
      );
    };
    const plugins = {
      legend: {
        display: false,
      },
      benchmark: {
        onUpdate: onUpdate,
      },
    };
    chart.set(
      new Chart(canvas, {
        type: "line",
        data: $data,
        plugins: [LegendPlugin],
        options: {
          plugins: plugins,
        },
      })
    );
  });

  function onItemClick(item: LegendItem) {
    return (_: Event) => {
      // https://www.chartjs.org/docs/latest/samples/legend/html.html
      $chart.setDatasetVisibility(
        item.datasetIndex,
        !$chart.isDatasetVisible(item.datasetIndex)
      );
      // Update chart
      $chart.update();
    };
  }

  function onFilterChanged(event: Event) {
    const target = event.currentTarget as HTMLInputElement;
    const value = target.value;
    $filter = expressionFilter(value);
  }

  async function onCopy(_: Event) {
    if ($chart) {
      await saveToClipboard($chart);
    }
  }
</script>

<article>
  <button
    class="copy outline"
    data-tooltip="Copy chart to clipboard."
    on:click={onCopy}
  >
    ⎘
  </button>
  <canvas id="chart" class="chart" bind:this={canvas} />
</article>

{#if $legendLabels && $legendLabels.length >= 0}
  <article>
    <div class="filter">
      <label for="metricFilter">
        <input
          type="text"
          id="metricFilter"
          name="metricFilter"
          placeholder="Filter metrics (regular expressions)"
          autocomplete="off"
          on:input={onFilterChanged}
        />
      </label>
    </div>
    <div class="legend">
      {#each $legendLabels as label, index}
        <div
          class="item"
          on:dblclick={onItemClick(label)}
          aria-label="legend"
          role="listitem"
        >
          <span
            class="box"
            style="background: {label.fillStyle}; border-color: {label.strokeStyle}; border-width: {label.lineWidth}px;"
          />
          <span
            class="label"
            style="text-decoration: {label.hidden ? 'line-through' : ''};"
          >
            {label.text}
          </span>
        </div>
      {/each}
    </div>
  </article>
{/if}

<style>
  .copy {
    width: 4%;
    position: relative;
    padding: 0;
    border: none;
    /* Looked okay on my machine. */
    right: -52rem;
    top: -3rem;
  }
  .chart {
    width: 100%;
  }
  .legend {
    display: flex;
    flex-direction: column;
    row-gap: 3px;
  }
  .item {
    display: flex;
    flex-direction: row;
    column-gap: 10px;
    align-items: center;
  }
  .item .box {
    display: inline-block;
    width: 20px;
    height: 20px;
  }
</style>
