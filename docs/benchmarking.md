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

The benchmark library supports capturing profiling information - sampled and
method - from the command line. Here's an example which runs the
`androidx.ui.benchmark.test.CheckboxesInRowsBenchmark#draw` method with
`MethodSampling` profiling:

```
./gradlew compose:integ:bench:cC \
    -P android.testInstrumentationRunnerArguments.androidx.benchmark.profiling.mode=MethodSampling \
    -P android.testInstrumentationRunnerArguments.class=androidx.ui.benchmark.test.CheckboxesInRowsBenchmark#draw
```

The command output will tell you where to look for the file on your host
machine:

```
04:33:49 I/Benchmark: Benchmark report files generated at
/androidx-main/out/ui/ui/integration-tests/benchmark/build/outputs/connected_android_test_additional_output
```

To inspect the captured trace, open the appropriate `*.trace` file in that
directory with Android Studio, using `File > Open`.

For more information on the `MethodSampling` and `MethodTracing` profiling
modes, see the
[Studio Profiler configuration docs](https://developer.android.com/studio/profile/cpu-profiler#configurations),
specifically Java Sampled Profiling, and Java Method Tracing.

![Sample flame chart](benchmarking_images/profiling_flame_chart.png "Sample flame chart")

### Advanced: Simpleperf Method Sampling

[Simpleperf](https://android.googlesource.com/platform/system/extras/+/master/simpleperf/doc/)
offers more accurate profiling for apps than standard method sampling, due to
lower overhead (as well as C++ profiling support). Simpleperf support will be
simplified and improved over time.

[Simpleperf app profiling docs](https://android.googlesource.com/platform/system/extras/+/master/simpleperf/doc/android_application_profiling.md).

#### Device

Get an API 29+ device. The rest of this section is about *why* those constraints
exist, skip if not interested.

Simpleperf has restrictions about where it can be used - Jetpack Benchmark will
only support API 29+ for now, due to
[platform/simpleperf constraints](https://android.googlesource.com/platform/system/extras/+/master/simpleperf/doc/android_application_profiling.md#prepare-an-android-application)
(see last subsection titled "If you want to profile Java code"). Summary is:

-   <=23 (M): Unsupported for Java code.

-   24-25 (N): Requires compiled Java code. We haven't investigated support.

-   26 (O): Requires compiled Java code, and wrapper script. We haven't
    investigated support.

-   27 (O.1): Can profile all Java code, but requires `userdebug`/rooted device

-   28 (P): Can profile all Java code, requires debuggable (or
    `userdebug`/rooted device, but this appears to not be supported by scripts
    currently)

-   \>=29 (Q): Can profile all Java code, requires profileable or debuggable (or
    `userdebug`/rooted device)

We aren't planning to support profiling debuggable APK builds, since they're
misleading for profiling.

#### Initial setup

Currently, we rely on Python scripts built by the simpleperf team. We can
eventually build this into the benchmark library / gradle plugin. Download the
scripts from AOSP:

```
# copying to somewhere outside of the androidx repo
git clone https://android.googlesource.com/platform/system/extras ~/simpleperf
```

Next configure your path to ensure the ADB that the scripts will use matches the
androidx tools:

```
export PATH=$PATH:<path/to/androidx>/prebuilts/fullsdk-<linux or darwin>/platform-tools
```

Now, setup your device for simpleperf:

```
~/simpleperf/simpleperf/scripts/api_profiler.py prepare --max-sample-rate 10000000
```

#### Build and Run, Option 1: Studio (slightly recommended)

Running from Studio is simpler, since you don't have to manually install and run
the APKs, avoiding Gradle.

Add the following to the benchmark module's build.gradle:

```
android {
    defaultConfig {
        // DO NOT COMMIT!!
        testInstrumentationRunnerArgument 'androidx.benchmark.profiling.mode', 'MethodSamplingSimpleperf'
        // Optional: Control freq / duration.
        testInstrumentationRunnerArgument 'androidx.benchmark.profiler.sampleFrequency', '1000000'
        testInstrumentationRunnerArgument 'androidx.benchmark.profiler.sampleDurationSeconds', '5'
    }
}
```

And run the test or tests you'd like to measure from within Studio.

#### Build and Run, Option 2: Command Line

**Note - this will be significantly simplified in the future**

Since we're not using AGP to pull the files yet, we can't invoke the benchmark
through Gradle, because Gradle uninstalls after each test run. Instead, let's
just build and run manually:

```
./gradlew compose:integration-tests:benchmark:assembleReleaseAndroidTest

adb install -r ../../../out/ui/compose/integration-tests/benchmark/build/outputs/apk/androidTest/release/benchmark-release-androidTest.apk

# run the test (can copy this line from Studio console, when running a benchmark)
adb shell am instrument -w -m --no-window-animation -e androidx.benchmark.profiling.mode MethodSamplingSimpleperf -e debug false -e class 'androidx.ui.benchmark.test.CheckboxesInRowsBenchmark#toggleCheckbox_draw' androidx.ui.benchmark.test/androidx.benchmark.junit4.AndroidBenchmarkRunner
```

#### Pull and open the trace

```
# move the files to host
# (Note: removes files from device)
~/simpleperf/simpleperf/scripts/api_profiler.py collect -p androidx.ui.benchmark.test -o ~/simpleperf/results

# create/open the HTML report
~/simpleperf/simpleperf/scripts/report_html.py -i ~/simpleperf/results/CheckboxesInRowsBenchmark_toggleCheckbox_draw\[1\].data
```

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
