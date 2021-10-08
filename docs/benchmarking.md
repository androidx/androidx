# Benchmarking in AndroidX

[TOC]

The public documentation at
[d.android.com/benchmark](http://d.android.com/benchmark) explains how to use
the library - this page focuses on specifics to writing libraries in the
AndroidX repo, and our continuous testing / triage process.

This page is for MICRO benchmarks measuring CPU performance of small sections of
code. If you're looking for measuring startup or jank, see the guide for
MACRObenchmarks [here](macrobenchmarking).

### Writing the benchmark

Benchmarks are just regular instrumentation tests! Just use the
[`BenchmarkRule`](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/benchmark/junit4/src/main/java/androidx/benchmark/junit4/BenchmarkRule.kt)
provided by the library:

<section class="tabs">

#### Kotlin {.new-tab}

```kotlin
@RunWith(AndroidJUnit4::class)
class ViewBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun simpleViewInflate() {
        val context = InstrumentationRegistry
                .getInstrumentation().targetContext
        val inflater = LayoutInflater.from(context)
        val root = FrameLayout(context)

        benchmarkRule.measure {
            inflater.inflate(R.layout.test_simple_view, root, false)
        }
    }
}
```

#### Java {.new-tab}

```java
@RunWith(AndroidJUnit4.class)
public class ViewBenchmark {
    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

    @Test
    public void simpleViewInflate() {
        Context context = InstrumentationRegistry
                .getInstrumentation().getTargetContext();
        final BenchmarkState state = mBenchmarkRule.getState();
        LayoutInflater inflater = LayoutInflater.from(context);
        FrameLayout root = new FrameLayout(context);

        while (state.keepRunning()) {
            inflater.inflate(R.layout.test_simple_view, root, false);
        }
    }
}
```

</section>

## Project structure

As in the public documentation, benchmarks in the AndroidX repo are test-only
library modules. Differences for AndroidX repo:

1.  Module must live in `integration-tests` group directory
1.  Module name must end with `-benchmark` in `settings.gradle`.

### I'm lazy and want to start quickly

Start by copying one of the following projects:

*   [navigation-benchmark](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/navigation/benchmark/)
*   [recyclerview-benchmark](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/recyclerview/recyclerview-benchmark/)

### Compose

Compose builds the benchmark from source, so usage matches the rest of the
AndroidX project. See existing Compose benchmark projects:

*   [Compose UI benchmarks](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/benchmark/)
*   [Compose Runtime benchmarks](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime/compose-runtime-benchmark/)

## Profiling

### Command Line

The benchmark library supports capturing profiling information - stack sampling
and method tracing - from the command line. Here's an example which runs the
`androidx.compose.material.benchmark.CheckboxesInRowsBenchmark#draw` method with
`StackSampling` profiling:

```
./gradlew compose:material:material-benchmark:cC \
    -P android.testInstrumentationRunnerArguments.androidx.benchmark.profiling.mode=StackSampling \
    -P android.testInstrumentationRunnerArguments.class=androidx.compose.material.benchmark.CheckboxesInRowsBenchmark#draw
```

The command output will tell you where to look for the file on your host
machine:

```
04:33:49 I/Benchmark: Benchmark report files generated at
/androidx-main/out/ui/ui/integration-tests/benchmark/build/outputs/connected_android_test_additional_output
```

To inspect the captured trace, open the appropriate `*.trace` file in that
directory with Android Studio, using `File > Open`.

NOTE For stack sampling, it's recommended to profile on Android Q(API 29) or
higher, as this enables the benchmark library to use
[Simpleperf](https://android.googlesource.com/platform/system/extras/+/master/simpleperf/doc/).
Simpleperf previously required a
[more complex setup process](https://issuetracker.google.com/issues/158303822) -
this has been fixed!

For more information on the `StackSampling` and `MethodTracing` profiling modes,
see the
[Studio Profiler configuration docs](https://developer.android.com/studio/profile/cpu-profiler#configurations),
specifically "Sample C/C++ Functions" (a confusing name for Simpleperf), and
Java Method Tracing.

![Sample flame chart](benchmarking_images/profiling_flame_chart.png "Sample flame chart")

NOTE Simpleperf captures stack traces from all threads, so click the test thread
in the left profiler panel, and select flame chart on the right to see just
samples from the test.

### Advanced: Studio Profiling

Profiling for allocations and simpleperf profiling requires Studio to capture.

Studio profiling tools require `debuggable=true`. First, temporarily override it
in your benchmark's `androidTest/AndroidManifest.xml`.

Next choose which profiling you want to do: Allocation, or Sampled (SimplePerf)

`ConnectedAllocation` will help you measure the allocations in a single run of a
benchmark loop, after warmup.

`ConnectedSampled` will help you capture sampled profiling, but with the more
detailed / accurate Simpleperf sampling.

Set the profiling type in your benchmark module's `build.gradle`:

```
android {
    defaultConfig {
        // Local only, don't commit this!
        testInstrumentationRunnerArgument 'androidx.benchmark.profiling.mode', 'ConnectedAllocation'
    }
}
```

Run `File > Sync Project with Gradle Files`, or sync if Studio asks you. Now any
benchmark runs in that project will permit debuggable, and pause before and
after the test, to allow you to connect a profiler and start recording, and then
stop recording.

#### Running and Profiling

After the benchmark test starts, you have about 20 seconds to connect the
profiler:

1.  Click the profiler tab at the bottom
1.  Click the plus button in the top left, `<device name>`, `<process name>`
1.  Next step depends on which you intend to capture

#### Allocations

Click the memory section, and right click the window, and select `Record
allocations`. Approximately 20 seconds later, right click again and select `Stop
recording`.
