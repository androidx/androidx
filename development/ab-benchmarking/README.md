# Local A/B MicroBenchmark Automation Tool

This tool automates the process of running local A/B performance micro benchmarks by
comparing two Git revisions (branches, commits, tags, etc.). It is designed to help developers
quickly measure the performance impact of their changes before code submission.

The script performs the following actions:

1.  Runs a specified benchmark test multiple times on a base revision (e.g., `main`).
2.  Runs the same test multiple times on a comparison revision (e.g., a feature branch or `HEAD~1`).
3.  Collects the timing results from all test runs for both revisions.
4.  Performs a statistical analysis to determine if there is a significant performance difference between the two revisions.
5.  Outputs a human-readable summary, a machine-readable CSV summary, a metadata file, and a histogram plot of the results.

## Prerequisites

Before running the tool, please ensure the following conditions are met:

1.  **Clean Git Working Directory**: Your repository must have no uncommitted changes, staged files, or untracked files. The script will perform a check and exit if the working tree is not clean. Please commit or stash your changes.
2.  **Connected Android Device(s)**:
    *   Benchmarks can only be run on a connected Android device.
    *   If a single device is connected via ADB, the script will automatically target it.
    *   If multiple devices are connected, you **must** use the `--serial` flag to specify which one to use. The script will exit with an error if multiple devices are detected without a specified ID.
3.  **Valid Git Revisions**: The Git revisions you provide for comparison must be valid and exist locally.

## Reducing Noise

To get more stable and reliable benchmark results, it's important to minimize
environmental noise. Here are some recommendations:

*   **Disable JIT (Just-In-Time) Compilation**: The JIT compiler can introduce
    variability. Use the provided script to disable it:
    ```bash
    ./benchmark/gradle-plugin/src/main/resources/scripts/disableJit.sh
    ```
*   **Lock CPU and GPU Clocks**: Fluctuations in clock speeds can affect
    measurements. Lock them with:
    ```bash
    ./benchmark/gradle-plugin/src/main/resources/scripts/lockClocks.sh
    ```
*   **Use a aosp-userdebug Build**: Flash your device with a `userdebug` build of AOSP
    for more performance control. AOSP build do not have GMS services hences reduces background interference.
*   **Minimize Device Activity**:
    *   Disable Wi-Fi, mobile data, and NFC.
    *   Enable Airplane Mode.
    *   Clear all applications from the "Recents" screen.

## Usage

The script is executed via Gradle from the `development/ab-benchmarking` directory.

```bash
./gradlew run --args="<rev_a> <rev_b> <module> <benchmarkTest> [options]"
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
    *   *Example*: `compose:ui:ui-benchmark`
4.  `benchmark_test` (String): The fully qualified class name of the benchmark test to run.
    *   *Example*: `androidx.compose.ui.benchmark.accessibility.AccessibilityBenchmark`
    *   **Note**: To run a single method within a test class, use the format `ClassName#methodName`.
    *   *Example*: `androidx.compose.ui.benchmark.ModifiersBenchmark#full[clickable_1x]`

### Options

*   `--run_count` (Int): The number of times the entire test suite should be run on *each* revision to gather a sample set. For example, a `run_count` of 10 will result in 10 test executions on `rev_a` and 10 on `rev_b`. Defaults to `1`.
*   `--iteration_count` (Int): The number of internal iterations the benchmark framework should perform in a single test run. This is passed directly to the `androidx.benchmark.iterations` argument. Defaults to `50`.
*   `--serial` (String): The serial ID of the target Android device to use for benchmarking. This is **required** if more than one device is connected. Use the `adb devices` command to find the ID.
*   `--output_path` (String): The path where temporary and final result files should be stored. This includes intermediate CSV files, the final `.metadata.json` file, and a histogram plot. Defaults to `~/androidx-main/frameworks/support/development/ab-benchmarking/app/build/benchmark-results/`.

## Example Commands

### Comparing Branches

Here is an example that compares the `main` branch against a feature branch named `my-perf-fix`.

```bash
./gradlew run --args="main my-perf-fix compose:ui:ui-benchmark androidx.compose.ui.benchmark.accessibility.AccessibilityBenchmark --run_count 5 --iteration_count 1000 --serial emulator-5554"
```

### Comparing a Commit Against its Parent

To measure the impact of the very last commit on the current branch, you can compare `HEAD` with its parent, `HEAD~1`.

```bash
./gradlew run --args="HEAD~1 HEAD compose:ui:ui-benchmark androidx.compose.ui.benchmark.accessibility.AccessibilityBenchmark --run_count 3 --iteration_count 1500 --serial emulator-5554"
```

### Comparing a Single Benchmark Method

To isolate the performance of a specific method within a benchmark class, use the `#` separator.

```bash
./gradlew run --args="main my-perf-fix compose:ui:ui-benchmark androidx.compose.ui.benchmark.accessibility.AccessibilityBenchmark#mySpecificTest --run_count 5 --iteration_count 500 --serial emulator-5554"
```

