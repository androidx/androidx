# Performance Monitoring

[TOC]

All AndroidX benchmarks run continuously in Android Engprod infra (unlike
correctness tests), and can be monitored via our instance of Skia Perf -
https://androidx-perf.skia.org.

NOTE: While all benchmark metrics are uploaded and can be inspected, regression
detection only applies to a specific subset on specific devices, defined
[here](https://androidx2-perf.skia.org/a). If you want a new metric to trigger
regressions, start a discussion on go/androidx-bench-chat.

### Contributor Expectations

-   Respect the limited device pool available, and only enable a minimal amount
    of benchmarks in CI to cover your code.

    -   For example, avoid excessive parameterization in CI, consider leaving
        the additional configurations commented out for local
        evaluation/experimentation

-   Document observed changes in runtime on your device for each CL that adds
    benchmarks, or adds significantly to existing benchmarks

    -   E.g. estimated added post-submit runtime(per-device)

### Triage

See go/androidx-bench-triage for triage process.

### Graphing CI Results

Go to the Home page of AndroidX Skia perf: https://androidx2-perf.skia.org/

![Initial filter state](../benchmarking_images/query_first.png "Filter by class")

First, type in the class name you care about select `test_class` in the left
side filter, and click the class. In this example, we're looking for
`LazyListScrollingBenchmark`.

![Filter by metric](../benchmarking_images/query_second.png "Filter by method")

Then, click the X, and select the `method` filter on the left, and you can
select any methods you want to graph. (Shift-click to select multiple)

Hit 'Plot', and you'll see recent results for that benchmark:

![Result plot](../benchmarking_images/result_plot.png "Result plot")

By default, the following filters will be set, so override if you like:

*   `device_name` = `oriole` (Pixel 6)
*   `os_version` = `API_37_REL`
*   `metric` = `timeNs` NOTE: for macrobenchmarks, you'll need something else!
*   `stat` = `min` NOTE: for macrobenchmarks, you'll want median (for e.g.
    startup) or P50,P90 (for e.g. frame timing)

If you have a device that doesn't have a label yet, you can also filter by
`bot` = `<your-atp-device-id>`.

If you just want to grab a recent benchmark trace from CI, you can get it from
either Sponge or AnTS.

For more information on the metrics shown here, check out the
[metrics docs](/docs/metrics.md).

If you'd like to look at traces or information from the test run, click on a
data point and select the `ATI Page` link. You'll also see information like
AndroidX build ID, OS fingerprint, and ART mainline version.

Click the link like `Commits At Step (NNNNN - MMMMM)` to see changes between the
current data point and the previous. Unfortunately this doesn't yet link to AOSP
CLs, and only uses superproject commits.

If you'd like to download the data (e.g. for spreadsheet analysis), hit the
`CSV` button.
