/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark.json

import androidx.benchmark.Arguments
import androidx.benchmark.CpuInfo
import androidx.benchmark.DeviceInfo
import androidx.benchmark.IsolationActivity
import androidx.benchmark.MemInfo
import androidx.benchmark.PackageInfo
import androidx.benchmark.Profiler
import androidx.benchmark.ResultWriter
import com.squareup.moshi.JsonClass

/**
 * Top level json object for benchmark output for a multi-test run
 *
 * Corresponds to <packagename>BenchmarkData.json file output.
 *
 * Must be public, restrict to for usage from macrobench. We avoid @RestrictTo on these objects, and
 * rely on package-info instead, as that works for adapters as well, which fail to be detected by
 * metalava: b/331978183.
 */
@JsonClass(generateAdapter = true)
data class BenchmarkData(val context: Context, val benchmarks: List<TestResult>) {
    /** Device & OS information */
    @JsonClass(generateAdapter = true)
    data class Context(
        val build: Build,
        val cpuCoreCount: Int,
        @Suppress("GetterSetterNames") // 1.0 JSON compat
        @get:Suppress("GetterSetterNames") // 1.0 JSON compat
        val cpuLocked: Boolean,
        val cpuMaxFreqHz: Long,
        val memTotalBytes: Long,
        @Suppress("GetterSetterNames") // 1.0 JSON compat
        @get:Suppress("GetterSetterNames") // 1.0 JSON compat
        val sustainedPerformanceModeEnabled: Boolean,
        val artMainlineVersion: Long, // -1 if not found
        val osCodenameAbbreviated: String,
        val compilationMode: String,
        // additional data that can be passed from instrumentation arguments and copied into
        // the json output.
        val payload: Map<String, String> = emptyMap() // need default value for backwards compat
        // Note: Convention is to add new entries at bottom
    ) {
        /** Default constructor populates with current run state */
        constructor() :
            this(
                build = Build(),
                cpuCoreCount = CpuInfo.coreDirs.size,
                cpuLocked = CpuInfo.locked,
                cpuMaxFreqHz = CpuInfo.maxFreqHz,
                memTotalBytes = MemInfo.memTotalBytes,
                sustainedPerformanceModeEnabled = IsolationActivity.sustainedPerformanceModeInUse,
                artMainlineVersion = DeviceInfo.artMainlineVersion,
                osCodenameAbbreviated =
                    if (android.os.Build.VERSION.CODENAME != "REL") {
                            // non-release build, use codename
                            android.os.Build.VERSION.CODENAME
                        } else {
                            // release build, use start of build ID
                            android.os.Build.ID
                        }
                        .substring(0, 1),
                compilationMode = PackageInfo.compilationMode,
                payload = Arguments.payload
            )

        /**
         * Device & OS information, corresponds to `android.os.Build`
         *
         * Anything that doesn't correspond exactly to `android.os.Build` should be in context
         * instead
         */
        @JsonClass(generateAdapter = true)
        data class Build(
            val brand: String,
            val device: String,
            val fingerprint: String,
            val id: String,
            val model: String,
            val type: String,
            val version: Version
            // Note: Convention is alphabetical
        ) {
            /** Default constructor which populates values from `android.os.BUILD` */
            constructor() :
                this(
                    brand = android.os.Build.BRAND,
                    device = android.os.Build.DEVICE,
                    fingerprint = android.os.Build.FINGERPRINT,
                    id = android.os.Build.ID,
                    model = android.os.Build.MODEL,
                    type = android.os.Build.TYPE,
                    version =
                        Version(
                            codename = android.os.Build.VERSION.CODENAME,
                            sdk = android.os.Build.VERSION.SDK_INT,
                        ),
                )

            @JsonClass(generateAdapter = true)
            data class Version(
                val codename: String,
                val sdk: Int,
            )
        }
    }

    /**
     * Measurements corresponding to a single test's invocation.
     *
     * Note that one parameterized test in code can produce more than one test result.
     */
    @JsonClass(generateAdapter = true)
    data class TestResult(
        val name: String,
        val params: Map<String, String>,
        val className: String,
        @Suppress("MethodNameUnits") @get:Suppress("MethodNameUnits") val totalRunTimeNs: Long,
        val metrics: Map<String, SingleMetricResult>,
        val sampledMetrics: Map<String, SampledMetricResult>,
        val warmupIterations: Int?,
        val repeatIterations: Int?,
        val thermalThrottleSleepSeconds: Long?,
        val profilerOutputs: List<ProfilerOutput>?,
    ) {
        init {
            profilerOutputs?.let { profilerOutput ->
                val labels = profilerOutput.map { it.label }
                require(labels.toSet().size == profilerOutput.size) {
                    "Each profilerOutput must have a distinct label. Labels seen: " +
                        labels.joinToString()
                }
            }
        }

        constructor(
            name: String,
            className: String,
            totalRunTimeNs: Long,
            metrics: List<androidx.benchmark.MetricResult>,
            warmupIterations: Int,
            repeatIterations: Int,
            thermalThrottleSleepSeconds: Long,
            profilerOutputs: List<ProfilerOutput>?
        ) : this(
            name = name,
            params = ResultWriter.getParams(name),
            className = className,
            totalRunTimeNs = totalRunTimeNs,
            metrics =
                metrics
                    .filter {
                        it.iterationData == null // single metrics only
                    }
                    .associate { it.name to SingleMetricResult(it) },
            sampledMetrics =
                metrics
                    .filter {
                        it.iterationData != null // single metrics only
                    }
                    .associate { it.name to SampledMetricResult(it) },
            warmupIterations = warmupIterations,
            repeatIterations = repeatIterations,
            thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
            profilerOutputs = profilerOutputs,
        )

        @JsonClass(generateAdapter = true)
        data class ProfilerOutput(
            /**
             * Type of trace.
             *
             * Note that multiple data formats may use the same type here, like simpleperf vs art
             * stack sampling traces.
             *
             * This isn't meant to be a specific data format, but more conceptual category.
             */
            val type: Type,
            /**
             * User facing label for the profiler output.
             *
             * If more than one profiler output has the same type, this label gives context
             * explaining the distinction.
             */
            val label: String,
            /** Filename of trace file. */
            val filename: String
        ) {
            constructor(
                profilerResult: Profiler.ResultFile
            ) : this(
                type = profilerResult.type,
                label = profilerResult.label,
                filename = profilerResult.outputRelativePath,
            )

            enum class Type {
                MethodTrace,
                PerfettoTrace,
                StackSamplingTrace
            }
        }

        sealed class MetricResult

        @JsonClass(generateAdapter = true)
        data class SingleMetricResult(
            val minimum: Double,
            val maximum: Double,
            val median: Double,
            val runs: List<Double>
        ) : MetricResult() {
            constructor(
                metricResult: androidx.benchmark.MetricResult
            ) : this(
                minimum = metricResult.min,
                maximum = metricResult.max,
                median = metricResult.median,
                runs = metricResult.data
            )
        }

        @JsonClass(generateAdapter = true)
        data class SampledMetricResult(
            @Suppress("PropertyName") val P50: Double,
            @Suppress("PropertyName") val P90: Double,
            @Suppress("PropertyName") val P95: Double,
            @Suppress("PropertyName") val P99: Double,
            val runs: List<List<Double>>
        ) : MetricResult() {
            constructor(
                metricResult: androidx.benchmark.MetricResult
            ) : this(
                P50 = metricResult.p50,
                P90 = metricResult.p90,
                P95 = metricResult.p95,
                P99 = metricResult.p99,
                runs = metricResult.iterationData!!
            )
        }
    }
}
