# Introduction

A Gradle Plugin to build and generate benchmarking results for KMP iOS benchmarks.

* Generates Skia Dashboard compatible results.
* Automatically generates the XCode project, and runs benchmarks on a target device running iOS
  or macOS (simulator or physical devices).

# Usage

A KMP project needs to do something like:

```groovy
plugins {
    id("androidx.benchmark.darwin")
}
```

and then it can use the `darwinBenchmark` block like so:

```groovy
darwinBenchmark {
    // XCodegen Schema YAML
    xcodeGenConfigFile = project.rootProject.file(
            "benchmark/benchmark-darwin-samples-xcode/xcodegen-project.yml"
    )
    // XCode project name
    xcodeProjectName = "benchmark-darwin-samples-xcode"
    // iOS app scheme
    scheme = "testapp-ios"

    // Destination
    // ios 13, 17.0
    destination = "platform=iOS Simulator,name=iPhone 13,OS=17.0"
    // Or a target device id
    destination = "id=7F61C467-4E4A-437C-B6EF-026FEEF3904C"

    // The XCFrameworkConfig name
    xcFrameworkConfig = "AndroidXDarwinSampleBenchmarks"
}
```

Example metrics look like:

```json
{
  "key": {
    "testDescription": "Allocate an ArrayList of size 1000",
    "metricName": "Memory Peak Physical",
    "metricIdentifier": "com.apple.dt.XCTMetric_Memory.physical_peak",
    "polarity": "prefers smaller",
    "units": "kB"
  },
  "measurements": {
    "stat": [
      {
        "value": "min",
        "measurement": 0.0
      },
      {
        "value": "median",
        "measurement": 0.0
      },
      {
        "value": "max",
        "measurement": 0.0
      },
      {
        "value": "stddev",
        "measurement": 0.0
      }
    ]
  }
}
```
