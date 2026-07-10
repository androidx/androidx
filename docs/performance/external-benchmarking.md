# Running AndroidX Benchmarks outside of AndroidX

[TOC]

# Running AndroidX Benchmarks outside of AndroidX

AndroidX-internal benchmarks can be a useful suite to test runtime, platform and
system performance/changes.

For this reason, this doc explains how to use these benchmarks outside of the
context of the AndroidX repository as APKs, and invoking them with `am
instrument`, as you would any standard test APK.

Note that AndroidX internal benchmarks are *not* a published suite with stable
releases - these benchmarks are updated continuously alongside the libraries
that use them.

To see more about writing and modifying benchmarks in the AndroidX repository,
and running them in Studio, see [this page](benchmarking.md).

## Benchmark types

Macrobenchmarks measure high level app interactions such as startup and
scrolling, and are preferred for measuring anything other than best case (AOT)
app performance.

Microbenchmarks measure specific function calls in a tight loop, and are only
designed to measure best-case, single-threaded performace for hot code.

For more info about micro vs macro, see
[here](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview).

## Getting the APKs

### Android Build Page

Build server built APKs are available in the
[aosp-androidx-main branch](https://ci.android.com/builds/branches/aosp-androidx-main/grid?legacy=1).
This corresponds to builds from source [here](https://cs.android.com/androidx).

1.  Click the build square under the `android_device_tests` target column (hover
    over columns to see it)
2.  Click 'Artifacts' at the bottom
3.  Download `androidTest.zip`

Inside that (**Note: ~4.5GB**) zip file, you'll see roughly two dozen benchmark
APKs. Each of these will have a name containing the substring
`"benchmark-releaseAndroidTest.apk"`

These generally correspond to library groups and subgroups within AndroidX.

An example build artifacts page with an `androidTest.zip` file is
[here](https://ci.android.com/builds/submitted/14809214/androidx_device_tests/latest)
(buildId `14809214`)

### Local Build

You can locally build AndroidX modules with Gradle commands like the following:

```shell
# MACRObenchmark example - two APKs are produced
./gradlew compose:integration-tests:hero:sysui:sysui-macrobenchmark:assemble compose:integration-tests:hero:sysui:sysui-macrobenchmark-target:assemble

# microbenchmark example - one APK is produced
./gradlew compose:runtime:runtime:benchmark:assembleAndroidTest
```

You can find all of the available benchmark modules in
[settings.gradle](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:settings.gradle).
Each can be built with `<modulepath>:assemble`.

Output APKs can be found in `../../out/androidx`, for example:

```shell
# above MACRObenchmark example:
../../out/androidx/compose/integration-tests/hero/sysui/sysui-macrobenchmark/build/outputs/apk/release/sysui-macrobenchmark-release.apk
../../out/androidx/compose/integration-tests/hero/sysui/sysui-macrobenchmark-target/build/outputs/apk/release/sysui-macrobenchmark-target-release.apk

# above microbenchmark example:
../../out/androidx/compose/runtime/runtime/benchmark/build/outputs/apk/androidTest/release/benchmark-release-androidTest.apk
```

For more information, see
[Benchmarking in CI](https://developer.android.com/topic/performance/benchmarking/benchmarking-in-ci)

## MACRObenchmark {#macrobenchmark}

Macrobenchmarks require two apks, but are otherwise simpler to setup and run:

*   Clocks are unlocked (partly because this affects deadlines like frame jank,
    and partly because they're expected to have non-CPU work)
*   Compilation (verify, speed-profile, speed) is configured and performed by
    the benchmark library itself

Note while profiling is
[partly supported](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args#profiling-mode),
it is not as comprehensive as in microbenchmark, due to the multi-process nature
of macrobenchmarks.

Many benchmarks can be parameterized across
[CompilationMode](https://developer.android.com/reference/kotlin/androidx/benchmark/macro/CompilationMode)
(an abstraction over art compilation filters / OS variations), or
[StartupMode](https://developer.android.com/reference/androidx/benchmark/macro/StartupMode)
(for startup benchmarks). For startup benchmarks, COLD startups are the ones
worth running / measuring.

Macrobenchmarks always output (and derive timing from) Perfetto traces, but can
be configured to capture method traces - note that it's intrusive to measure, as
it's not run in a separate phase. Same argument is used, as below in
microbenchmarks:

```shell
-e androidx.benchmark.profiling.mode MethodTracing
```

### Running MACRObenchmarks

MACRObenchmark tests are always installed in apk pairs, one for each of the test
and target packages:

```shell
adb install -r <modulename>-macrobenchmark-release.apk
adb install -r <modulename>-macrobenchmark-target_for_<modulename>-macrobenchmark-release.apk
```

Individual tests can always be run with commands of the format:

```shell
adb shell am instrument -e "class" "<androidx.somepackage.BenchmarkClass>#<benchmarkMethod>" -w <BenchmarkPackage>/androidx.test.runner.AndroidJUnitRunner
```

You can discover the classes contained within from
[code search](https://cs.android.com/androidx), or find available classes with a
shell command:

```shell
adb shell am instrument -e log true -w <BenchmarkPackage>/androidx.test.runner.AndroidJUnitRunner
```

See examples below, note parameterization is often required, especially for
macrobenchmarks.

#### Pokedex Hero Macrobenchmarks

Install

```
adb install -r compose-integration-tests-hero-pokedex-pokedex-macrobenchmark-release.apk
adb install -r compose-integration-tests-hero-pokedex-pokedex-macrobenchmark-target_for_compose-integration-tests-hero-pokedex-pokedex-macrobenchmark-release.apk
```

Run \
Note: parameterization of compilation mode is limited, to limit CI runtimes

```shell
adb shell am instrument -e "class" "androidx.compose.integration.hero.pokedex.macrobenchmark.PokedexScrollBenchmark#scrollHomeCompose[compilation=Full,eSTS=true,eSET=true]" -w androidx.compose.integration.hero.pokedex.macrobenchmark/androidx.test.runner.AndroidJUnitRunner
```

#### Jetsnack Hero Macrobenchmarks

[JetsnackScrollBenchmark](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/hero/jetsnack/jetsnack-macrobenchmark/src/main/java/androidx/compose/integration/hero/jetsnack/macrobenchmark/JetsnackScrollBenchmark.kt)
and
[JetsnackStartupBenchmark](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/hero/jetsnack/jetsnack-macrobenchmark/src/main/java/androidx/compose/integration/hero/jetsnack/macrobenchmark/JetsnackStartupBenchmark.kt)
are higher level, more realistic scroll/startup benchmarks that are also
important.

Install:

```shell
adb install -r compose-integration-tests-hero-jetsnack-jetsnack-macrobenchmark-release.apk
adb install -r compose-integration-tests-hero-jetsnack-jetsnack-macrobenchmark-target_for_compose-integration-tests-hero-jetsnack-jetsnack-macrobenchmark-release.apk
```

Run:

```shell
adb shell am instrument -e "class" "androidx.compose.integration.hero.jetsnack.macrobenchmark.JetsnackScrollBenchmark#scrollHome[compilation=BaselineProfile]" -w androidx.compose.integration.hero.jetsnack.macrobenchmark/androidx.test.runner.AndroidJUnitRunner
```

#### Compose Minimal Macrobenchmarks

[TrivialListScrollBenchmark](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark/src/main/java/androidx/compose/integration/macrobenchmark/TrivialListScrollBenchmark.kt?q=compose%2Fintegration-tests%2Fmacrobenchmark%2Fsrc%2Fmain%2Fjava%2Fandroidx%2Fcompose%2Fintegration%2Fmacrobenchmark%2FTrivialListScrollBenchmark.kt)
and
[SmallListStartupBenchmark](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/macrobenchmark/src/main/java/androidx/compose/integration/macrobenchmark/SmallListStartupBenchmark.kt)
are good low-level starting points, since they cover the most critical building
blocks of UI display and scrolling.

Install:

```shell
adb install -r compose-integration-tests-macrobenchmark-release.apk
adb install -r compose-integration-tests-macrobenchmark-target_for_compose-integration-tests-macrobenchmark-release.apk
```

Run:

```shell
adb shell am instrument -e "class" "androidx.compose.integration.macrobenchmark.TrivialListScrollBenchmark#start[compilation=BaselineProfile]" -w androidx.compose.integration.macrobenchmark/androidx.test.runner.AndroidJUnitRunner
```

## Microbenchmark

Microbenchmarks measure code entrypoints, and are somewhat more stable than
[macrobenchmarks (see below)](#macrobenchmark), but they do not yet support
being built with R8. Support is being added to the gradle build system
currently. See
[this doc](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview)
for more info about micro vs macro.

### Running the Microbenchmark

Install the benchmark, e.g.: `adb install
compose-foundation-foundation-benchmarks.apk` Then prepare the device by locking
clocks and disabling JIT (requires adb root):

```shell
benchmark/gradle-plugin/src/main/resources/scripts/disableJit.sh
benchmark/gradle-plugin/src/main/resources/scripts/lockClocks.sh
```

These script are available
[here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:benchmark/gradle-plugin/src/main/resources/scripts),
and can be run from host or device.

Then speed compile the app, to ensure stable workload:

```shell
cmd package compile -f -m speed androidx.compose.foundation.benchmark.test
```

Note: this command as well as all general parameters can be found in the xml
file alongside the benchmark apk.

Now you can invoke a single test from the command line:

```shell
adb shell am instrument -e "class" "androidx.compose.foundation.benchmark.lazy.LazyListScrollingBenchmark\#scrollProgrammatically\_newItemComposed\[LazyColumn\]" -w androidx.compose.foundation.benchmark.test/androidx.benchmark.junit4.AndroidBenchmarkRunner
```

> NOTE: Device screen must be on, so activities can be launched to host UI.
>
> Locally, use Developer Options \> Stay Awake.
>
> If you're running with Tradefed, this can be configured for your suite:
>
> ```xml
>     <lab_preparer class="com.google.android.tradefed.targetprep.GoogleDeviceSetup">
>         <option name="disable" value="true" />
>         <option name="screen-always-on" value="on" />
>         <option name="screen-adaptive-brightness" value="off" />
>     </lab_preparer>
> ```

This will have a very basic output on the command line, but CLI text output
doesn't include Perfetto trace (captured by default), method trace (recommended,
captured in separate phase), or json output with detailed metrics, these need to
be pulled separately. See
[Benchmarking in Continuous Integration](https://developer.android.com/topic/performance/benchmarking/benchmarking-in-ci)
for more info on pulling trace/json output.

You can also modify instrumentation args to e.g. add profiling, like the
following which captures a method trace after timing measurements:

```shell
adb shell am instrument -e "class"
"androidx.compose.foundation.benchmark.lazy.LazyListScrollingBenchmark\#scrollProgrammatically\_newItemComposed\[LazyColumn\]"
-e androidx.benchmark.profiling.mode MethodTracing -w
androidx.compose.foundation.benchmark.test/androidx.benchmark.junit4.AndroidBenchmarkRunner
```

See the
[public docs for instrumentation args](https://developer.android.com/topic/performance/benchmarking/microbenchmark-instrumentation-args)
to see what can be configured.

### Where to start:

[LazyListScrollingBenchmark](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/benchmark/src/androidTest/java/androidx/compose/foundation/benchmark/lazy/LazyListScrollingBenchmark.kt)
and
[scrollProgrammattically_newItemComposed](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/benchmark/src/androidTest/java/androidx/compose/foundation/benchmark/lazy/LazyListScrollingBenchmark.kt;l=81;drc=e6d33dd5d0a60001a5784d84123b05308d35f410)
in particular is a good place to start, as it captures much of the cost of the
scrolling container in Compose (albeit with a trivial layout).

[PokedexScrollingBenchmark](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/integration-tests/hero/pokedex/pokedex-macrobenchmark/src/main/java/androidx/compose/integration/hero/pokedex/macrobenchmark/PokedexScrollBenchmark.kt)
is a higher level benchmark, and also a good place to start.

You can also pick a specific class to run - note that the entire suite will take
10s of minutes to run, and heavily parameterized classes can take many minutes
as well.
