/*
 * Copyright (C) 2023 The Android Open Source Project
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

/**
 * Experimental config object for microbenchmarks for defining custom metrics, tracing behavior,
 * and profiling, which overrides options set in
 * [instrumentation arguments](https://developer.android.com/topic/performance/benchmarking/microbenchmark-instrumentation-args).
 */
@ExperimentalBenchmarkConfigApi
class MicrobenchmarkConfig constructor(
    /**
     * Timing metrics for primary phase, post-warmup
     *
     * Defaults to [TimeCapture].
     */
    val metrics: List<MetricCapture> = listOf(TimeCapture()),

    /**
     * Set to true to enable capture of `trace("foo") {}` blocks in the output Perfetto trace.
     *
     * Defaults to false to minimize interference.
     */
    @get:JvmName("shouldEnableTraceAppTag")
    val shouldEnableTraceAppTag: Boolean = false,

    /**
     * Set to true to enable capture of tracing-perfetto trace events, such as in Compose
     * composition tracing.
     *
     * Defaults to false to minimize interference.
     */
    @get:JvmName("shouldEnablePerfettoSdkTracing")
    val shouldEnablePerfettoSdkTracing: Boolean = false,

    /**
     * Optional profiler to be used after the primary timing phase.
     */
    val profiler: ProfilerConfig? = null,
)
