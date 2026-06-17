# Benchmarking Jetpack Compose

[TOC]

## Introduction to Compose Benchmarking

Benchmarking Jetpack Compose involves specific considerations compared to the
View system. In microbenchmarks we generally track the three phases of
rendering: Recomposition, Layout, and Draw, but in macrobenchmarks we look more
at metrics like frame duration and jank.

It is important to understand the difference between the two types of benchmarks
we use:

*   **Macrobenchmarks:** Measure high-level app interactions (e.g., app startup
    or scrolling a list). They test the full out-of-process app performance.
*   **Microbenchmarks:** Measure specific function calls in a tight loop (e.g.,
    recomposition of a specific UI component).

For a core explanation of the differences between Microbenchmarks and
Macrobenchmarks, see the [AndroidX Benchmarking Overview](benchmarking.md).

## Standard Reference Device: Oriole (Pixel 6)

When running benchmarks for Compose to establish baselines or test regressions,
**Oriole (Pixel 6)** is the standard reference device.

*   It provides a consistent baseline for our measurements and is representative
    of modern device topologies.
*   For hardware setup instructions to reduce variance (e.g., locking clocks,
    disabling JIT), please refer to the
    [AndroidX Microbenchmarking Guide](microbenchmarking.md).

## Interpreting Results and Metrics

When reviewing benchmark output, you will typically focus on metrics such as
**Time to Initial Display (TTID)**, **Time to Full Display (TTFD)**, **Jank
Rate** and frame duration.

For a breakdown of Compose performance metrics, how they are calculated, and
what they signify, please read our dedicated
[Compose Metrics Overview](compose_metrics.md).

### Skia Perf Dashboards

You can view CI results for Compose benchmarks on the
[AndroidX Skia Perf Dashboard](https://androidx-perf.skia.org). See the
[Performance Monitoring Guide](monitoring.md) for how to filter and view
results.
