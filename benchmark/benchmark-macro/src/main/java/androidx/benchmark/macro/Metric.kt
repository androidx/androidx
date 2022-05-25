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

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Shell
import androidx.benchmark.macro.BatteryCharge.hasMinimumCharge
import androidx.benchmark.macro.PowerRail.hasMetrics
import androidx.benchmark.macro.perfetto.AudioUnderrunQuery
import androidx.benchmark.macro.perfetto.BatteryDischargeQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric
import androidx.benchmark.macro.perfetto.PerfettoResultsParser.parseStartupResult
import androidx.benchmark.macro.perfetto.PerfettoTraceProcessor
import androidx.benchmark.macro.perfetto.PowerQuery
import androidx.benchmark.macro.perfetto.Slice
import androidx.benchmark.macro.perfetto.StartupTimingQuery
import androidx.benchmark.macro.perfetto.camelCase
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Metric interface.
 */
public sealed class Metric {

    internal abstract fun configure(packageName: String)

    internal abstract fun start()

    internal abstract fun stop()
    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a
     *  general config object coming into [start].
     */
    internal abstract fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult

    internal data class CaptureInfo(
        val apiLevel: Int,
        val targetPackageName: String,
        val testPackageName: String,
        val startupMode: StartupMode?
    )
}

private fun Long.nsToDoubleMs(): Double = this / 1_000_000.0

/**
 * Metric which captures information about underruns while playing audio.
 *
 * Each time an instance of [android.media.AudioTrack] is started, the systems repeatedly
 * logs the number of audio frames available for output. This doesn't work when audio offload is
 * enabled. No logs are generated while there is no active track. See
 * [android.media.AudioTrack.Builder.setOffloadedPlayback] for more details.
 *
 * Test fails in case of multiple active tracks during a single iteration.
 *
 * This outputs the following measurements:
 *
 * * `audioTotalMs` - Total duration of played audio captured during the iteration.
 * The test fails if no counters are detected.
 *
 * * `audioUnderrunMs` - Duration of played audio when zero audio frames were available for output.
 * Each single log of zero frames available for output indicates a gap in audio playing.
 */
@ExperimentalMetricApi
@Suppress("CanSealedSubClassBeObject")
public class AudioUnderrunMetric : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        val subMetrics = AudioUnderrunQuery.getSubMetrics(tracePath)

        return IterationResult(
            singleMetrics = mapOf(
                "audioTotalMs" to subMetrics.totalMs.toDouble(),
                "audioUnderrunMs" to subMetrics.zeroMs.toDouble()
            ),
            sampledMetrics = emptyMap(),
            timelineRangeNs = null
        )
    }
}

