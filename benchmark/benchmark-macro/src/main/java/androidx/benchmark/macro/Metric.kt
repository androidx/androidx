/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Shell
import androidx.benchmark.macro.BatteryCharge.hasMinimumCharge
import androidx.benchmark.macro.PowerMetric.Type
import androidx.benchmark.macro.PowerRail.hasMetrics
import androidx.benchmark.macro.TraceSectionMetric.Mode
import androidx.benchmark.macro.perfetto.BatteryDischargeQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric
import androidx.benchmark.macro.perfetto.FrameTimingQuery.getFrameSubMetrics
import androidx.benchmark.macro.perfetto.MemoryCountersQuery
import androidx.benchmark.macro.perfetto.MemoryUsageQuery
import androidx.benchmark.macro.perfetto.PowerQuery
import androidx.benchmark.macro.perfetto.StartupTimingQuery
import androidx.benchmark.macro.perfetto.camelCase
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.Slice

/** Metric interface. */
sealed class Metric {

    internal abstract fun configure(packageName: String)

    internal abstract fun start()

    internal abstract fun stop()

    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a general config object
     *   coming into [start].
     */
    internal abstract fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement>

    @ExperimentalMetricApi
    data class CaptureInfo(
        val apiLevel: Int,
        val targetPackageName: String,
        val testPackageName: String,
        val startupMode: StartupMode?
    )

    /**
     * Represents a Metric's measurement of a single iteration.
     *
     * To validate results in tests, use [assertEqualMeasurements]
     */
    @ExperimentalMetricApi
    data class Measurement
    internal constructor(
        /**
         * Unique name of the metric, should be camel case with abbreviated suffix, e.g.
         * `startTimeNs`
         */
        val name: String,
        /**
         * Measurement values captured by the metric, length constraints defined by
         * [requireSingleValue].
         */
        val data: List<Double>,
        /**
         * True if the [data] param is a single value per measurement, false if it contains an
         * arbitrary number of samples.
         */
        val requireSingleValue: Boolean
    ) {

        /**
         * Represents a measurement with a single value captured per iteration.
         *
         * For example, in a startup Macrobenchmark, [StartupTimingMetric] returns a single
         * measurement for `timeToInitialDisplayMs`.
         */
        constructor(
            name: String,
            data: Double
        ) : this(name, listOf(data), requireSingleValue = true)

        /**
         * Represents a measurement with a value sampled an arbitrary number of times per iteration.
         *
         * For example, in a jank Macrobenchmark, [FrameTimingMetric] can return multiple
         * measurements for `frameOverrunMs` - one for each observed frame.
         *
         * When measurements are merged across multiple iterations, percentiles are extracted from
         * the total pool of samples: P50, P90, P95, and P99.
         */
        constructor(
            name: String,
            dataSamples: List<Double>
        ) : this(name, dataSamples, requireSingleValue = false)

        init {
            require(!requireSingleValue || data.size == 1) {
                "Metric.Measurement must be in multi-sample mode, or include only one data item"
            }
        }
    }
}

private fun Long.nsToDoubleMs(): Double = this / 1_000_000.0

/**
 * Metric which captures timing information from frames produced by a benchmark, such as a scrolling
 * or animation benchmark.
 *
 * This outputs the following measurements:
 * * `frameOverrunMs` (Requires API 31) - How much time a given frame missed its deadline by.
 *   Positive numbers indicate a dropped frame and visible jank / stutter, negative numbers indicate
 *   how much faster than the deadline a frame was.
 * * `frameDurationCpuMs` - How much time the frame took to be produced on the CPU - on both the UI
 *   Thread, and RenderThread. Note that this doesn't account for time before the frame started
 *   (before Choreographer#doFrame), as that data isn't available in traces prior to API 31.
 * * `frameCount` - How many total frames were produced. This is a secondary metric which can be
 *   used to understand *why* the above metrics changed. For example, when removing unneeded frames
 *   that were incorrectly invalidated to save power, `frameOverrunMs` and `frameDurationCpuMs` will
 *   often get worse, as the removed frames were trivial. Checking `frameCount` can be a useful
 *   indicator in such cases.
 *
 * Generally, prefer tracking and detecting regressions with `frameOverrunMs` when it is available,
 * as it is the more complete data, and accounts for modern devices (including higher, variable
 * framerate rendering) more naturally.
 */
