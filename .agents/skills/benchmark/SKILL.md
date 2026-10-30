---
name: benchmark
description: Benchmarking and improving the performance of Jetpack Compose. Use this skill when requested to run, analyze, or create microbenchmarks and macrobenchmarks for Compose components or features.
---

# Overview

This skill focuses on benchmarking and improving the performance of Compose.

## Core Principles

- **Prefer Existing Benchmarks:** Focus mostly on running existing benchmarks. ONLY add new ones if the requested scenario is not tested or when explicitly requested.
- **Use `development/ab-benchmarking` for Comparisons:** When evaluating the performance impact of a code change or comparing two Git revisions/branches, use the automated A/B benchmarking tool in `development/ab-benchmarking` rather than manually running single benchmarks back-to-back. Use direct Gradle test execution when profiling a single revision, inspecting Perfetto traces, or developing a new benchmark.
- **Address Flakiness:** Some benchmarks are flaky. If results are uncertain, run the same benchmark multiple times.
    - 1-2% variation is normal.
    - 10% variation implies a broken benchmark that cannot be trusted.
- **Hardware Requirement:** Always use a real device for microbenchmarks. **DO NOT** use an emulator as it is not representative.

---

## Microbenchmarks

Microbenchmarks measure fine-grained performance (e.g., individual component measure/layout).

### Location & Execution
- **Location:** `<module-name>/benchmark` (e.g., `compose/ui/ui/benchmark`).
- **Command:** Run as an Android test (e.g., `:compose:ui:ui-benchmark:connectedReleaseAndroidTest`).
- **Filter Tests:** Use `-Pandroid.testInstrumentationRunnerArguments.tests_regex` to run specific tests. This is especially useful for parameterized tests where `class#method` filtering might not work as expected.
    - **Example:** `./gradlew :compose:ui:ui-benchmark:connectedReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=.*MyBenchmark.myMethod.*`
    - **Validation:** Always verify the console output to ensure only the intended number of tests were executed (e.g., `Finished 1 tests`). If more tests ran than expected, refine your regex.
- **Preparation:**
    1. Run `./benchmark/gradle-plugin/src/main/resources/scripts/disableJit.sh` to stabilize the platform.
    2. Run `./benchmark/gradle-plugin/src/main/resources/scripts/lockClocks.sh`. Make sure it is run /after/ disabling JIT.
- **Errors:** Do **NOT** suppress configuration errors; they guide proper device setup.

### Analysis
- **Output Path:** `../../out/androidx/compose/<folder>/benchmark/build/outputs/connected_android_test_additional_output/`
- **Files:**
    - `.txt`: Short summary.
    - `.json`: Full data for analysis.
    - `.perfetto_trace`: Detailed trace.
- **Method Tracing:** The trace includes every method executed. Note that method tracing adds overhead and significantly affects timing accuracy, but it is invaluable for verifying which codepaths were executed.
- **Tooling:** Use `~/trace_processor` to analyze traces.

### After benchmark
- **Cleanup:** Reset device with `./benchmark/gradle-plugin/src/main/resources/scripts/resetDevice.sh`

---

## Macrobenchmarks

Macrobenchmarks measure high-level interactions (startup, scrolling) using UI Automator on a real application.

### Location
- **Standard:** `compose/integration-tests/macrobenchmark` (Target app: `macrobenchmark-target`).
- **Hero Benchmarks:** `compose/integration-tests/hero` (More representative of real-world apps).

### Execution & Metrics
- **Command:** Run with `connectedReleaseAndroidTest`.
- **Filter Tests:** Use `-Pandroid.testInstrumentationRunnerArguments.tests_regex` to run specific tests. This is especially useful for parameterized tests where `class#method` filtering might not work as expected.
  - **Example:** `./gradlew :compose:integration-tests:macrobenchmark:connectedReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=.*MyMacrobenchmark.myMethod.*`
  - **Validation:** Always verify the console output to ensure only the intended number of tests were executed (e.g., `Finished 1 tests`). If more tests ran than expected, refine your regex.
- **Metrics:**
    - **Startup:** `timeToInitialDisplayMs`, `timeToFullDisplayMs`.
    - **Scroll:** Frame duration and frame overrun.
- **Output Path:** `../../out/androidx/compose/<folder>/build/outputs/connected_android_test_additional_output/`

