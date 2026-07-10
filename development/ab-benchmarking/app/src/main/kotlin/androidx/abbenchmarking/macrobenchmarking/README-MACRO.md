# Local A/B MacroBenchmark Automation Tool

## Usage

The script is executed via Gradle from the `development/ab-benchmarking` directory.

```bash
./gradlew :app:runMacrobenchmark --args="<rev_a> <rev_b> <module> <benchmarkTest> [options]"
```

### Finding device serial
To find the serial ID of all connected devices, run the following ADB command in your terminal:
```bash
adb devices
```
The output will list your connected devices. The string in the first column is the serial ID.
```
List of devices attached
emulator-5554   device
123456789ABCDEF device
```

### Command-Line Arguments

The script accepts the following positional arguments in order:

1.  `rev_a` (String): The first Git revision to test (e.g., a branch, commit hash, tag, or `HEAD`).
2.  `rev_b` (String): The second Git revision to test.
3.  `module` (String): The Gradle module path containing the benchmark test.
    *   *Example*: `compose:integration-tests:macrobenchmark`
4.  `benchmark_test` (String): The fully qualified class name of the benchmark test to run.
    *   *Example*: `androidx.compose.integration.macrobenchmark.SampleMacroBenchmark`
    *   **Note**: To run a single method within a test class, use the format `ClassName#methodName`.
    *   *Example*: `androidx.compose.integration.macrobenchmark.SampleMacroBenchmark#startup`

### Options

*   `--run_count` (Int): The number of times the entire test suite should be run on *each* revision to gather a sample set. For example, a `run_count` of 10 will result in 10 test executions on `rev_a` and 10 on `rev_b`. Defaults to `1`.
*   `--serial` (String): The serial ID of the target Android device to use for benchmarking. This is **required** if more than one device is connected. Use the `adb devices` command to find the ID.
*   `--output_path` (String): The path where temporary and final result files should be stored. This includes intermediate CSV files, the final `.metadata.json` file, and a histogram plot. Defaults to `~/androidx-main/frameworks/support/development/app/build/benchmark-results/`.

## Example Commands

### Comparing Branches

Here is an example that compares the `main` branch against a feature branch named `my-perf-fix`.

```bash
./gradlew :app:runMacrobenchmark --args="main my-perf-fix :compose:integration-tests:macrobenchmark androidx.compose.integration.macrobenchmark.SampleMacroBenchmark --run_count 5 --serial 123456789ABCDEF"
```
### Comparing a Commit Against its Parent

To measure the impact of the very last commit on the current branch, you can compare `HEAD` with its parent, `HEAD~1`.

```bash
./gradlew :app:runMacrobenchmark --args="HEAD~1 HEAD :compose:integration-tests:macrobenchmark androidx.compose.integration.macrobenchmark.SampleMacroBenchmark --run_count 5 --serial 123456789ABCDEF"
```

### Comparing a Single Benchmark Method

To isolate the performance of a specific method within a benchmark class, use the `#` separator.

```bash
./gradlew :app:runMacrobenchmark --args="main my-perf-fix :compose:integration-tests:macrobenchmark androidx.compose.integration.macrobenchmark.SampleMacroBenchmark --run_count 5 --serial 123456789ABCDEF"
```

## Interpreting the Output

The tool produces four forms of output: a human-readable summary, a machine-readable CSV line, a metadata JSON file, and a histogram plot.

### Statistical Summary

The summary provides descriptive statistics for the benchmark timings (in nanoseconds) from both datasets (revisions) and an analysis of their difference.