@Suppress("CanSealedSubClassBeObject")
class FrameTimingMetric : Metric() {
    override fun configure(packageName: String) {}

    override fun start() {}

    override fun stop() {}

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val frameData =
            FrameTimingQuery.getFrameData(
                session = traceSession,
                captureApiLevel = captureInfo.apiLevel,
                packageName = captureInfo.targetPackageName
            )
        return frameData
            .getFrameSubMetrics(captureInfo.apiLevel)
            .filterKeys { it == SubMetric.FrameDurationCpuNs || it == SubMetric.FrameOverrunNs }
            .map {
                Measurement(
                    name =
                        if (it.key == SubMetric.FrameDurationCpuNs) {
                            "frameDurationCpuMs"
                        } else {
                            "frameOverrunMs"
                        },
                    dataSamples = it.value.map { timeNs -> timeNs.nsToDoubleMs() }
                )
            } + listOf(Measurement("frameCount", frameData.size.toDouble()))
    }
}

/**
 * Version of FrameTimingMetric based on 'dumpsys gfxinfo' instead of trace data.
 *
 * Added for experimentation in contrast to FrameTimingMetric, as the platform accounting of frame
 * drops currently behaves differently from that of FrameTimingMetric.
 *
 * Likely to be removed when differences in jank behavior are reconciled between this class, and
 * [FrameTimingMetric].
 *
 * Note that output metrics do not match perfectly to FrameTimingMetric, as individual frame times
 * are not available, only high level, millisecond-precision statistics.
 */
@ExperimentalMetricApi
class FrameTimingGfxInfoMetric : Metric() {
    private lateinit var packageName: String
    private val helper = JankCollectionHelper()
    private var metrics = mutableMapOf<String, Double>()

    override fun configure(packageName: String) {
        this.packageName = packageName
        helper.addTrackedPackages(packageName)
    }

    override fun start() {
        try {
            helper.startCollecting()
        } catch (exception: RuntimeException) {
            // Ignore the exception that might result from trying to clear GfxInfo
            // The current implementation of JankCollectionHelper throws a RuntimeException
            // when that happens. This is safe to ignore because the app being benchmarked
            // is not showing any UI when this happens typically.

            // Once the MacroBenchmarkRule has the ability to setup the app in the right state via
            // a designated setup block, we can get rid of this.
            if (!Shell.isPackageAlive(packageName)) {
                error(exception.message ?: "Assertion error, $packageName not running")
            }
        }
    }

    override fun stop() {
        helper.stopCollecting()

        // save metrics on stop to attempt to more closely match perfetto based metrics
        metrics.clear()
        metrics.putAll(helper.metrics)
    }

    /**
     * Used to convert keys from platform to JSON format.
     *
     * This both converts `snake_case_format` to `camelCaseFormat`, and renames for clarity.
     *
     * Note that these will still output to inst results in snake_case, with `MetricNameUtils` via
     * [androidx.benchmark.MetricResult.putInBundle].
     */
    private val keyRenameMap =
        mapOf(
            "frame_render_time_percentile_50" to "gfxFrameTime50thPercentileMs",
            "frame_render_time_percentile_90" to "gfxFrameTime90thPercentileMs",
            "frame_render_time_percentile_95" to "gfxFrameTime95thPercentileMs",
            "frame_render_time_percentile_99" to "gfxFrameTime99thPercentileMs",
            "gpu_frame_render_time_percentile_50" to "gpuFrameTime50thPercentileMs",
            "gpu_frame_render_time_percentile_90" to "gpuFrameTime90thPercentileMs",
            "gpu_frame_render_time_percentile_95" to "gpuFrameTime95thPercentileMs",
            "gpu_frame_render_time_percentile_99" to "gpuFrameTime99thPercentileMs",
            "missed_vsync" to "vsyncMissedFrameCount",
            "deadline_missed" to "deadlineMissedFrameCount",
            "deadline_missed_legacy" to "deadlineMissedFrameCountLegacy",
            "janky_frames_count" to "jankyFrameCount",
            "janky_frames_legacy_count" to "jankyFrameCountLegacy",
            "high_input_latency" to "highInputLatencyFrameCount",
            "slow_ui_thread" to "slowUiThreadFrameCount",
            "slow_bmp_upload" to "slowBitmapUploadFrameCount",
            "slow_issue_draw_cmds" to "slowIssueDrawCommandsFrameCount",
            "total_frames" to "gfxFrameTotalCount",
            "janky_frames_percent" to "gfxFrameJankPercent",
            "janky_frames_legacy_percent" to "jankyFramePercentLegacy"
        )

