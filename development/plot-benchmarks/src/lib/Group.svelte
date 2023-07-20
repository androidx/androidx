<script lang="ts">
  import { Session, type IndexedWrapper } from "../wrappers/session.js";
  import Dataset from "./Dataset.svelte";

  export let className: string;
  export let datasetGroup: IndexedWrapper[];
  let datasetNames: Set<string>;
  $: {
    datasetNames = Session.datasetNames(datasetGroup);
  }
</script>

<details open>
  <summary>{className}</summary>
  <div class="details">
    {#each datasetNames as name (name)}
      <Dataset {datasetGroup} {name} />
    {/each}
  </div>
</details>
