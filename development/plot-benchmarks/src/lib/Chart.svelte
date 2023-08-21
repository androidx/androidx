<script lang="ts">
  import type { ChartType, LegendItem } from "chart.js";
  import { Chart } from "chart.js/auto";
  import { createEventDispatcher, onMount } from "svelte";
  import { writable, type Writable } from "svelte/store";
  import type { Data } from "../types/chart.js";
  import { LegendPlugin } from "../plugins.js";
  import Legend from "./Legend.svelte";
  import { saveToClipboard as save } from "../clipboard.js";
  import type { Controls, ControlsEvent } from "../types/events.js";

  export let data: Data;
  export let chartType: ChartType = "line";
  export let isExperimental: boolean = false;
  export let showHistogramControls: boolean = false;

  $: {
    if ($chart) {
      $chart.data = data;
      $chart.update();
    }
  }

  // State
  let controlsDispatcher = createEventDispatcher<ControlsEvent>();
  let element: HTMLCanvasElement;
  let buckets: Writable<number> = writable(100);
  let chart: Writable<Chart | null> = writable(null);
  let items: Writable<LegendItem[] | null> = writable(null);

  // Effects
  onMount(() => {
    const onUpdate = (chart: Chart) => {
      $chart = chart;
      // Bad typings.
      const legend = chart.options.plugins.legend as any;
      $items = legend.labels.generateLabels(chart);
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

  function onHistogramChanged(event: Event) {
    const element = event.target as EventTarget & HTMLInputElement;
    const oldValue = $buckets;
    $buckets = parseInt(element.value, 10);
    if (oldValue != $buckets) {
      let controls: Controls = {
        buckets: $buckets,
      };
      controlsDispatcher("controls", controls);
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
  <canvas class="chart" bind:this={element} />
  {#if showHistogramControls}
    <div class="controls">
      <label for="buckets">
        Histogram
        <input
          type="range"
          data-tooltip={$buckets}
          data-placement="right"
          min="10"
          max="250"
          value={$buckets}
          id="buckets"
          name="buckets"
          on:change={onHistogramChanged}
        />
      </label>
    </div>
  {/if}
  {#if isExperimental}
    <footer class="slim">
      <section class="experimental">
        <kbd>Experimental</kbd>
      </section>
    </footer>
  {/if}
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
  .controls {
    margin-top: 20px;
    width: 100%;
  }
  .slim {
    margin-bottom: 0px;
    padding: 0;
  }
  .experimental {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    justify-content: center;
    margin-bottom: 0px;
  }
</style>