    /** Filters output to only frameTimeXXthPercentileMs and totalFrameCount */
    private val keyAllowList =
        setOf(
            "gfxFrameTime50thPercentileMs",
            "gfxFrameTime90thPercentileMs",
            "gfxFrameTime95thPercentileMs",
            "gfxFrameTime99thPercentileMs",
            "gfxFrameTotalCount",
            "gfxFrameJankPercent",
        )

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        return metrics
            .map {
                val prefix = "gfxinfo_${packageName}_"
                val keyWithoutPrefix = it.key.removePrefix(prefix)

                if (keyWithoutPrefix != it.key && keyRenameMap.containsKey(keyWithoutPrefix)) {
                    Measurement(keyRenameMap[keyWithoutPrefix]!!, it.value)
                } else {
                    throw IllegalStateException("Unexpected key ${it.key}")
                }
            }
            .filter { keyAllowList.contains(it.name) }
    }
}

/**
 * Captures app startup timing metrics.
 *
 * This outputs the following measurements:
 * * `timeToInitialDisplayMs` - Time from the system receiving a launch intent to rendering the
 *   first frame of the destination Activity.
 * * `timeToFullDisplayMs` - Time from the system receiving a launch intent until the application
 *   reports fully drawn via [android.app.Activity.reportFullyDrawn]. The measurement stops at the
 *   completion of rendering the first frame after (or containing) the `reportFullyDrawn()` call.
 *   This measurement may not be available prior to API 29.
 */
@Suppress("CanSealedSubClassBeObject")
class StartupTimingMetric : Metric() {
    override fun configure(packageName: String) {}

    override fun start() {}

    override fun stop() {}

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        return StartupTimingQuery.getFrameSubMetrics(
                session = traceSession,
                captureApiLevel = captureInfo.apiLevel,
                targetPackageName = captureInfo.targetPackageName,

                // Pick an arbitrary startup mode if unspecified. In the future, consider throwing
                // an
                // error if startup mode not defined
                startupMode = captureInfo.startupMode ?: StartupMode.COLD
            )
            ?.run {
                mapOf(
                        "timeToInitialDisplayMs" to timeToInitialDisplayNs.nsToDoubleMs(),
                        "timeToFullDisplayMs" to timeToFullDisplayNs?.nsToDoubleMs()
                    )
                    .filterValues { it != null }
                    .map { Measurement(it.key, it.value!!) }
            } ?: emptyList()
    }
}

