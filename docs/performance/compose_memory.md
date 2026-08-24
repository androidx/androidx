# Compose Memory Performance and Tooling

[TOC]

## What is Memory?

At the OS level, memory is tracked by how much address space and physical RAM a
process consumes. Android systems primarily use the following metrics to account
for memory:

*   **VSS (Virtual Set Size):** The total amount of virtual address space
    accessible to a process. This includes memory that has been allocated but
    not yet written to, meaning it is not necessarily backed by physical RAM.
*   **RSS (Resident Set Size):** The portion of a process's memory that is
    actively held in physical RAM. On Android, **Anonymous RSS** (dynamically
    allocated memory, such as the Java/ART heap, that is not backed by files)
    combined with **Swap** (memory compressed in zRAM) serves as the primary
    indicator of a process's active memory footprint.

For more details on how Android handles physical memory, refer to the
[Android Memory Overview](https://developer.android.com/topic/performance/memory-overview).
To learn more about tracking these metrics during execution, see the
[Perfetto Memory Case Study](https://perfetto.dev/docs/case-studies/memory).

## How to measure memory

The primary tool for macrobenchmarks is the AndroidX `MemoryUsageMetric`.

*   **Modes**:
    *   `Max`: Captures the largest sample observed during the measurement. This
        is the primary mode to evaluate peak memory pressure.
    *   `Last`: Captures the last observed value during an iteration. This
        should be reserved for tracking down memory leaks or evaluating memory
        at the end of an operation (e.g., startup).
*   **Key SubMetrics**:
    *   `HeapSize`: Tracks the total size of the managed heap. This is typically
        captured after a Garbage Collection event.
    *   `RssAnon`: Measures memory allocated directly by the process (e.g., via
        `malloc` or `mmap`) that is not backed by a file on disk. It is the
        primary indicator of dynamic memory consumption.
    *   `RssFile`: Measures memory used for file mappings. This includes shared
        libraries, DEX files, and resources.
    *   `Gpu`: Measures memory used by the GPU.
*   **Microbenchmarks**: Allocation count is the relevant metric measurable at
    the microbenchmark level.

## How to analyze memory consumption

### Heap Dumps vs Heap Profiles

There are different formats for different use cases:

*   **hprof (Heap Dump)**: `hprof` is used for Java/ART heap dumps to track
    managed object allocations and references. They are snapshots in time.
    Roughly equivalent to the number measured by `MemoryUsageMetric`'s
    **`Last`** measurement mode.
*   **pprof (Heap Profile)**: `pprof` is a sampled heap profile used for
    profiling memory over time. Helpful for investigating results of
    `MemoryUsageMetric`'s **`Max`** measurement mode.

In most cases, you will likely want to capture pprofs. For example, pprofs let
you record the allocations during a startup, scroll or navigation transition.

If you are investigating a specific state that you know memory is bad in, a heap
dump can be helpful. For example, for a memory leak investigation, a heap dump
will tell you what was retained in the memory at a certain point in time. Since
heap dumps only capture a specific point in time, be mindful of Garbage
Collection (GC), which might clear references you would have wanted to analyze.

### Capturing Heap Dumps/Heap Profiles

Currently, memory analysis can be captured manually using the following tools:

*   **Android Studio Profiler:** Allows capturing a heap dump.
    [Follow the Android Studio documentation](https://developer.android.com/studio/profile/capture-heap-dump).
*   **Perfetto:** Allows capturing heap dumps and heap profiles.
    [Follow the Perfetto documentation](https://perfetto.dev/docs/getting-started/memory-profiling).

### Visualize Heap Dumps/Heap Profiles

Once you have an hprof or a pprof, you can visualize it using the following
tools:

*   **Android Studio:** Android Studio allows opening
    **hprofs**.

*   **Perfetto:** Perfetto has built-in viewers for Perfetto traces containing
    **hprofs and pprofs**.
