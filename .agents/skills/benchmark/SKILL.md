---
name: benchmark
description: Benchmarking and improving the performance of Jetpack Compose. Use this skill when requested to run, analyze, or create microbenchmarks and macrobenchmarks for Compose components or features.
---

# Overview 

This skill focuses on benchmarking and improving the performance of Compose.

## Core Principles

- **Prefer Existing Benchmarks:** Focus mostly on running existing benchmarks. ONLY add new ones if the requested scenario is not tested or when explicitly requested.
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
    1. Run `./benchmark/gradle-plugin/src/main/resources/scripts/lockClocks.sh`.
    2. Run `./benchmark/gradle-plugin/src/main/resources/scripts/disableJit.sh` to stabilize the platform.
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
  - **Example:** `./gradlew :compose:ui:ui-benchmark:connectedReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=.*MyBenchmark.myMethod.*`
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