/** Captures app startup timing metrics. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("CanSealedSubClassBeObject")
@RequiresApi(29)
class StartupTimingLegacyMetric : Metric() {
    override fun configure(packageName: String) {}

    override fun start() {}

    override fun stop() {}

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        // Acquires perfetto metrics
        val traceMetrics = traceSession.getTraceMetrics("android_startup")
        val androidStartup =
            traceMetrics.android_startup
                ?: throw IllegalStateException("No android_startup metric found.")
        val appStartup =
            androidStartup.startup.firstOrNull { it.package_name == captureInfo.targetPackageName }
                ?: throw IllegalStateException(
                    "Didn't find startup for pkg " +
                        "${captureInfo.targetPackageName}, found startups for pkgs: " +
                        "${androidStartup.startup.map { it.package_name }}"
                )

        // Extract app startup
        val measurements = mutableListOf<Measurement>()

        val durMs = appStartup.to_first_frame?.dur_ms
        if (durMs != null) {
            measurements.add(Measurement("startupMs", durMs))
        }

        val fullyDrawnMs = appStartup.report_fully_drawn?.dur_ms
        if (fullyDrawnMs != null) {
            measurements.add(Measurement("fullyDrawnMs", fullyDrawnMs))
        }

        return measurements
    }
}

/**
 * Metric which captures results from a Perfetto trace with custom [PerfettoTraceProcessor] queries.
 *
 * This is a more customizable version of [TraceSectionMetric] which can perform arbitrary queries
 * against the captured PerfettoTrace.
 *
 * Sample metric which finds the duration of the first "activityResume" trace section for the traced
 * package:
 * ```
 * class ActivityResumeMetric : TraceMetric() {
 *     override fun getMeasurements(
 *         captureInfo: CaptureInfo,
 *         traceSession: PerfettoTraceProcessor.Session
 *     ): List<Measurement> {
 *         val rowSequence = traceSession.query(
 *             """
 *             SELECT
 *                 slice.name as name,
 *                 slice.ts as ts,
 *                 slice.dur as dur
 *             FROM slice
 *                 INNER JOIN thread_track on slice.track_id = thread_track.id
 *                 INNER JOIN thread USING(utid)
 *                 INNER JOIN process USING(upid)
 *             WHERE
 *                 process.name LIKE ${captureInfo.targetPackageName}
 *                     AND slice.name LIKE "activityResume"
 *             """.trimIndent()
 *         )
 *         // this metric queries a single slice type to produce submetrics, but could be extended
 *         // to capture timing of every component of activity lifecycle
 *         val activityResultNs = rowSequence.firstOrNull()?.double("dur")
 *         return if (activityResultMs != null) {
 *             listOf(Measurement("activityResumeMs", activityResultNs / 1_000_000.0))
 *         } else {
 *             emptyList()
 *         }
 *     }
 * }
 * ```
 *
 * @see PerfettoTraceProcessor
 * @see PerfettoTraceProcessor.Session
 * @see PerfettoTraceProcessor.Session.query
 */
@ExperimentalMetricApi
abstract class TraceMetric : Metric() {
    override fun configure(packageName: String) {}

    override fun start() {}

    override fun stop() {}

    /**
     * Get the metric result for a given iteration given information about the target process and a
     * TraceProcessor session
     */
    public abstract override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement>
}

/**
 * Captures the time taken by named trace section - a named begin / end pair matching the provided
 * [sectionName].
 *
 * Select how matching sections are resolved into a duration metric with [mode], and configure if
 * sections outside the target process are included with [targetPackageOnly].
 *
 * The following TraceSectionMetric counts the number of JIT method compilations that occur within a
 * trace:
 * ```
 * TraceSectionMetric(
 *     sectionName = "JIT Compiling %",
 *     mode = TraceSectionMetric.Mode.Sum
 * )
 * ```
 *
 * Note that non-terminating slices in the trace (where duration = -1) are always ignored by this
 * metric.
 *
 * @see androidx.tracing.Trace.beginSection
 * @see androidx.tracing.Trace.endSection
 * @see androidx.tracing.trace
 */
