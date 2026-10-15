# Collection multiplatform benchmarks

This module uses kotlinx-benchmark to measure collection performance on Node.js and native
targets on Linux x64 and Apple Silicon macOS hosts. Benchmarks live in `src/commonMain`.

## Run on JavaScript

Run from the AndroidX checkout with the repository toolchains configured:

```sh
PROJECT_PREFIX=:collection ./gradlew :collection:collection-benchmark-kmp:jsBenchmark
```

The task runs the Node.js benchmarks and prints the location of the reports. Use the default KMP target settings
when running this module.

For offline builds, see the [JS dependency setup](../../kotlin-js-store/README.md).
