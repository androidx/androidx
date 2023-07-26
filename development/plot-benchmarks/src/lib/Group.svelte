<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import type {
    Selection,
    SelectionEvent,
    StatEvent,
    StatInfo,
  } from "../types/events.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Dataset from "./Dataset.svelte";

  export let className: string;
  export let datasetGroup: IndexedWrapper[];

  let selectionDispatcher = createEventDispatcher<SelectionEvent>();
  let statDispatcher = createEventDispatcher<StatEvent>();
  let datasetNames: Set<string>;

  // Forward events.
  let selection = function (event: CustomEvent<Selection[]>) {
    selectionDispatcher("selections", event.detail);
  };
  let stat = function (event: CustomEvent<StatInfo[]>) {
    statDispatcher("info", event.detail);
  };

  $: {
    datasetNames = Session.datasetNames(datasetGroup);
  }
</script>

<details open>
  <summary>{className}</summary>
  <div class="details">
    {#each datasetNames as name (name)}
      <Dataset {datasetGroup} {name} on:selections={selection} on:info={stat} />
    {/each}
  </div>
</details>
