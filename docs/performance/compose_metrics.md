# Compose Metrics Overview

[TOC]

## Introduction

This document provides an overview of the key performance metrics tracked when
benchmarking Jetpack Compose.

## Macrobenchmark Metrics

Macrobenchmarks measure end-to-end interactions, focusing on metrics that
reflect the user's perceived performance.

### 1. [Time to Initial Display (TTID)](https://developer.android.com/topic/performance/vitals/launch-time#time-initial)

Time from the system receiving a launch intent to rendering the first frame of
the destination Activity. Includes process initialization, Activity creation,
and the initial drawing of the UI.

### 2. [Time to Full Display (TTFD)](https://developer.android.com/topic/performance/vitals/launch-time#time-full)

Time from the system receiving a launch intent until the application reports
fully drawn via `android.app.Activity.reportFullyDrawn`. The measurement stops
at the completion of rendering the first frame after (or containing) the
`reportFullyDrawn()` call.

TTFD extends beyond TTID to encompass network requests, database loads, and
subsequent UI updates required to populate the full screen. This is the metric
that most developers will care about when it comes to startup.

### 3. Jank Rate

The percentage of frames in the benchmark target process that missed their
assigned frame deadline and resulted in jank (a delay between visible frames
updates on the display).

Warning: `gfxFrameJankPercent` is calculated from dumping the `gfxinfo` system
service. That means it is not easily re-calculable from a Perfetto trace. This
metric will be replaced long-term (b/468046200).

### 4. Frame Time and Frame Overrun

**`frameDurationCpuMs`** is the time it takes to produce a frame on the CPU.
This spans from the start of `Choreographer#DoFrame` until the frame has been
handed off to the GPU from the RenderThread.

**`frameOverrunMs`** measures how much faster or slower than the deadline a
frame was. Positive numbers indicate frame overruns, negative numbers indicate
that the frame was faster than the deadline.

## Microbenchmark Metrics

Microbenchmarks measure the performance of specific operations.

### 1. Median Time (timeNs)

Median time taken to execute a benchmark iteration, e.g. recomposing a
component.

### 2. Allocations

The number of object allocations occurring during a benchmark loop. It's
important to keep allocations low, especially during recurring operations like
scrolling, as allocations increase memory pressure.
