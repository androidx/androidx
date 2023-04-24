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
import androidx.benchmark.macro.PowerMetric.Type
import androidx.benchmark.macro.PowerRail.hasMetrics
import androidx.benchmark.macro.perfetto.AudioUnderrunQuery
import androidx.benchmark.macro.perfetto.BatteryDischargeQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric
import androidx.benchmark.macro.perfetto.FrameTimingQuery.getFrameSubMetrics
import androidx.benchmark.macro.perfetto.MemoryCountersQuery
import androidx.benchmark.macro.perfetto.PowerQuery
import androidx.benchmark.macro.perfetto.StartupTimingQuery
import androidx.benchmark.macro.perfetto.camelCase
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.Slice

/**
 * Metric interface.
 */
sealed class Metric {

    internal abstract fun configure(packageName: String)

    internal abstract fun start()

    internal abstract fun stop()

    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a
     *  general config object coming into [start].
     */
    internal abstract fun getResult(
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
    data class Measurement internal constructor(
        /**
         * Unique name of the metric, should be camel case with abbreviated suffix,
         * e.g. `startTimeNs`
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
        constructor(name: String, data: Double) : this(
            name,
            listOf(data),
            requireSingleValue = true
        )

        /**
         * Represents a measurement with a value sampled an arbitrary number of times per iteration.
         *
         * For example, in a jank Macrobenchmark, [FrameTimingMetric] can return multiple
         * measurements for `frameOverrunMs` - one for each observed frame.
         *
         * When measurements are merged across multiple iterations, percentiles are extracted from
         * the total pool of samples: P50, P90, P95, and P99.
         */
        constructor(name: String, dataSamples: List<Double>) : this(
            name,
            dataSamples,
            requireSingleValue = false
        )

        init {
            require(!requireSingleValue || data.size == 1) {
                "Metric.Measurement must be in multi-sample mode, or include only one data item"
            }
        }
    }
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
class AudioUnderrunMetric : Metric() {
    override fun configure(packageName: String) {
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val subMetrics = AudioUnderrunQuery.getSubMetrics(traceSession)

        return listOf(
            Measurement("audioTotalMs", subMetrics.totalMs.toDouble()),
            Measurement("audioUnderrunMs", subMetrics.zeroMs.toDouble())
        )
    }
}

/**
 * Metric which captures timing information from frames produced by a benchmark, such as
 * a scrolling or animation benchmark.
 *
 * This outputs the following measurements:
 *
 * * `frameOverrunMs` (Requires API 31) - How much time a given frame missed its deadline by.
 * Positive numbers indicate a dropped frame and visible jank / stutter, negative numbers indicate
 * how much faster than the deadline a frame was.
 *
 * * `frameCpuTimeMs` - How much time the frame took to be produced on the CPU - on both the UI
 * Thread, and RenderThread.
 */
@Suppress("CanSealedSubClassBeObject")
class FrameTimingMetric : Metric() {
    override fun configure(packageName: String) {}
    override fun start() {}
    override fun stop() {}

    @SuppressLint("SyntheticAccessor")
    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        return FrameTimingQuery.getFrameData(
            session = traceSession,
            captureApiLevel = Build.VERSION.SDK_INT,
            packageName = captureInfo.targetPackageName
        )
            .getFrameSubMetrics(Build.VERSION.SDK_INT)
            .filterKeys { it == SubMetric.FrameDurationCpuNs || it == SubMetric.FrameOverrunNs }
            .map {
                Measurement(
                    name = if (it.key == SubMetric.FrameDurationCpuNs) {
                        "frameDurationCpuMs"
                    } else {
                        "frameOverrunMs"
                    },
                    dataSamples = it.value.map { timeNs -> timeNs.nsToDoubleMs() }
                )
            }
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
class StartupTimingMetric : Metric() {
    override fun configure(packageName: String) {
    }

    override fun start() {
    }

    override fun stop() {
    }

    @SuppressLint("SyntheticAccessor")
    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        return StartupTimingQuery.getFrameSubMetrics(
            session = traceSession,
            captureApiLevel = captureInfo.apiLevel,
            targetPackageName = captureInfo.targetPackageName,

            // Pick an arbitrary startup mode if unspecified. In the future, consider throwing an
            // error if startup mode not defined
            startupMode = captureInfo.startupMode ?: StartupMode.COLD
        )?.run {
            mapOf(
                "timeToInitialDisplayMs" to timeToInitialDisplayNs.nsToDoubleMs(),
                "timeToFullDisplayMs" to timeToFullDisplayNs?.nsToDoubleMs()
            ).filterValues { it != null }
                .map {
                    Measurement(it.key, it.value!!)
                }
        } ?: emptyList()
    }
}

/**
 * Captures app startup timing metrics.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("CanSealedSubClassBeObject")
@RequiresApi(29)
class StartupTimingLegacyMetric : Metric() {
    override fun configure(packageName: String) {
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        // Acquires perfetto metrics
        val traceMetrics = traceSession.getTraceMetrics("android_startup")
        val androidStartup = traceMetrics.android_startup
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
 *     override fun getResult(
 *         captureInfo: CaptureInfo,
 *         traceSession: PerfettoTraceProcessor.Session
 *     ): Result {
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
 *             Result("activityResumeMs", activityResultNs / 1_000_000.0)
 *         } else {
 *             Result()
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
    override fun configure(packageName: String) {
    }

    override fun start() {
    }

    override fun stop() {
    }

    /**
     * Get the metric result for a given iteration given information about the target process and a TraceProcessor session
     */
    public abstract override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement>
}

/**
 * Captures the time taken by named trace section - a named begin / end pair matching the provided
 * [sectionName].
 *
 * Select how matching sections are resolved into a duration metric with [mode].
 *
 * @see androidx.tracing.Trace.beginSection
 * @see androidx.tracing.Trace.endSection
 * @see androidx.tracing.trace
 */
@ExperimentalMetricApi
class TraceSectionMetric(
    private val sectionName: String,
    private val mode: Mode = Mode.First
) : Metric() {
    enum class Mode {
        /**
         * Captures the duration of the first instance of `sectionName` in the trace.
         *
         * When this mode is used, no measurement will be reported if the named section does
         * not appear in the trace.
         */
        First,

        /**
         * Captures the sum of all instances of `sectionName` in the trace.
         *
         * When this mode is used, a measurement of `0` will be reported if the named section
         * does not appear in the trace
         */
        Sum
    }

    override fun configure(packageName: String) {
    }

    override fun start() {
    }

    override fun stop() {
    }

    @SuppressLint("SyntheticAccessor")
    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val slices = traceSession.querySlices(sectionName)

        return when (mode) {
            Mode.First -> {
                val slice = slices.firstOrNull()
                if (slice == null) {
                    emptyList()
                } else listOf(
                    Measurement(
                        name = sectionName + "Ms",
                        data = slice.dur / 1_000_000.0
                    )
                )
            }

            Mode.Sum -> {
                listOf(
                    Measurement(
                        name = sectionName + "Ms",
                        // note, this duration assumes non-reentrant slices
                        data = slices.sumOf { it.dur } / 1_000_000.0
                    )
                )
            }
        }
    }
}

