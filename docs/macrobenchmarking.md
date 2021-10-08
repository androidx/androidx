# Benchmarking in AndroidX

[TOC]

<!-- Copied from macrobenchmark docs -->

<table>
    <tr>
      <td><strong>Macrobenchmark</strong> (new!)</td>
      <td><strong>Benchmark</strong> (existing!)</td>
    </tr>
    <tr>
        <td>Measure high-level entry points (Activity launch / Scrolling a list)</td>
        <td>Measure individual functions</td>
    </tr>
    <tr>
        <td>Out-of-process test of full app</td>
        <td>In-process test of CPU work</td>
    </tr>
    <tr>
        <td>Slow iteration speed (Often several minutes)</td>
        <td>Fast iteration speed (Often less than 10 seconds)</td>
    </tr>
    <tr>
        <td>Results come with profiling traces</td>
        <td>Optional method sampling/tracing</td>
    </tr>
    <tr>
        <td>Min API 29</td>
        <td>Min API 14</td>
    </tr>
</table>

The
[public documentation](https://developer.android.com/studio/profile/macrobenchmark)
for macrobenchmark explains how to use the library. This page focuses on
specifics to writing library macrobenchmarks in the AndroidX repo. If you're
looking for measuring CPU perf of individual functions, see the guide for
MICRObenchmarks [here](benchmarking).

### Writing the benchmark

Benchmarks are just regular instrumentation tests! Just use the
[`MacrobenchmarkRule`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:benchmark/macro-junit4/src/main/java/androidx/benchmark/macro/junit4/MacrobenchmarkRule.kt)
provided by the library:

<section class="tabs">

#### Kotlin {.new-tab}

```kotlin
@RunWith(AndroidJUnit4::class)
class MyStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "mypackage.myapp",
        metrics = listOf(StartupTimingMetric()),
        startupMode = StartupMode.COLD,
        iterations = 5
    ) { // this = MacrobenchmarkScope
        pressHome()
        val intent = Intent()
        intent.setPackage("mypackage.myapp")
        intent.setAction("mypackage.myapp.myaction")
        startActivityAndWait(intent)
    }
}
```

#### Java {.new-tab}

```java
// TODO: Java APIs are not yet available.
```

</section>

## Project structure

As in the public documentation, macrobenchmarks in the AndroidX repo are
comprised of an app, and a separate macrobenchmark module. Additional setups
steps/constraints for the AndroidX repository are listed below.

1.  App and macrobenchmark modules must be unique, and map 1:1.

1.  Target app path in `settings.gradle` must end with
    `:integration-tests:macrobenchmark-target`.

1.  Macrobenchmark library path must be at the same path, but instead ending
    with `:integration-tests:macrobenchmark`

1.  An entry should be placed in AffectedModuleDetector to recognize
    macrobenchmark dependency on target app module,
    [for example](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/dependencyTracker/AffectedModuleDetector.kt;l=518;drc=cfb504756386b6225a2176d1d6efe2f55d4fa564)

Compose Macrobenchmark Examples:

*   [`:compose:integration-tests:macrobenchmark-target`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark-target/)

*   [`:compose:integration-tests:macrobenchmark`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark/)

*   [AffectedModuleDetector Entry](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/dependencyTracker/AffectedModuleDetector.kt;l=526;drc=cfb504756386b6225a2176d1d6efe2f55d4fa564)

Note: Compose macrobenchmarks are generally duplicated with View system
counterparts, defined in `:benchmark:integration-tests:macrobenchmark-target`.
This is how we compare performance of the two systems.