@ExperimentalMetricApi
class TraceSectionMetric
@JvmOverloads
constructor(
    /**
     * Section name or pattern to match.
     *
     * "%" can be used as a wildcard, as this is supported by the underlying
     * [PerfettoTraceProcessor] query. For example `"JIT %"` will match a section named `"JIT
     * compiling int com.package.MyClass.method(int)"` present in the trace.
     */
    private val sectionName: String,
    /**
     * Defines how slices matching [sectionName] should be confirmed to metrics, by default uses
     * [Mode.Sum] to count and sum durations of all matching trace sections.
     */
    private val mode: Mode = Mode.Sum,
    /** Metric label, defaults to [sectionName]. */
    private val label: String = sectionName,
    /** Filter results to trace sections only from the target process, defaults to true. */
    private val targetPackageOnly: Boolean = true
) : Metric() {
    sealed class Mode(internal val name: String) {
        /**
         * Captures the duration of the first instance of `sectionName` in the trace.
         *
         * When this mode is used, no measurement will be reported if the named section does not
         * appear in the trace.
         */
        object First : Mode("First")

        /**
         * Captures the sum of all instances of `sectionName` in the trace.
         *
         * When this mode is used, a measurement of `0` will be reported if the named section does
         * not appear in the trace.
         */
        object Sum : Mode("Sum")

        /**
         * Reports the maximum observed duration for a trace section matching `sectionName` in the
         * trace.
         *
         * When this mode is used, no measurement will be reported if the named section does not
         * appear in the trace.
         */
        object Min : Mode("Min")

        /**
         * Reports the maximum observed duration for a trace section matching `sectionName` in the
         * trace.
         *
         * When this mode is used, no measurement will be reported if the named section does not
         * appear in the trace.
         */
        object Max : Mode("Max")

        /**
         * Counts the number of observed instances of a trace section matching `sectionName` in the
         * trace.
         *
         * When this mode is used, a measurement of `0` will be reported if the named section does
         * not appear in the trace.
         */
        object Count : Mode("Count")

        /**
         * Average duration of trace sections matching `sectionName` in the trace.
         *
         * When this mode is used, a measurement of `0` will be reported if the named section does
         * not appear in the trace.
         */
        object Average : Mode("Average")

        /**
         * Internal class to prevent external exhaustive when statements, which would break as we
         * add more to this sealed class.
         */
        internal object WhenPrevention : Mode("N/A")
    }

    override fun configure(packageName: String) {}

    override fun start() {}

    override fun stop() {}

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val slices =
            traceSession
                .querySlices(
                    sectionName,
                    packageName = if (targetPackageOnly) captureInfo.targetPackageName else null
                )
                .filter { it.dur != -1L } // filter out non-terminating slices

        return when (mode) {
            Mode.First -> {
                val slice = slices.firstOrNull()
                if (slice == null) {
                    emptyList()
                } else listOf(Measurement(name = label + "FirstMs", data = slice.dur / 1_000_000.0))
            }
            Mode.Sum -> {
                listOf(
                    Measurement(
                        name = label + "SumMs",
                        // note, this duration assumes non-reentrant slices
                        data = slices.sumOf { it.dur } / 1_000_000.0
                    ),
                    Measurement(name = label + "Count", data = slices.size.toDouble())
                )
            }
            Mode.Min -> {
                if (slices.isEmpty()) {
                    emptyList()
                } else
                    listOf(
                        Measurement(
                            name = label + "MinMs",
                            data = slices.minOf { it.dur } / 1_000_000.0
                        )
                    )
            }
            Mode.Max -> {
                if (slices.isEmpty()) {
                    emptyList()
                } else
                    listOf(
                        Measurement(
                            name = label + "MaxMs",
                            data = slices.maxOf { it.dur } / 1_000_000.0
                        )
                    )
            }
            Mode.Count -> {
                listOf(Measurement(name = label + "Count", data = slices.size.toDouble()))
            }
            Mode.Average -> {
                listOf(
                    Measurement(
                        name = label + "AverageMs",
                        data = slices.sumOf { it.dur } / 1_000_000.0 / slices.size
                    )
                )
            }
            Mode.WhenPrevention -> throw IllegalStateException("WhenPrevention should be unused")
        }
    }
}