### Analysis & Optimization
- **Traces:** These traces do **not** have method traces by design. They contain hand-annotated spans for `recompose`, `measure`, and `layout`.
- **Tooling:** Use `~/trace_processor`.
- **Custom Tracing:** Add temporary trace blocks to investigate specific codepaths:
  ```kotlin
  trace("name") {
      // target code block
  }
  ```

---

## A/B Benchmarking (`development/ab-benchmarking`)

When comparing performance between two Git revisions (e.g., verifying an optimization between `HEAD~1` and `HEAD`, or `main` and a feature branch), use the local A/B benchmarking tool in `development/ab-benchmarking`. It interleaves runs across both revisions, aggregates measurements, computes statistical significance, and generates histogram plots.

### Prerequisites
1. **Clean Git Working Tree:** The repository must have **no uncommitted, staged, or untracked changes** (`git status --porcelain` must be empty), or the tool will exit with an error. Commit local changes before comparing (e.g., compare `HEAD~1` against `HEAD`).
2. **Device Preparation:** For microbenchmarks, run `./benchmark/gradle-plugin/src/main/resources/scripts/disableJit.sh` and `./benchmark/gradle-plugin/src/main/resources/scripts/lockClocks.sh` from the `frameworks/support` root before running A/B comparisons, and `./benchmark/gradle-plugin/src/main/resources/scripts/resetDevice.sh` afterward.
3. **Device Serial:** If multiple devices are connected (`adb devices`), you must pass `--serial <device_id>`.

### Execution
Run from the `frameworks/support` repository root using `-p development/ab-benchmarking` (or omit `-p development/ab-benchmarking` if running directly inside the `development/ab-benchmarking` directory):

- **Microbenchmarks:**
  ```bash
  ./gradlew -p development/ab-benchmarking :app:runMicrobenchmark \
    --args="<rev_a> <rev_b> <module> <benchmark_test> [--run_count <N>] [--iteration_count <M>] [--serial <id>] [--output_path <dir>]"
  ```
  - **Example:**
    ```bash
    ./gradlew -p development/ab-benchmarking :app:runMicrobenchmark \
      --args="HEAD~1 HEAD compose:ui:ui-benchmark androidx.compose.ui.benchmark.ModifiersBenchmark#full[clickable_1x] --run_count 5 --iteration_count 500"
    ```

- **Macrobenchmarks:**
  ```bash
  ./gradlew -p development/ab-benchmarking :app:runMacrobenchmark \
    --args="<rev_a> <rev_b> <module> <benchmark_test> [--run_count <N>] [--serial <id>] [--output_path <dir>]"
  ```
  - **Example:**
    ```bash
    ./gradlew -p development/ab-benchmarking :app:runMacrobenchmark \
      --args="HEAD~1 HEAD :compose:integration-tests:macrobenchmark androidx.compose.integration.macrobenchmark.TrivialStartupBenchmark#startup --run_count 5"
    ```

### Arguments & Filtering Notes
- **`<benchmark_test>` format:** Passed to `-Pandroid.testInstrumentationRunnerArguments.class` (does **not** use `tests_regex`). Specify the fully qualified class name or `ClassName#methodName` (including parameterized suffixes such as `ClassName#method[param]`, without spaces).
- **`--run_count` (default `1`):** Number of interleaved suite executions per revision (`rev_a` then `rev_b`, repeated `N` times).
- **`--iteration_count` (microbenchmarks only, default `50`):** Passed to `androidx.benchmark.iterations` per test run (`total measurements = run_count * iteration_count`).

### Interpreting Results & Outputs
- **Statistical Summary & CSV:** Printed to stdout. Negative median/mean differences indicate `rev_b` is faster than `rev_a`.
- **Statistical Significance:** Look at the **95% BCa Confidence Interval (`95% CI of Diff`)** and **Mann-Whitney U `P-value`**:
  - **Significant difference:** 95% CI does **not** contain zero and `P-value < 0.05`.
  - **No significant difference (noise):** 95% CI contains zero or `P-value >= 0.05`.
- **Output Files:** Written to `--output_path` (defaults to `development/ab-benchmarking/app/build/benchmark-results/`):
  - `<rev_a>.csv` and `<rev_b>.csv`: Raw extracted measurements per revision.
  - `.metadata.json`: Execution timestamp, resolved commit hashes, device info, and parameters.
  - `<benchmark>_<metric>_histogram.png`: Distribution comparison plots.
