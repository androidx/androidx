<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import type { Selection, SelectionEvent } from "../types/events.js";
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Dataset from "./Dataset.svelte";

  export let className: string;
  export let datasetGroup: IndexedWrapper[];

  let dispatcher = createEventDispatcher<SelectionEvent>();
  let datasetNames: Set<string>;

  let selection = function (event: CustomEvent<Selection[]>) {
    // Forward events.
    dispatcher("selections", event.detail);
  };

  $: {
    datasetNames = Session.datasetNames(datasetGroup);
  }
</script>

<details open>
  <summary>{className}</summary>
  <div class="details">
    {#each datasetNames as name (name)}
      <Dataset {datasetGroup} {name} on:selections={selection} />
    {/each}
  </div>
</details>