/**
 * Captures the change of power, energy or battery charge metrics over time for specified duration.
 * A configurable output of power, energy, subsystems, and battery charge will be generated.
 * Subsystem outputs will include the sum of all power or energy metrics within it. A metric total
 * will also be generated for power and energy, as well as a metric which is the sum of all
 * unselected metrics.
 *
 * @param type Either [Type.Energy] or [Type.Power], which can be configured to show components of
 *   system power usage, or [Type.Battery], which will halt charging of device to measure power
 *   drain.
 *
 * For [Type.Energy] or [Type.Power], the sum of all categories will be displayed as a `Total`
 * metric. The sum of all unrequested categories will be displayed as an `Unselected` metric. The
 * subsystems that have not been categorized will be displayed as an `Uncategorized` metric. You can
 * check if the local device supports this high precision tracking with
 * [deviceSupportsHighPrecisionTracking].
 *
 * For [Type.Battery], the charge for the start of the run and the end of the run will be displayed.
 * An additional `Diff` metric will be displayed to indicate the charge drain over the course of the
 * test.
 *
 * The metrics will be stored in the format `<type><name><unit>`. This outputs measurements like the
 * following:
 *
 * Power metrics example:
 * ```
 * powerCategoryDisplayUw       min       128.2,   median       128.7,   max       129.8
 * powerComponentCpuBigUw       min         1.9,   median         2.9,   max         3.4
 * powerComponentCpuLittleUw    min        65.8,   median        76.2,   max        79.7
 * powerComponentCpuMidUw       min        10.8,   median        13.3,   max        13.6
 * powerTotalUw                 min       362.4,   median       395.2,   max       400.6
 * powerUnselectedUw            min       155.3,   median       170.8,   max       177.8
 * ```
 *
 * Energy metrics example:
 * ```
 * energyCategoryDisplayUws     min    610,086.0,   median    623,183.0,   max    627,259.0
 * energyComponentCpuBigUws     min      9,233.0,   median     13,566.0,   max     16,536.0
 * energyComponentCpuLittleUws  min    318,591.0,   median    368,211.0,   max    379,106.0
 * energyComponentCpuMidUws     min     52,143.0,   median     64,462.0,   max     64,893.0
 * energyTotalUws               min  1,755,261.0,   median  1,880,687.0,   max  1,935,402.0
 * energyUnselectedUws          min    752,111.0,   median    813,036.0,   max    858,934.0
 * ```
 *
 * Battery metrics example:
 * ```
 * batteryDiffMah       min         2.0,   median         2.0,   max         4.0
 * batteryEndMah        min     3,266.0,   median     3,270.0,   max     3,276.0
 * batteryStartMah      min     3,268.0,   median     3,274.0,   max     3,278.0
 * ```
 *
 * This measurement is not available prior to API 29.
 */
@RequiresApi(29)
@ExperimentalMetricApi
class PowerMetric(private val type: Type) : Metric() {

    companion object {
        internal const val MEASURE_BLOCK_SECTION_NAME = "measureBlock"

        @JvmStatic
        fun Battery(): Type.Battery {
            return Type.Battery()
        }

        @JvmStatic
        fun Energy(
            categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ): Type.Energy {
            return Type.Energy(categories)
        }

        @JvmStatic
        fun Power(
            categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ): Type.Power {
            return Type.Power(categories)
        }

        /**
         * Returns true if the current device can be used for high precision [Power] and [Energy]
         * metrics.
         *
         * This can be used to change behavior or fall back to lower precision tracking:
         * ```
         * metrics = listOf(
         *     if (PowerMetric.deviceSupportsHighPrecisionTracking()) {
         *         PowerMetric(Type.Energy()) // high precision tracking
         *     } else {
         *         PowerMetric(Type.Battery()) // fall back to less precise tracking
         *     }
         * )
         * ```
         *
         * Or to skip a test when detailed tracking isn't available:
         * ```
         * @Test fun myDetailedPowerBenchmark {
         *     assumeTrue(PowerMetric.deviceSupportsHighPrecisionTracking())
         *     macrobenchmarkRule.measureRepeated (
         *         metrics = listOf(PowerMetric(Type.Energy(...)))
         *     ) {
         *         ...
         *     }
         * }
         * ```
         */
        @JvmStatic
        fun deviceSupportsHighPrecisionTracking(): Boolean =
            hasMetrics(throwOnMissingMetrics = false)

        /**
         * Returns true if [Type.Battery] measurements can be performed, based on current device
         * charge.
         *
         * This can be used to change behavior or throw a clear error before metric configuration,
         * or to skip the test, e.g. with `assumeTrue(PowerMetric.deviceBatteryHasMinimumCharge())`
         */
        @JvmStatic
        fun deviceBatteryHasMinimumCharge(): Boolean =
            hasMinimumCharge(throwOnMissingMetrics = false)
    }