## Interpreting the Output

The tool produces four forms of output: a human-readable summary, a machine-readable CSV line, a metadata JSON file, and a histogram plot.

### Statistical Summary

The summary provides descriptive statistics for the benchmark timings (in nanoseconds) from both datasets (revisions) and an analysis of their difference.

```
--- Comparison for: fetchAccessibilityNodeInfo ---
                             Dataset 1 (Branch A)   | Dataset 2 (Branch B)
--------------------------------------------------------------------------
Count                        | 100                  | 100
Min (ns)                     | 170276.08            | 171739.96
Mean (ns)                    | 180593.75            | 180930.72
Median (ns)                  | 181528.44            | 182427.48
Std. Dev. (ns)               | 4490.37              | 5726.01
Min Difference:              | 1463.88 ns (0.86%)
Mean Difference:             | 336.98 ns (0.19%)
Median Difference:           | 899.04 ns (0.50%)
95% CI of Diff:              | [-555.54, 2070.51] ns ([-0.31%, 1.14%])
P-value (Mann-Whitney U):    | 0.4954

The confidence interval contains zero, suggesting no statistically significant difference between the medians.

-------------------------------------------------------
```

*   **Descriptive Statistics**: `Count`, `Min`, `Mean`, `Median`, and `Std. Dev.` provide a basic overview of the timing distributions for each revision.
*   **Median Difference**: Shows the absolute and percentage change between the median of Rev B and Rev A. A negative value indicates a performance improvement (Rev B is faster).
*   **95% CI of Diff (BCa Bootstrap)**: The 95% Bias-Corrected and Accelerated (BCa) bootstrap confidence interval for the median difference.
    *   If this interval **does not** contain zero, there is strong evidence that a real performance difference exists.
    *   If this interval **does** contain zero, the observed difference may be due to random chance.
*   **P-value**: The result of the Mann-Whitney U Test.
    *   A p-value less than `0.05` indicates a **statistically significant** difference between the two revisions.
    *   A p-value greater than or equal to `0.05` suggests no statistically significant difference was detected.

### Machine-Readable CSV

A single CSV line is printed for easy parsing by other scripts or for logging results.

```
--- Machine-Readable CSV ---
benchmarkName,count,min1,min2,min_diff,min_diff_%,mean1,mean2,mean_diff_%,median1,median2,p-value,median_diff_%,median_diff,median_diff_ci_lower,median_diff_ci_upper,median_diff_ci_lower_%,median_diff_ci_upper_%
withTrailingLambdas_compose,100,160768.30,160433.01,-335.28,-0.21%,167300.93,167229.12,-0.04%,164604.77,164904.08,0.5675,0.18%,299.31,-678.86,1390.19,-0.41%,0.84%
```
*   `benchmarkName`: The name of the benchmark test method.
*   `count`: The number of measurements taken for each revision.
*   `min1`: The minimum timing value for revision A.
*   `min2`: The minimum timing value for revision B.
*   `min_diff`: The absolute difference in minimums (min2 - min1).
*   `min_diff_%`: The percentage difference in minimums.
*   `mean1`: The mean (average) timing for revision A.
*   `mean2`: The mean (average) timing for revision B.
*   `mean_diff_%`: The percentage difference in means.
*   `median1`: Median of the baseline revision (A).
*   `median2`: Median of the comparison revision (B).
*   `p-value`: The p-value from the Mann-Whitney U test.
*   `median_diff_%`: The percentage difference in medians (`(median2 - median1) / median1`).
*   `median_diff`: The absolute difference in medians (`median2 - median1`).
*   `median_diff_CI_lower`: The lower bound of the 95% BCa confidence interval for the median difference.
*   `median_diff_CI_upper`: The upper bound of the 95% BCa confidence interval for the median difference.
*   `median_diff_CI_lower_%`: The lower bound of the BCa confidence interval as a percentage.
*   `median_diff_CI_upper_%`: The upper bound of the BCa confidence interval as a percentage.

### Metadata File

A JSON file named `metadata.json` is created in the output directory. It contains information about the benchmark run, including:
*   Timestamp of the execution
*   Git revision information (name and commit hash)
*   Device information
*   Input parameters for the run

### Histogram Plot

A PNG image file named `<benchmark_name>_histogram.png` is created in the output directory, where benchmark_name is the name of the benchmark test method.
This plot visualizes the distribution of the benchmark timings for both revisions, making it easier to spot differences in performance.
**Note**: The `<path_to_output_dir>` is the value passed to the `--output_path` parameter. 
If this parameter is not specified, it defaults to `~/androidx-main/frameworks/support/development/ab-benchmarking/app/build/benchmark-results/`.
```
--- Graphical Plot ---
Saved histogram to: file://<path_to_output_dir>/<benchmark_name>_histogram.png
```