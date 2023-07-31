<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import {
    type DatasetSelection,
    type DatasetSelectionEvent,
    type MetricSelection,
    type MetricSelectionEvent,
    type StatEvent,
    type StatInfo,
    type StatType,
  } from "../types/events.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";

  export let name: string;
  export let datasetGroup: IndexedWrapper[];
  export let suppressedMetrics: Set<string>;

  // Dispatchers
  let datasetDispatcher = createEventDispatcher<DatasetSelectionEvent>();
  let metricsDispatcher = createEventDispatcher<MetricSelectionEvent>();
  let statDispatcher = createEventDispatcher<StatEvent>();
  // State
  let selected: boolean = true;
  let compute: boolean = false;
  let sources: Set<string>;
  let sampledMetrics: Set<string>;
  let metrics: Set<string>;

  // Events
  let selection = function (event: Event) {
    event.stopPropagation();
    const target = event.target as HTMLInputElement;
    selected = target.checked;
    const selection: DatasetSelection = {
      name: name,
      enabled: selected,
    };
    datasetDispatcher("datasetSelections", [selection]);
  };

  let stat = function (type: StatType) {
    return function (event: Event) {
      event.stopPropagation();
      const target = event.target as HTMLInputElement;
      compute = target.checked;
      const stat: StatInfo = {
        name: name,
        type: type,
        enabled: compute,
      };
      statDispatcher("info", [stat]);
    };
  };

  let metricSelection = function (metric: string) {
    return function (event: Event) {
      event.stopPropagation();
      const target = event.target as HTMLInputElement;
      const checked = target.checked;
      const selection: MetricSelection = {
        name: metric,
        enabled: checked,
      };
      metricsDispatcher("metricSelections", [selection]);
    };
  };

  $: {
    sources = Session.sources(datasetGroup);
    let labels = datasetGroup
      .map((indexed) => indexed.value.metricLabels())
      .flat();

    let sampled = datasetGroup
      .map((indexed) => indexed.value.sampledLabels())
      .flat();

    metrics = new Set<string>(labels);
    sampledMetrics = new Set<string>(sampled);
  }
</script>

<div class="dataset">
  <hgroup>
    <div class="section">
      <span class="item">{name}</span>
      <div class="item actions">
        <fieldset>
          <label for="switch">
            Show
            <input
              type="checkbox"
              role="switch"
              checked={selected}
              on:change={selection}
            />
          </label>
        </fieldset>
        {#if sources.size > 1}
          <fieldset>
            <label for="switch">
              P
              <input
                type="checkbox"
                role="switch"
                checked={compute}
                on:change={stat("p")}
              />
            </label>
          </fieldset>
        {/if}
      </div>
    </div>
    <div class="details">
      <div class="sources">
        {#each sources as source (source)}
          <div>📁 <small>{source}</small></div>
        {/each}
      </div>
      {#if metrics.size > 0}
        <div class="metrics">
          {#each metrics as metric (metric)}
            <fieldset class="metric">
              <label for={metric}>
                <input
                  type="checkbox"
                  name={metric}
                  id={metric}
                  checked={!suppressedMetrics.has(metric)}
                  on:change={metricSelection(metric)}
                />
                📏 {metric}
              </label>
            </fieldset>
          {/each}
        </div>
      {/if}
      {#if sampledMetrics.size > 0}
        <div class="sampled">
          {#each sampledMetrics as metric (metric)}
            <fieldset class="metric">
              <label for={metric}>
                <input
                  type="checkbox"
                  name={metric}
                  id={metric}
                  checked={!suppressedMetrics.has(metric)}
                  on:change={metricSelection(metric)}
                />
                📏 {metric}
              </label>
            </fieldset>
          {/each}
        </div>
      {/if}
    </div>
  </hgroup>
</div>

<style>
  .dataset {
    margin: 0.5rem;
    padding-top: 1rem;
    padding-left: 1rem;
    border: 1px solid #b3cee5;
  }
  .details {
    padding-left: 1rem;
  }

  .details > div {
    margin-top: 0.25rem;
  }
  .section {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
  }

  .section .item {
    margin: 0px 10px;
  }
  .actions {
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
  }
  .actions fieldset {
    margin-left: 5px;
  }
  .metric {
    margin-bottom: 0;
  }
  .metric label {
    font-size: 0.875em;
  }
</style>