```
================================================================================
          Statistical Summary for Benchmark: startup
================================================================================

--- Comparison for: frameCount ---
                          | Dataset 1 (Branch A) | Dataset 2 (Branch B)
----------------------------------------------------------------
Count                     | 100                  | 100
Min                       | 1.00                 | 1.00
Mean                      | 1.04                 | 1.09
Max                       | 2.00                 | 2.00
Median                    | 1.00                 | 1.00
Std. Dev.                 | 0.20                 | 0.29
Min Difference:           | 0.00 (0.00%)
Mean Difference:          | 0.05 (4.81%)
Max Difference:           | 0.00 (0.00%)
Median Difference:        | 0.00 (0.00%)
95% CI of Diff:           | [0.00, 0.00] ([0.00%, 0.00%])
P-value (Mann-Whitney U): | 0.5413

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------


--- Comparison for: timeToInitialDisplayMs ---
                          | Dataset 1 (Branch A) | Dataset 2 (Branch B)
----------------------------------------------------------------
Count                     | 100                  | 100
Min (ms)                  | 229.92               | 219.64
Mean (ms)                 | 396.67               | 394.59
Max (ms)                  | 758.94               | 646.69
Median (ms)               | 401.84               | 397.09
Std. Dev. (ms)            | 65.38                | 70.87
Min Difference:           | -10.28 ms (-4.47%)
Mean Difference:          | -2.08 ms (-0.53%)
Max Difference:           | -112.26 ms (-14.79%)
Median Difference:        | -4.75 ms (-1.18%)
95% CI of Diff:           | [-14.56, 8.64] ms ([-3.62%, 2.15%])
P-value (Mann-Whitney U): | 0.4201

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------


--- Comparison for: frameDurationCpuMs ---
                          | Dataset 1 (Branch A) | Dataset 2 (Branch B)
----------------------------------------------------------------
Count                     | 104                  | 109
Min (ms)                  | 4.04                 | 4.31
Mean (ms)                 | 151.84               | 142.32
Max (ms)                  | 219.76               | 207.40
Median (ms)               | 159.68               | 156.32
P90 (ms)                  | 178.07               | 178.78
P95 (ms)                  | 185.19               | 184.04
P99 (ms)                  | 219.15               | 207.15
Std. Dev. (ms)            | 35.75                | 46.64
Min Difference:           | 0.26 ms (6.50%)
Mean Difference:          | -9.52 ms (-6.27%)
Max Difference:           | -12.36 ms (-5.62%)
Median Difference:        | -3.37 ms (-2.11%)
95% CI of Diff:           | [-6.81, 0.62] ms ([-4.26%, 0.39%])
P-value (Mann-Whitney U): | 0.0864

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------


--- Comparison for: frameOverrunMs ---
                          | Dataset 1 (Branch A) | Dataset 2 (Branch B)
----------------------------------------------------------------
Count                     | 104                  | 109
Min (ms)                  | 93.51                | 89.69
Mean (ms)                 | 222.83               | 221.50
Max (ms)                  | 622.78               | 526.67
Median (ms)               | 245.83               | 241.15
P90 (ms)                  | 282.58               | 285.55
P95 (ms)                  | 301.53               | 339.97
P99 (ms)                  | 612.88               | 524.45
Std. Dev. (ms)            | 76.20                | 77.86
Min Difference:           | -3.82 ms (-4.08%)
Mean Difference:          | -1.33 ms (-0.60%)
Max Difference:           | -96.11 ms (-15.43%)
Median Difference:        | -4.69 ms (-1.91%)
95% CI of Diff:           | [-13.38, 7.96] ms ([-5.44%, 3.24%])
P-value (Mann-Whitney U): | 0.4836

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------
```

*   **Descriptive Statistics**: `Count`, `Min`, `Mean`, `Max` `Median`, and `Std. Dev.` provide a basic overview of the timing distributions for each revision. Additional percentiles (`P90`, `P95`, `P99`) are shown only for specific metrics like `frameDurationCpuMs` and `frameOverrunMs`.
*   **Median Difference**: Shows the absolute and percentage change between the median of Rev B and Rev A. A negative value indicates a performance improvement (Rev B is faster).
*   **95% CI of Diff (BCa Bootstrap)**: The 95% Bias-Corrected and Accelerated (BCa) bootstrap confidence interval for the difference between the medians.
    *   If this interval **does not** contain zero, there is strong evidence that a real performance difference exists.
    *   If this interval **does** contain zero, the observed difference may be due to random chance.
*   **P-value**: The result of the Mann-Whitney U Test.
    *   A p-value less than `0.05` indicates a **statistically significant** difference between the two revisions.
    *   A p-value greater than or equal to `0.05` suggests no statistically significant difference was detected.

### Machine-Readable CSV

A single CSV line is printed for easy parsing by other scripts or for logging results.

