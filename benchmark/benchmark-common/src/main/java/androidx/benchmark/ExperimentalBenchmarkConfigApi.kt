/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.benchmark

import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.perfetto.PerfettoConfig

/**
 * Annotates declarations that are considered experimental within the Benchmark API, and are likely
 * to change before becoming stable. Using experimental features can potentially break your code if
 * the design or behavior changes.
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalBenchmarkConfigApi

/**
 * Experimental configuration options for a benchmark.
 *
 * Currently used to override the default [PerfettoConfig], enable Startup
 * [Insights][androidx.benchmark.traceprocessor.Insight]s, or configure memory heap profiling.
 */
@ExperimentalBenchmarkConfigApi
public class ExperimentalConfig(
    /** The PerfettoConfig for the benchmark - `null` to use the default config. */
    public val perfettoConfig: PerfettoConfig? = null,

    /** The StartupInsightsConfig for the benchmark - `null` to not enable insights reporting. */
    public val startupInsightsConfig: StartupInsightsConfig? = null,

    /** The MemoryProfilingConfig for the benchmark - `null` to disable memory profiling. */
    public val memoryProfilingConfig: MemoryProfilingConfig? = null,
)

/**
 * Configuration for memory heap profiling during macrobenchmark execution.
 *
 * Enables Perfetto `heapprofd` data source to sample memory allocations in target applications on
 * Android Q (API 29)+.
 *
 * When [MemoryProfilingConfig] is provided in [ExperimentalConfig], memory profiling runs during an
 * additional benchmark pass after all regular measurement iterations complete. Regular measurement
 * iterations are not profiled to avoid measurement overhead and memory footprint distortion.
 *
 * The recorded trace from the memory profiling pass is saved with a `-memoryProfiling` suffix (e.g.
 * `${uniqueName}_iter000-memoryProfiling.perfetto-trace`) and contains embedded `pprof` profile
 * data. Traces can be viewed directly in [Perfetto UI](https://ui.perfetto.dev/) or converted to
 * standard `pprof` format using the Perfetto
 * [traceconv tool](https://perfetto.dev/docs/analysis/traceconv).
 *
 * For more details, see:
 * - [Perfetto ART Allocation
 *   Profiling](https://perfetto.dev/docs/data-sources/native-heap-profiler#art-allocation-profiling)
 * - [Perfetto Native Heap Profiler](https://perfetto.dev/docs/data-sources/native-heap-profiler)
 * - [Perfetto Traceconv Tool](https://perfetto.dev/docs/analysis/traceconv)
 */
@RequiresApi(29)
@ExperimentalBenchmarkConfigApi
public class MemoryProfilingConfig(
    /**
     * Set to true to sample ART Java/Kotlin heap allocations (`com.android.art`), tracking object
     * allocations made by the Android Runtime.
     *
     * For details on ART heap profiling, see
     * [Perfetto ART Allocation Profiling](https://perfetto.dev/docs/data-sources/native-heap-profiler#art-allocation-profiling).
     */
    public val isSampleArtHeapEnabled: Boolean,

    /**
     * Set to true to sample native heap allocations (`libc.malloc`), tracking C/C++ memory
     * allocations (e.g. `malloc`, `calloc`, `new`) made by native libraries.
     *
     * For details on native heap profiling, see
     * [Perfetto Native Heap Profiler](https://perfetto.dev/docs/data-sources/native-heap-profiler).
     */
    public val isSampleNativeHeapEnabled: Boolean,
) {
    init {
        // At least one memory sampling source must be enabled for memory profiling to capture data.
        require(isSampleArtHeapEnabled || isSampleNativeHeapEnabled) {
            "At least one of isSampleArtHeapEnabled or isSampleNativeHeapEnabled must be enabled."
        }
    }
}

/**
 * Configuration for Startup Insights.
 *
 * By passing this object to a `MacrobenchmarkRule`, you can enable reporting of Startup
 * [Insights][androidx.benchmark.traceprocessor.Insight]s - problems patterns discovered during your
 * application startup.
 *
 * c, as well in the Benchmark JSON output file.
 */
@ExperimentalBenchmarkConfigApi
public class StartupInsightsConfig(
    /** Set to true to enable reporting of Startup Insights. */
    public val isEnabled: Boolean
) {
    /**
     * Base URL for linking to more information about specific startup reasons. This URL should
     * accept a reason ID as a direct suffix. For example, a base URL of
     * `https://developer.android.com/[...]/slow-start-reason#` could be combined with a reason ID
     * of `MAIN_THREAD_MONITOR_CONTENTION` to create a complete URL like:
     * `https://developer.android.com/[...]/slow-start-reason#MAIN_THREAD_MONITOR_CONTENTION`
     */
    public val reasonHelpUrlBase: String? = Arguments.startupInsightsHelpUrlBase
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get
}

/**
 * Configuration for in-process tracing.
 *
 * By passing this object in `MacrobenchmarkRule` you can enable capture of in-process traces.
 */
@ExperimentalBenchmarkConfigApi
public enum class InProcessTracingMode {
    /**
     * Enable in-process tracing, which requires the target application to include the
     * `androidx.tracing:tracing-wire` dependency.
     *
     * If `androidx.tracing:tracing-wire` is not present in the target application, an exception is
     * thrown during the benchmark.
     */
    Require,
    /** Enable in-process tracing if the app depends on `androidx.tracing:tracing-wire`. */
    UseIfAvailable,
    /** Do not include in-process traces. */
    Disable,
}