/**
 * Legacy version of FrameTimingMetric, based on 'dumpsys gfxinfo' instead of trace data.
 *
 * Temporary - to be removed after transition to FrameTimingMetric
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FrameTimingGfxInfoMetric : Metric() {
    private lateinit var packageName: String
    private val helper = JankCollectionHelper()

    internal override fun configure(packageName: String) {
        this.packageName = packageName
        helper.addTrackedPackages(packageName)
    }

    internal override fun start() {
        try {
            helper.startCollecting()
        } catch (exception: RuntimeException) {
            // Ignore the exception that might result from trying to clear GfxInfo
            // The current implementation of JankCollectionHelper throws a RuntimeException
            // when that happens. This is safe to ignore because the app being benchmarked
            // is not showing any UI when this happens typically.

            // Once the MacroBenchmarkRule has the ability to setup the app in the right state via
            // a designated setup block, we can get rid of this.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            if (instrumentation != null) {
                if (!Shell.isPackageAlive(packageName)) {
                    error(exception.message ?: "Assertion error, $packageName not running")
                }
            }
        }
    }

    internal override fun stop() {
        helper.stopCollecting()
    }

    /**
     * Used to convert keys from platform to JSON format.
     *
     * This both converts `snake_case_format` to `camelCaseFormat`, and renames for clarity.
     *
     * Note that these will still output to inst results in snake_case, with `MetricNameUtils`
     * via [androidx.benchmark.MetricResult.putInBundle].
     */
    private val keyRenameMap = mapOf(
        "frame_render_time_percentile_50" to "frameTime50thPercentileMs",
        "frame_render_time_percentile_90" to "frameTime90thPercentileMs",
        "frame_render_time_percentile_95" to "frameTime95thPercentileMs",
        "frame_render_time_percentile_99" to "frameTime99thPercentileMs",
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
        "total_frames" to "totalFrameCount",
        "janky_frames_percent" to "jankyFramePercent",
        "janky_frames_legacy_percent" to "jankyFramePercentLegacy"
    )

    /**
     * Filters output to only frameTimeXXthPercentileMs and totalFrameCount
     */
    private val keyAllowList = setOf(
        "frameTime50thPercentileMs",
        "frameTime90thPercentileMs",
        "frameTime95thPercentileMs",
        "frameTime99thPercentileMs",
        "totalFrameCount"
    )

    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String) = IterationResult(
        singleMetrics = helper.metrics
            .map {
                val prefix = "gfxinfo_${packageName}_"
                val keyWithoutPrefix = it.key.removePrefix(prefix)

                if (keyWithoutPrefix != it.key && keyRenameMap.containsKey(keyWithoutPrefix)) {
                    keyRenameMap[keyWithoutPrefix]!! to it.value
                } else {
                    throw IllegalStateException("Unexpected key ${it.key}")
                }
            }
            .toMap()
            .filterKeys { keyAllowList.contains(it) },
        sampledMetrics = emptyMap(),
        timelineRangeNs = null
    )
}

/**
 * Metric which captures timing information from frames produced by a benchmark, such as
 * a scrolling or animation benchmark.
 *
 * This outputs the following measurements:
 *
 * * `frameOverrunMs` (Requires API 29) - How much time a given frame missed its deadline by.
 * Positive numbers indicate a dropped frame and visible jank / stutter, negative numbers indicate
 * how much faster than the deadline a frame was.
 *
 * * `frameCpuTimeMs` - How much time the frame took to be produced on the CPU - on both the UI
 * Thread, and RenderThread.
 */
@Suppress("CanSealedSubClassBeObject")
public class FrameTimingMetric : Metric() {
    internal override fun configure(packageName: String) {}
    internal override fun start() {}
    internal override fun stop() {}

    @SuppressLint("SyntheticAccessor")
    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        val subMetricsMsMap = FrameTimingQuery.getFrameSubMetrics(
            absoluteTracePath = tracePath,
            captureApiLevel = Build.VERSION.SDK_INT,
            packageName = captureInfo.targetPackageName
        )
            .filterKeys { it == SubMetric.FrameDurationCpuNs || it == SubMetric.FrameOverrunNs }
            .mapKeys {
                if (it.key == SubMetric.FrameDurationCpuNs) {
                    "frameDurationCpuMs"
                } else {
                    "frameOverrunMs"
                }
            }
            .mapValues { entry ->
                entry.value.map { timeNs -> timeNs.nsToDoubleMs() }
            }
        return IterationResult(
            singleMetrics = emptyMap(),
            sampledMetrics = subMetricsMsMap,
            timelineRangeNs = null
        )
    }
}

/**
 * Captures app startup timing metrics.
 *
 * This outputs the following measurements:
 *
 * * `timeToInitialDisplayMs` - Time from the system receiving a launch intent to rendering the
 * first frame of the destination Activity.
 *
 * * `timeToFullDisplayMs` - Time from the system receiving a launch intent until the application
 * reports fully drawn via [android.app.Activity.reportFullyDrawn]. The measurement stops at the
 * completion of rendering the first frame after (or containing) the `reportFullyDrawn()` call. This
 * measurement may not be available prior to API 29.
 */
