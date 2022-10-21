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
        <td>Slow iteration speed (Often more than a minute)</td>
        <td>Fast iteration speed (Often less than 10 seconds)</td>
    </tr>
    <tr>
        <td>Results come with profiling traces</td>
        <td>Optional stack sampling/method tracing</td>
    </tr>
    <tr>
        <td>Min API 23</td>
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
```

#### Java {.new-tab}

```java
    @Rule
    public MacrobenchmarkRule mBenchmarkRule = MacrobenchmarkRule()

    @Test
    public void startup() {
        mBenchmarkRule.measureRepeated(
                "mypackage.myapp",
                Collections.singletonList(new StartupTimingMetric()),
                StartupMode.COLD,
                /* iterations = */ 5,
                scope -> {
                    scope.pressHome();
                    Intent intent = Intent();
                    intent.setPackage("mypackage.myapp");
                    intent.setAction("mypackage.myapp.myaction");
                    scope.startActivityAndWait(intent);
                    return Unit.INSTANCE;
                }
        );
    }
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

### Setup checklist

<table>
    <tr>
      <td><strong>Did you setup...</strong></td>
      <td><strong>Required setup</strong></td>
    </tr>
    <tr>
        <td>Two modules in settings.gradle</td>
        <td>Both the macrobenchmark and target must be defined in sibling
          modules</td>
    </tr>
    <tr>
        <td>The module name for the benchmark (test) module</td>
        <td>It must match /.*:integration-tests:.*macrobenchmark/</td>
    </tr>
    <tr>
        <td>The module name for the target (integration app) module</td>
        <td>It must match /.*:integration-tests:.*macrobenchmark-target</td>
    </tr>
    <tr>
        <td>Register the modules in AffectedModuleDetector.kt</td>
        <td>It must link the modules (see docs above)</td>
    </tr>
    <tr>
        <td>Name the test class in a discoverable way</td>
        <td>Test classes should have standalone names for easy discovery in the
          web UI. E.g EmojiStartupTest instead of StartupTest.</td>
    </tr>
</table>