```
--- Machine-Readable CSV for Benchmark: startup ---
benchmark_name,metric_name,count,min1,min2,min_diff,min_diff_%,max1,max2,max_diff,max_diff_%,mean1,mean2,mean_diff_%,median1,median2,p-value,median_diff_%,median_diff,median_diff_ci_lower,median_diff_ci_upper,median_diff_ci_lower_%,median_diff_ci_upper_%,p90_1,p95_1,p99_1,p90_2,p95_2,p99_2
startup,frameCount,100,1.00,1.00,0.00,0.00%,2.00,2.00,0.00,0.00%,1.04,1.09,4.81%,1.00,1.00,0.5413,0.00%,0.00,0.00,0.00,0.00%,0.00%,1.00,1.00,2.00,1.00,2.00,2.00
startup,timeToInitialDisplayMs,100,229.92,219.64,-10.28,-4.47%,758.94,646.69,-112.26,-14.79%,396.67,394.59,-0.53%,401.84,397.09,0.4201,-1.18%,-4.75,-14.56,8.64,-3.62%,2.15%,437.30,453.61,756.90,442.43,541.65,646.68
startup,frameDurationCpuMs,104,4.04,4.31,0.26,6.50%,219.76,207.40,-12.36,-5.62%,151.84,142.32,-6.27%,159.68,156.32,0.0864,-2.11%,-3.37,-6.81,0.62,-4.26%,0.39%,178.07,185.19,219.15,178.78,184.04,207.15
startup,frameOverrunMs,104,93.51,89.69,-3.82,-4.08%,622.78,526.67,-96.11,-15.43%,222.83,221.50,-0.60%,245.83,241.15,0.4836,-1.91%,-4.69,-13.38,7.96,-5.44%,3.24%,282.58,301.53,612.88,285.55,339.97,524.45
-------------------------------------------------------
```
*   `benchmarkName`: The name of the benchmark test method.
*   `metricName`: The name of the metric.
*   `count`: The number of measurements taken for each revision.
*   `min1`: The minimum timing value for revision A.
*   `min2`: The minimum timing value for revision B.
*   `min_diff`: The absolute difference in minimums (min2 - min1).
*   `min_diff_%`: The percentage difference in minimums.
*   `max1`: The maximum timing value for revision A.
*   `max2`: The maximum timing value for revision B.
*   `max_diff`: The absolute difference in maximums (min2 - min1).
*   `max_diff_%`: The percentage difference in maximums.
*   `mean1`: The mean (average) timing for revision A.
*   `mean2`: The mean (average) timing for revision B.
*   `mean_diff_%`: The percentage difference in means.
*   `median1`: Median of the baseline revision (A).
*   `median2`: Median of the comparison revision (B).
*   `p-value`: The p-value from the Mann-Whitney U test.
*   `median_diff_%`: The percentage difference in medians (`(median2 - median1) / median1`).
*   `median_diff`: The absolute difference in medians (`median2 - median1`).
*   `median_diff_ci_lower`: The lower bound of the 95% BCa confidence interval for the difference between the medians.
*   `median_diff_ci_upper`: The upper bound of the 95% BCa confidence interval for the difference between the medians.
*   `median_diff_ci_lower_%`: The lower bound of the BCa confidence interval as a percentage.
*   `median_diff_ci_upper_%`: The upper bound of the BCa confidence interval as a percentage.
*   `P90_1`: The 90th percentile timing for revision A. This means 90% of the measurements for revision A were at or below this value.
*   `P90_2`: The 90th percentile timing for revision B.
*   `P95_1`: The 95th percentile timing for revision A.
*   `P95_2`: The 90th percentile timing for revision B.
*   `P99_1`: The 95th percentile timing for revision A.
*   `P99_2`: The 95th percentile timing for revision B.

### Metadata File

A JSON file named `metadata.json` is created in the output directory. It contains information about the benchmark run, including:
*   Timestamp of the execution
*   Git revision information (name and commit hash)
*   Device information
*   Input parameters for the run

### Histogram Plot

A PNG image file named `<benchmark_name>_<metric_name>_histogram.png` is created in the output directory, where benchmark_name is the name of the benchmark test method.
This plot visualizes the distribution of the benchmark timings for both revisions, making it easier to spot differences in performance.
**Note**: The `<path_to_output_dir>` is the value passed to the `--output_path` parameter.
If this parameter is not specified, it defaults to `~/androidx-main/frameworks/support/development/ab-benchmarking/app/build/benchmark-results/`.
```
--- Generating Histogram Plots for Benchmark: startup ---
Saved histogram for startup - frameCount to: file://<path_to_output_dir>/startup_frameCount_histogram.png
Saved histogram for startup - timeToInitialDisplayMs to: file://<path_to_output_dir>/startup_timeToInitialDisplayMs_histogram.png
Saved histogram for startup - frameDurationCpuMs to: file://<path_to_output_dir>/startup_frameDurationCpuMs_histogram.png
Saved histogram for startup - frameOverrunMs to: file://<path_to_output_dir>/startup_frameOverrunMs_histogram.png
-------------------------------------------------------
```