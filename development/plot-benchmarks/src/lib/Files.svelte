<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import type { Writable } from "svelte/store";
  import { writable } from "svelte/store";
  import type { FileMetadata, FileMetadataEvent } from "../files.js";
  import { readBenchmarks } from "../files.js";

  let active: Writable<boolean> = writable(false);
  let entries: Writable<Array<FileMetadata>> = writable([]);
  let dispatcher = createEventDispatcher<FileMetadataEvent>();

  entries.subscribe(($entries) => {
    // Dispatch an event anytime files change.
    dispatcher("entries", [...$entries]);
  });

  function onDropFile(event: DragEvent) {
    handleDropFile(event); // async
    active.set(false);
    event.preventDefault();
  }

  async function handleDropFile(event: DragEvent) {
    const items = [...event.dataTransfer.items];
    const metadata = [];
    if (items) {
      for (let i = 0; i < items.length; i += 1) {
        if (items[i].kind === "file") {
          const file = items[i].getAsFile();
          if (file.name.endsWith(".json")) {
            const benchmarks = await readBenchmarks(file);
            const entry: FileMetadata = {
              enabled: true,
              file: file,
              benchmarks: benchmarks,
            };
            metadata.push(entry);
          }
        }
      }
      // Deep copy
      $entries = [...$entries, ...metadata];
    }
  }

  function onDragOver(event: DragEvent) {
    active.set(true);
    event.preventDefault();
  }

  function onDragLeave(event: DragEvent) {
    active.set(false);
    event.preventDefault();
  }

  function onChecked(entries: Array<FileMetadata>, index: number) {
    return (event: Event) => {
      // Deep copy
      const copied = [...entries];
      copied[index].enabled = !copied[index].enabled;
      $entries = copied;
    };
  }
</script>

<article
  id="drop"
  class="drop"
  class:active={$active}
  on:drop={onDropFile}
  on:dragover={onDragOver}
  on:dragleave={onDragLeave}
>
  {#if $entries.length > 0}
    <div class="files">
      {#each $entries as entry, index}
        <div class="file">
          <input
            type="checkbox"
            checked={entry.enabled}
            on:change={onChecked($entries, index)}
          />
          <div class="index">
            <span>{index + 1}</span>
          </div>
          <span class="label">{entry.file.name}</span>
        </div>
      {/each}
    </div>
  {:else}
    <h5>Drag and drop benchmark results to get started.</h5>
  {/if}
</article>

<style>
  .active {
    background-color: #00bfff;
    outline: gray;
    outline-style: groove;
  }

  .files {
    display: flex;
    flex-direction: column;
    row-gap: 3px;
  }

  .file {
    display: flex;
    flex-direction: row;
    column-gap: 10px;
    align-items: center;
  }

  .index {
    width: 2rem;
    height: 2rem;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    border: 2px solid sandybrown;
  }
</style>