    /**
     * Configures the PowerMetric request.
     *
     * @param categories A map which is used to configure which metrics are displayed. The key is a
     *   `PowerCategory` enum, which configures the subsystem category that will be displayed. The
     *   value is a `PowerCategoryDisplayLevel`, which configures whether each subsystem in the
     *   category will have metrics displayed independently or summed for a total metric of the
     *   category.
     */
    sealed class Type(var categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()) {
        class Power(powerCategories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()) :
            Type(powerCategories)

        class Energy(energyCategories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()) :
            Type(energyCategories)

        class Battery : Type()
    }

    override fun configure(packageName: String) {
        if (type is Type.Energy || type is Type.Power) {
            hasMetrics(throwOnMissingMetrics = true)
        } else {
            hasMinimumCharge(throwOnMissingMetrics = true)
        }
    }

    override fun start() {
        if (type is Type.Battery) {
            Shell.executeScriptSilent("setprop power.battery_input.suspended true")
        }
    }

    override fun stop() {
        if (type is Type.Battery) {
            Shell.executeScriptSilent("setprop power.battery_input.suspended false")
        }
    }

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        // collect metrics between trace point flags
        val slice =
            traceSession.querySlices(MEASURE_BLOCK_SECTION_NAME, packageName = null).firstOrNull()
                ?: return emptyList()

        if (type is Type.Battery) {
            return getBatteryDischargeMetrics(traceSession, slice)
        }

        return getPowerMetrics(traceSession, slice)
    }

    private fun getBatteryDischargeMetrics(
        session: PerfettoTraceProcessor.Session,
        slice: Slice
    ): List<Measurement> {
        val metrics = BatteryDischargeQuery.getBatteryDischargeMetrics(session, slice)
        return metrics.map { measurement ->
            Measurement(getLabel(measurement.name), measurement.chargeMah)
        }
    }

    private fun getPowerMetrics(
        session: PerfettoTraceProcessor.Session,
        slice: Slice
    ): List<Measurement> {
        val metrics = PowerQuery.getPowerMetrics(session, slice)

        val metricMap: Map<String, Double> = getSpecifiedMetrics(metrics)
        if (metricMap.isEmpty()) {
            return emptyList()
        }

        val extraMetrics: Map<String, Double> = getTotalAndUnselectedMetrics(metrics)

        return (metricMap + extraMetrics).map { Measurement(it.key, it.value) }
    }

    private fun getLabel(metricName: String, displayType: String = ""): String {
        return when (type) {
            is Type.Power -> "power${displayType}${metricName}Uw"
            is Type.Energy -> "energy${displayType}${metricName}Uws"
            is Type.Battery -> "battery${metricName}Mah"
        }
    }

    private fun getTotalAndUnselectedMetrics(
        metrics: Map<PowerCategory, PowerQuery.CategoryMeasurement>
    ): Map<String, Double> {
        return mapOf(
                getLabel("Total") to
                    metrics.values.fold(0.0) { total, next -> total + next.getValue(type) },
                getLabel("Unselected") to
                    metrics
                        .filter { (category, _) -> !type.categories.containsKey(category) }
                        .values
                        .fold(0.0) { total, next -> total + next.getValue(type) }
            )
            .filter { (_, measurement) -> measurement != 0.0 }
    }

    private fun getSpecifiedMetrics(
        metrics: Map<PowerCategory, PowerQuery.CategoryMeasurement>
    ): Map<String, Double> {
        return metrics
            .filter { (category, _) -> type.categories.containsKey(category) }
            .map { (category, measurement) ->
                val sectionName = if (category == PowerCategory.UNCATEGORIZED) "" else "Category"
                when (type.categories[category]) {
                    // if total category specified, create component of sum total of category
                    PowerCategoryDisplayLevel.TOTAL ->
                        listOf(
                            getLabel(category.toString().camelCase(), sectionName) to
                                measurement.components.fold(0.0) { total, next ->
                                    total + next.getValue(type)
                                }
                        )
                    // if breakdown, append all ComponentMeasurements metrics from category
                    else ->
                        measurement.components.map { component ->
                            getLabel(component.name, "Component") to component.getValue(type)
                        }
                }
            }
            .flatten()
            .associate { pair -> Pair(pair.first, pair.second) }
    }
}

