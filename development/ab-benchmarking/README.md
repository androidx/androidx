# Local A/B Benchmarking Automation Tool

This tool automates the process of running local A/B performance micro benchmarks by
comparing two Git revisions (branches, commits, tags, etc.). It is designed to help developers
quickly measure the performance impact of their changes before code submission.

## Available Tools

* **[MacroBenchmark Tool](./app/src/main/kotlin/androidx/abbenchmarking/macrobenchmarking/README-MACRO.md)**: For A/B Macro benchmarking
* **[MicroBenchmark Tool](./app/src/main/kotlin/androidx/abbenchmarking/microbenchmarking/README-MICRO.md)**: For A/B Micro benchmarking

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

> **For specific command-line arguments, usage examples, and example output, please see the README for the specific tool you are using:**
> * **[MacroBenchmark Tool](./app/src/main/kotlin/androidx/abbenchmarking/macrobenchmarking/README-MACRO.md)**: For A/B Macro benchmarking
> * **[MicroBenchmark Tool](./app/src/main/kotlin/androidx/abbenchmarking/microbenchmarking/README-MICRO.md)**: For A/B Micro benchmarking