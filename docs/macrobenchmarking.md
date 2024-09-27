# MACRObenchmarking in AndroidX

[TOC]

<!-- Copied from macrobenchmark docs -->

<table>
    <tr>
      <td><strong>Macrobenchmark</strong></td>
      <td><strong>Benchmark</strong></td>
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
        <td>Configure compilation with <a href="https://developer.android.com/reference/androidx/benchmark/macro/CompilationMode">CompilationMode</a></td>
        <td>Always fully AOT (<code>speed</code>) compiled.</td>
    </tr>
    <tr>
        <td>Min API 23</td>
        <td>Min API 19</td>
    </tr>
    <tr>
        <td>Relatively lower stability measurements</td>
        <td>Higher stability measurements</td>
    </tr>
</table>

The
[public documentation](https://developer.android.com/studio/profile/macrobenchmark)
for macrobenchmark explains how to use the library. This page focuses on
specifics to writing library macrobenchmarks in the AndroidX repo. If you're
looking for measuring CPU perf of individual functions, see the guide for
MICRObenchmarks [here](/docs/benchmarking.md).

### Writing the benchmark

Benchmarks are just regular instrumentation tests! Just use the
[`MacrobenchmarkRule`](https://developer.android.com/reference/kotlin/androidx/benchmark/macro/junit4/MacrobenchmarkRule)
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
        iterations = 10
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
                /* iterations = */ 10,
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
comprised of an app, and a separate macrobenchmark test module. In the AndroidX
repository, there are additional requirements:

1.  Macrobenchmark test module path in `settings.gradle` **must** end with
    `macrobenchmark` to run in CI.

1.  Macrobenchmark target module path in `settings.gradle` **should** end with
    `macrobenchmark-target` to follow convention.

1.  Macrobenchmark modules **must** use project dependencies where available (so
    `implementation(project(":activity:activity-ktx"))` rather than
    `implementation("androidx.activity:activity-ktx:1.5.0")`). This prevents
    accidentally testing against out-of-date versions, and increases coverage of
    lower level libraries.

1.  Each library group **must** declare its own in-group macrobenchmark test and
    app module, with out using these modules for anything else (e.g. samples).
    We want to be intentional about which changes affect measurements. More than
    one of either is allowed, which is sometimes necessary to compare different
    startup behaviors, see e.g.
    `:emoji2:integration-tests:init-<disabled/enabled>-macrobenchmark-target`.
    Note that comparing multiple app variants are not currently supported by CI.

Compose Macrobenchmark Examples:

*   [`:compose:integration-tests:macrobenchmark-target`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark-target/)

*   [`:compose:integration-tests:macrobenchmark`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark/)

Note: Compose macrobenchmarks are ideally duplicated with View system
counterparts, defined in `:benchmark:integration-tests:macrobenchmark-target`.
This is how we compare performance of the two systems.

### Setup checklist

<table>
    <tr>
      <td><strong>Did you setup...</strong></td>
      <td><strong>Required setup</strong></td>
    </tr>
    <tr>
        <td>Two modules in <code>settings.gradle</code></td>
        <td>Both the macrobenchmark and target must be added for your group</td>
    </tr>
    <tr>
        <td>The module name for the benchmark (<code>com.android.test</code>) module</td>
        <td>Must end with <code>macrobenchmark</code></td>
    </tr>
    <tr>
        <td>The module name for the app (<code>com.android.app</code>) module</td>
        <td>Must end with <code>macrobenchmark-target</code></td>
    </tr>
    <tr>
        <td>Name the test class in a discoverable way</td>
        <td>Test classes should have standalone names for easy discovery in the
          web UI. E.g EmojiStartupTest instead of StartupTest.</td>
    </tr>
</table>