/**
 * Metric for tracking the memory usage of the target application.
 *
 * There are two modes for measurement - `Last`, which represents the last observed value during an
 * iteration, and `Max`, which represents the largest sample observed per measurement.
 *
 * By default, reports:
 * * `memoryRssAnonKb` - Anonymous resident/allocated memory owned by the process, not including
 *   memory mapped files or shared memory.
 * * `memoryRssAnonFileKb` - Memory allocated by the process to map files.
 * * `memoryHeapSizeKb` - Heap memory allocations from the Android Runtime, sampled after each GC.
 * * `memoryGpuKb` - GPU Memory allocated for the process.
 *
 * By passing a custom `subMetrics` list, you can enable other [SubMetric]s.
 */
@ExperimentalMetricApi
class MemoryUsageMetric(
    private val mode: Mode,
    private val subMetrics: List<SubMetric> =
        listOf(
            SubMetric.HeapSize,
            SubMetric.RssAnon,
            SubMetric.RssFile,
            SubMetric.Gpu,
        )
) : TraceMetric() {
    enum class Mode {
        /**
         * Select the last available sample for each value. Useful for inspecting the final state of
         * e.g. Heap Size.
         */
        Last,

        /**
         * Select the maximum value observed.
         *
         * Useful for inspecting the worst case state, e.g. finding worst heap size during a given
         * scenario.
         */
        Max
    }

    enum class SubMetric(
        /** Name of counter in trace. */
        internal val counterName: String,
        /**
         * False if the metric is represented in the trace in bytes, and must be divided by 1024 to
         * be converted to KB.
         */
        internal val alreadyInKb: Boolean
    ) {
        HeapSize("Heap size (KB)", alreadyInKb = true),
        RssAnon("mem.rss.anon", alreadyInKb = false),
        RssFile("mem.rss.file", alreadyInKb = false),
        RssShmem("mem.rss.shmem", alreadyInKb = false),
        Gpu("GPU Memory", alreadyInKb = false)
    }

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {

        val suffix = mode.toString()
        return MemoryUsageQuery.getMemoryUsageKb(
                session = traceSession,
                targetPackageName = captureInfo.targetPackageName,
                mode = mode
            )
            ?.mapNotNull {
                if (it.key in subMetrics) {
                    Measurement("memory${it.key}${suffix}Kb", it.value.toDouble())
                } else {
                    null
                }
            } ?: listOf()
    }
}

/** Captures the number of page faults over time for a target package name. */
@ExperimentalMetricApi
class MemoryCountersMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val metrics =
            MemoryCountersQuery.getMemoryCounters(
                session = traceSession,
                targetPackageName = captureInfo.targetPackageName
            ) ?: return listOf()

        return listOf(
            Measurement("minorPageFaults", metrics.minorPageFaults),
            Measurement("majorPageFaults", metrics.majorPageFaults),
            Measurement("pageFaultsBackedBySwapCache", metrics.pageFaultsBackedBySwapCache),
            Measurement("pageFaultsBackedByReadIO", metrics.pageFaultsBackedByReadIO),
            Measurement("memoryCompactionEvents", metrics.memoryCompactionEvents),
            Measurement("memoryReclaimEvents", metrics.memoryReclaimEvents),
        )
    }
}