/**
 * Captures the change of power, energy or battery charge metrics over time for specified duration.
 * A configurable output of power, energy, subsystems, and battery charge will be generated.
 * Subsystem outputs will include the sum of all power or energy metrics within it.  A metric total
 * will also be generated for power and energy, as well as a metric which is the sum of all
 * unselected metrics.
 *
 * @param type Either [Type.Energy] or [Type.Power], which can be configured to show components
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
@ExperimentalMetricApi
class PowerMetric(
    private val type: Type
) : Metric() {

    companion object {
        internal const val MEASURE_BLOCK_SECTION_NAME = "measureBlock"

        @Suppress("FunctionName")
        @JvmStatic
        fun Battery(): Type.Battery {
            return Type.Battery()
        }

        @Suppress("FunctionName")
        @JvmStatic
        fun Energy(
            categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ): Type.Energy {
            return Type.Energy(categories)
        }

        @Suppress("FunctionName")
        @JvmStatic
        fun Power(
            categories: Map<PowerCategory, PowerCategoryDisplayLevel> = emptyMap()
        ): Type.Power {
            return Type.Power(categories)
        }
    }

    /**
     * Configures the PowerMetric request.
     *
     * @param categories A map which is used to configure which metrics are displayed.  The key
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

    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        // collect metrics between trace point flags
        val slice = traceSession.querySlices(MEASURE_BLOCK_SECTION_NAME)
            .firstOrNull()
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
        val metrics = BatteryDischargeQuery.getBatteryDischargeMetrics(
            session,
            slice
        )
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

        return (metricMap + extraMetrics).map {
            Measurement(it.key, it.value)
        }
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

/**
 * Captures the number of page faults over time for a target package name.
 */
@ExperimentalMetricApi
class MemoryCountersMetric : TraceMetric() {
    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session
    ): List<Measurement> {
        val metrics = MemoryCountersQuery.getMemoryCounters(
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