@Suppress("CanSealedSubClassBeObject")
public class StartupTimingMetric : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    @SuppressLint("SyntheticAccessor")
    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        return StartupTimingQuery.getFrameSubMetrics(
            absoluteTracePath = tracePath,
            captureApiLevel = captureInfo.apiLevel,
            targetPackageName = captureInfo.targetPackageName,
            testPackageName = captureInfo.testPackageName,

            // Pick an arbitrary startup mode if unspecified. In the future, consider throwing an
            // error if startup mode not defined
            startupMode = captureInfo.startupMode ?: StartupMode.COLD
        )?.run {
            @Suppress("UNCHECKED_CAST")
            IterationResult(
                singleMetrics = mapOf(
                    "timeToInitialDisplayMs" to timeToInitialDisplayNs.nsToDoubleMs(),
                    "timeToFullDisplayMs" to timeToFullDisplayNs?.nsToDoubleMs()
                ).filterValues { it != null } as Map<String, Double>,
                sampledMetrics = emptyMap(),
                timelineRangeNs = timelineRangeNs
            )
        } ?: IterationResult.EMPTY
    }
}

/**
 * Captures app startup timing metrics.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("CanSealedSubClassBeObject")
@RequiresApi(29)
public class StartupTimingLegacyMetric : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        val json = PerfettoTraceProcessor.getJsonMetrics(tracePath, "android_startup")
        return parseStartupResult(json, captureInfo.targetPackageName)
    }
}

/**
 * Captures the time taken by a trace section - a named begin / end pair matching the provided name.
 *
 * Always selects the first instance of a trace section captured during a measurement.
 *
 * @see androidx.tracing.Trace.beginSection
 * @see androidx.tracing.Trace.endSection
 * @see androidx.tracing.trace
 */
