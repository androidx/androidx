<script lang="ts">
  import type { ChartType, LegendItem } from "chart.js";
  import { Chart } from "chart.js/auto";
  import { onMount } from "svelte";
  import { writable, type Writable } from "svelte/store";
  import type { Data } from "../types/chart.js";
  import { LegendPlugin } from "../plugins.js";
  import Legend from "./Legend.svelte";
  import { saveToClipboard as save } from "../clipboard.js";

  export let data: Data;
  export let chartType: ChartType = "line";

  $: {
    if ($chart) {
      $chart.data = data;
      $chart.update();
    }
  }

  // State
  let element: HTMLCanvasElement;
  let chart: Writable<Chart | null> = writable(null);
  let items: Writable<LegendItem[] | null> = writable(null);

  // Effects
  onMount(() => {
    const onUpdate = (chart: Chart) => {
      $chart = chart;
      $items = chart.options.plugins.legend.labels.generateLabels(chart);
    };
    const plugins = {
      legend: {
        display: false,
      },
      benchmark: {
        onUpdate: onUpdate,
      },
    };
    $chart = new Chart(element, {
      data: data,
      type: chartType,
      plugins: [LegendPlugin],
      options: {
        plugins: plugins,
      },
    });
  });

  // Copy to clip board
  async function copy(event: Event) {
    if ($chart) {
      await save($chart);
    }
  }
</script>

<article>
  <div class="toolbar">
    <button
      class="btn outline"
      data-tooltip="Copy chart to clipboard."
      on:click={copy}
    >
      ⎘
    </button>
  </div>
  <canvas id="chart" class="chart" bind:this={element} />
</article>

{#if $items}
  <Legend chart={$chart} items={$items} />
{/if}

<style>
  .chart {
    width: 100%;
  }
  .toolbar {
    padding: 0;
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
  }

  .toolbar .btn {
    width: auto;
    height: auto;
    border: none;
    padding: 5px;
  }
</style>
