<script lang="ts">
  import type { Chart, LegendItem } from "chart.js";
  export let chart: Chart;
  export let items: LegendItem[];

  const handlerFactory = (item: LegendItem) => {
    return (event: Event) => {
      // https://www.chartjs.org/docs/latest/samples/legend/html.html
      chart.setDatasetVisibility(
        item.datasetIndex,
        !chart.isDatasetVisible(item.datasetIndex)
      );
      chart.update();
    };
  };
</script>

<div class="legend">
  <h6>Legend</h6>
  {#each items as item (item)}
    <div
      class="item"
      on:dblclick={handlerFactory(item)}
      aria-label="legend"
      role="listitem"
    >
      <span
        class="box"
        style="background: {item.fillStyle}; border-color: {item.strokeStyle}; border-width: {item.lineWidth}px;"
      />
      <span
        class="label"
        style="text-decoration: {item.hidden ? 'line-through' : ''};"
      >
        {item.text}
      </span>
    </div>
  {/each}
</div>

<style>
  .legend {
    padding: 4px;
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
