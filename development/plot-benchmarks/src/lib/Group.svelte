<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import type {
    DatasetSelection,
    DatasetSelectionEvent,
    MetricSelection,
    MetricSelectionEvent,
    StatEvent,
    StatInfo,
  } from "../types/events.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Dataset from "./Dataset.svelte";

  export let className: string;
  export let datasetGroup: IndexedWrapper[];
  export let suppressedMetrics: Set<string>;

  let datasetDispatcher = createEventDispatcher<DatasetSelectionEvent>();
  let metricsDispatcher = createEventDispatcher<MetricSelectionEvent>();
  let statDispatcher = createEventDispatcher<StatEvent>();
  let datasetNames: Set<string>;

  // Forward events.
  let datasetSelection = function (event: CustomEvent<DatasetSelection[]>) {
    datasetDispatcher("datasetSelections", event.detail);
  };

  let stat = function (event: CustomEvent<StatInfo[]>) {
    statDispatcher("info", event.detail);
  };

  let metricSelection = function (event: CustomEvent<MetricSelection[]>) {
    metricsDispatcher("metricSelections", event.detail);
  };

  $: {
    datasetNames = Session.datasetNames(datasetGroup);
  }
</script>

<details open>
  <summary>{className}</summary>
  <div class="details">
    {#each datasetNames as name (name)}
      <Dataset
        {datasetGroup}
        {suppressedMetrics}
        {name}
        on:datasetSelections={datasetSelection}
        on:metricSelections={metricSelection}
        on:info={stat}
      />
    {/each}
  </div>
</details>