@RequiresApi(29) // Remove once b/182386956 fixed, as app tag may be needed for this to work.
@ExperimentalMetricApi
public class TraceSectionMetric(
    private val sectionName: String
) : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    @SuppressLint("SyntheticAccessor")
    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        val slice = PerfettoTraceProcessor.querySlices(tracePath, sectionName).firstOrNull()
        return if (slice == null) {
            IterationResult.EMPTY
        } else IterationResult(
            singleMetrics = mapOf(
                sectionName + "Ms" to slice.dur / 1_000_000.0
            ),
            sampledMetrics = emptyMap(),
            timelineRangeNs = slice.ts..slice.endTs
        )
    }
}
/**
 * Captures the change of power, energy or battery charge metrics over time for specified duration.
 * A configurable output of power, energy, subsystems, and battery charge will be generated.
 * Subsystem outputs will include the sum of all power or energy metrics within it.  A metric total
 * will also be generated for power and energy, as well as a metric which is the sum of all
 * unselected metrics.
 *
 * @param `type` - Either [Type.Energy] or [Type.Power], which can be configured to show components
 * of system power usage, or [Type.Battery], which will halt charging of device to measure power
 * drain.
 *
 * For [Type.Energy] or [Type.Power], the sum of all categories will be displayed as a `Total`
 * metric.  The sum of all unrequested categories will be displayed as an `Unselected` metric.  The
 * subsystems that have not been categorized will be displayed as an `Uncategorized` metric.
 *
 * For [Type.Battery], the charge for the start of the run and the end of the run will be displayed.
 * An additional `Diff` metric will be displayed to indicate the charge drain over the course of
 * the test.
 *
 * The metrics will be stored in the format `<type><name><unit>`.  This outputs measurements like
 * the following:
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PowerMetric(
    private val type: Type
) : Metric() {

    companion object {
        internal const val MEASURE_BLOCK_SECTION_NAME = "measureBlock"

        fun Battery(): Type.Battery {
            return Type.Battery()
        }

        fun Energy(
            categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ): Type.Energy {
            return Type.Energy(categories)
        }

        fun Power(
            categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ): Type.Power {
            return Type.Power(categories)
        }
    }

    /**
     * Configures the PowerMetric request.
     *
     * @param `categories` - A map which is used to configure which metrics are displayed.  The key
     * is a `PowerCategory` enum, which configures the subsystem category that will be displayed.
     * The value is a `PowerCategoryDisplayLevel`, which configures whether each subsystem in the
     * category will have metrics displayed independently or summed for a total metric of the
     * category.
     */
    sealed class Type(var categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()) {
        class Power(
            powerCategories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ) : Type(powerCategories)
        class Energy(
            energyCategories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ) : Type(energyCategories)
        class Battery : Type()
    }

    internal override fun configure(packageName: String) {
        if (type is Type.Energy || type is Type.Power) {
            hasMetrics(throwOnMissingMetrics = true)
        } else {
            hasMinimumCharge(throwOnMissingMetrics = true)
        }
    }

    internal override fun start() {
        if (type is Type.Battery) {
            Shell.executeCommand("setprop power.battery_input.suspended true")
        }
    }

    internal override fun stop() {
        if (type is Type.Battery) {
            Shell.executeCommand("setprop power.battery_input.suspended false")
        }
    }

    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        // collect metrics between trace point flags
        val slice = PerfettoTraceProcessor.querySlices(tracePath, MEASURE_BLOCK_SECTION_NAME)
            .firstOrNull()
            ?: return IterationResult.EMPTY

        if (type is Type.Battery) {
            return getBatteryDischargeMetrics(tracePath, slice)
        }

        return getPowerMetrics(tracePath, slice)
    }

    private fun getBatteryDischargeMetrics(tracePath: String, slice: Slice): IterationResult {
        val metrics = BatteryDischargeQuery.getBatteryDischargeMetrics(tracePath, slice)

        val metricMap: Map<String, Double> = metrics.associate { measurement ->
            getLabel(measurement.name) to measurement.chargeMah
        }

        return IterationResult(
            singleMetrics = metricMap,
            sampledMetrics = emptyMap())
    }

    private fun getPowerMetrics(tracePath: String, slice: Slice): IterationResult {
        val metrics = PowerQuery.getPowerMetrics(tracePath, slice)

        val metricMap: Map<String, Double> = getSpecifiedMetrics(metrics)
        if (metricMap.isEmpty()) {
            return IterationResult(
                singleMetrics = emptyMap(),
                sampledMetrics = emptyMap())
        }

        val extraMetrics: Map<String, Double> = getTotalAndUnselectedMetrics(metrics)

        return IterationResult(
            singleMetrics = metricMap + extraMetrics,
            sampledMetrics = emptyMap())
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
                metrics.values.fold(0.0) { total, next ->
                total + next.getValue(type)
            },
            getLabel("Unselected") to
                metrics.filter { (category, _) ->
                !type.categories.containsKey(category)
            }.values.fold(0.0) { total, next ->
                total + next.getValue(type)
            }
        ).filter { (_, measurement) ->
            measurement != 0.0
        }
    }

    private fun getSpecifiedMetrics(
        metrics: Map<PowerCategory, PowerQuery.CategoryMeasurement>
    ): Map<String, Double> {
        return metrics.filter { (category, _) ->
            type.categories.containsKey(category)
        }.map { (category, measurement) ->
            val sectionName = if (category == PowerCategory.UNCATEGORIZED) "" else "Category"
            when (type.categories[category]) {
                // if total category specified, create component of sum total of category
                PowerCategoryDisplayLevel.TOTAL -> listOf(
                    getLabel(
                        category.toString().camelCase(), sectionName
                    ) to measurement.components.fold(0.0) { total, next ->
                        total + next.getValue(type)
                    }
                )
                // if breakdown, append all ComponentMeasurements metrics from category
                else -> measurement.components.map { component ->
                    getLabel(
                        component.name, "Component"
                    ) to component.getValue(type)
                }
            }
        }.flatten().associate { pair -> Pair(pair.first, pair.second) }
    }
}
