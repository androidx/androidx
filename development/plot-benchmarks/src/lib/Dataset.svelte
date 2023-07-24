<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import type { SelectionEvent, Selection } from "../types/events.js";

  export let name: string;
  export let datasetGroup: IndexedWrapper[];

  // State
  let dispatcher = createEventDispatcher<SelectionEvent>();
  let selected: boolean = true;
  let sources: Set<string>;
  let sampledMetrics: Set<string>;
  let metrics: Set<string>;

  // Events
  let selection = function (event: Event) {
    event.stopPropagation();
    const target = event.target as HTMLInputElement;
    selected = target.checked;
    const selection: Selection = {
      name: name,
      enabled: selected,
    };
    dispatcher("selections", [selection]);
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
      <fieldset class="item">
        <label for="switch">
          <input
            type="checkbox"
            role="switch"
            checked={selected}
            on:change={selection}
          />
        </label>
      </fieldset>
    </div>
    <div class="details">
      <div class="sources">
        {#each sources as source (source)}
          <div>📁 <small>{source}</small></div>
        {/each}
      </div>
      {#if metrics.size > 0}
        <div class="metrics">
          {#each metrics as metric}
            <div>📏 <small>{metric}</small></div>
          {/each}
        </div>
      {/if}
      {#if sampledMetrics.size > 0}
        <div class="sampled">
          {#each sampledMetrics as metric}
            <div>📏 <small>{metric}</small></div>
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
</style>
