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
public data class BenchmarkData(
    public val context: Context,
    public val benchmarks: List<TestResult>,
) {
    /** Device & OS information */
    @JsonClass(generateAdapter = true)
    public data class Context(
        public val build: Build,
        public val cpuCoreCount: Int,
        @Suppress("GetterSetterNames") // 1.0 JSON compat
        @get:Suppress("GetterSetterNames") // 1.0 JSON compat
        public val cpuLocked: Boolean,
        public val cpuMaxFreqHz: Long,
        public val memTotalBytes: Long,
        @Suppress("GetterSetterNames") // 1.0 JSON compat
        @get:Suppress("GetterSetterNames") // 1.0 JSON compat
        public val sustainedPerformanceModeEnabled: Boolean,
        public val artMainlineVersion: Long, // -1 if not found
        public val osCodenameAbbreviated: String,
        public val compilationMode: String,
        // additional data that can be passed from instrumentation arguments and copied into
        // the json output.
        public val payload: Map<String, String> =
            emptyMap(), // need default value for backwards compat
        // Note: Convention is to add new entries at bottom
    ) {
        /** Default constructor populates with current run state */
        public constructor() :
            this(
                build = Build(),
                cpuCoreCount = CpuInfo.coreDirs.size,
                cpuLocked = CpuInfo.locked,
                cpuMaxFreqHz = CpuInfo.maxFreqHz,
                memTotalBytes = MemInfo.memTotalBytes,
                sustainedPerformanceModeEnabled = IsolationActivity.sustainedPerformanceModeInUse,
                artMainlineVersion = DeviceInfo.artMainlineVersion,
                osCodenameAbbreviated =
                    if (
                        android.os.Build.VERSION.SDK_INT >= 35 &&
                            android.os.Build.VERSION.CODENAME == "REL"
                    ) {
                        "REL" // OS doesn't support codename letters anymore
                    } else {
                        if (android.os.Build.VERSION.CODENAME != "REL") {
                                // non-release build, use codename
                                android.os.Build.VERSION.CODENAME
                            } else {
                                // release build, use start of build ID
                                android.os.Build.ID
                            }
                            .substring(0, 1)
                    },
                compilationMode = PackageInfo.compilationMode,
                payload = Arguments.payload,
            )

        /**
         * Device & OS information, corresponds to `android.os.Build`
         *
         * Anything that doesn't correspond exactly to `android.os.Build` should be in context
         * instead
         */
        @JsonClass(generateAdapter = true)
        public data class Build(
            public val brand: String,
            public val device: String,
            public val fingerprint: String,
            public val id: String,
            public val model: String,
            public val type: String,
            public val version: Version,
            // Note: Convention is alphabetical
        ) {
            /** Default constructor which populates values from `android.os.BUILD` */
            public constructor() :
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
            public data class Version(val codename: String, val sdk: Int)
        }
    }

    /**
     * Measurements corresponding to a single test's invocation.
     *
     * Note that one parameterized test in code can produce more than one test result.
     */
    @JsonClass(generateAdapter = true)
    public data class TestResult(
        public val name: String,
        public val params: Map<String, String>,
        public val className: String,
        @Suppress("MethodNameUnits")
        @get:Suppress("MethodNameUnits")
        public val totalRunTimeNs: Long,
        public val metrics: Map<String, SingleMetricResult>,
        public val sampledMetrics: Map<String, SampledMetricResult>,
        public val warmupIterations: Int?,
        public val repeatIterations: Int?,
        public val thermalThrottleSleepSeconds: Long?,
        public val profilerOutputs: List<ProfilerOutput>?,
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

        public constructor(
            name: String,
            className: String,
            totalRunTimeNs: Long,
            metrics: List<androidx.benchmark.MetricResult>,
            warmupIterations: Int,
            repeatIterations: Int,
            thermalThrottleSleepSeconds: Long,
            profilerOutputs: List<ProfilerOutput>?,
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
        public data class ProfilerOutput(
            /**
             * Type of trace.
             *
             * Note that multiple data formats may use the same type here, like simpleperf vs art
             * stack sampling traces.
             *
             * This isn't meant to be a specific data format, but more conceptual category.
             */
            public val type: Type,
            /**
             * User facing label for the profiler output.
             *
             * If more than one profiler output has the same type, this label gives context
             * explaining the distinction.
             */
            public val label: String,
            /** Filename of trace file. */
            public val filename: String,
        ) {
            public constructor(
                profilerResult: Profiler.ResultFile
            ) : this(
                type = profilerResult.type,
                label = profilerResult.label,
                filename = profilerResult.outputRelativePath,
            )

            public enum class Type {
                MethodTrace,
                PerfettoTrace,
                StackSamplingTrace,
            }
        }

        public sealed class MetricResult

        @JsonClass(generateAdapter = true)
        public data class SingleMetricResult(
            public val minimum: Double,
            public val maximum: Double,
            public val median: Double,
            public val coefficientOfVariation: Double,
            public val runs: List<Double>,
        ) : MetricResult() {
            public constructor(
                metricResult: androidx.benchmark.MetricResult
            ) : this(
                minimum = metricResult.min,
                maximum = metricResult.max,
                median = metricResult.median,
                coefficientOfVariation = metricResult.coefficientOfVariation,
                runs = metricResult.data,
            )
        }

        @JsonClass(generateAdapter = true)
        public data class SampledMetricResult(
            @Suppress("PropertyName") public val P50: Double,
            @Suppress("PropertyName") public val P90: Double,
            @Suppress("PropertyName") public val P95: Double,
            @Suppress("PropertyName") public val P99: Double,
            public val runs: List<List<Double>>,
        ) : MetricResult() {
            public constructor(
                metricResult: androidx.benchmark.MetricResult
            ) : this(
                P50 = metricResult.p50,
                P90 = metricResult.p90,
                P95 = metricResult.p95,
                P99 = metricResult.p99,
                runs = metricResult.iterationData!!,
            )
        }
    }
}
