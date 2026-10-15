/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.testutils

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.processNameLikePkg

// TODO: Consider moving TraceCounterMetric to :benchmark:benchmark-macro public API alongside
//  TraceSectionMetric once the API surface (modes and window slicing) is finalized.

/**
 * Captures summary statistics for a named Perfetto counter track emitted via
 * [androidx.tracing.Trace.setCounter].
 *
 * Select how matching counter samples are aggregated into measurements with [modes] (or [mode]),
 * configure whether counters outside the target process are included with [targetPackageOnly], and
 * optionally slice measurements into chronological time windows matching [windowSliceName].
 *
 * When no matching samples exist, [Mode.Min], [Mode.Median], [Mode.Max], and [Mode.Last] omit
 * measurements while [Mode.Count] reports `0.0`. [Mode.Count] records the number of counter sample
 * events (`Trace.setCounter` value emissions) on matching counter tracks, not the number of counter
 * tracks.
 *
 * @param counterName Name or SQL `LIKE` pattern of the Perfetto counter track to query (for
 *   example, `"pokedex:grid:items:active"`). `"%"` can be used as a wildcard.
 * @param label Metric name prefix presented in benchmark results (for example, `"activeGridItems"`
 *   produces `"activeGridItemsMin"`, `"activeGridItemsMedian"`, etc.).
 * @param modes Aggregation [Mode]s used to summarize counter samples into [Measurement]s.
 * @param windowSliceName Optional section name or SQL `LIKE` pattern used to define chronological
 *   time windows (for example, `"PokedexScrollBenchmark#fling%"`). Matches completed slices from
 *   any process in the trace (e.g. benchmark test process slices). When specified, measurements are
 *   sliced into chronological windows labeled with `Window1..WindowN`.
 * @param targetPackageOnly Filters counter tracks to those emitted by the target application
 *   process (`captureInfo.targetPackageName`). Defaults to `true`. Unlike [counterName],
 *   [windowSliceName] matches completed slices across any process in the trace.
 */
@OptIn(ExperimentalMetricApi::class)
class TraceCounterMetric(
    private val counterName: String,
    private val label: String,
    private val modes: List<Mode>,
    private val windowSliceName: String? = null,
    private val targetPackageOnly: Boolean = true,
) : TraceMetric() {

    /**
     * Captures a single summary [mode] for a named Perfetto counter track emitted via
     * [androidx.tracing.Trace.setCounter].
     *
     * @param counterName Name or SQL `LIKE` pattern of the Perfetto counter track to query.
     * @param label Metric name prefix presented in benchmark results.
     * @param mode Aggregation [Mode] used to summarize counter samples into a [Measurement].
     * @param windowSliceName Optional section name or SQL `LIKE` pattern used to define
     *   chronological time windows. Matches completed slices from any process in the trace.
     * @param targetPackageOnly Filters counter tracks to those emitted by the target application
     *   process (`captureInfo.targetPackageName`). Defaults to `true`.
     */
    constructor(
        counterName: String,
        label: String,
        mode: Mode,
        windowSliceName: String? = null,
        targetPackageOnly: Boolean = true,
    ) : this(
        counterName = counterName,
        label = label,
        modes = listOf(mode),
        windowSliceName = windowSliceName,
        targetPackageOnly = targetPackageOnly,
    )

    init {
        require(modes.isNotEmpty()) { "At least one TraceCounterMetric.Mode must be specified." }
    }

    /** Defines how counter samples matching [counterName] are aggregated into measurements. */
    sealed class Mode(internal val name: String) {
        /**
         * Reports the minimum observed counter value. No measurement is reported if no matching
         * counter samples exist.
         */
        object Min : Mode("Min")

        /**
         * Reports the median observed counter value (or the mean of the two middle values for an
         * even sample count). No measurement is reported if no matching counter samples exist.
         */
        object Median : Mode("Median")

        /**
         * Reports the maximum observed counter value. No measurement is reported if no matching
         * counter samples exist.
         */
        object Max : Mode("Max")

        /**
         * Reports the last observed counter value in chronological order. No measurement is
         * reported if no matching counter samples exist.
         */
        object Last : Mode("Last")

        /**
         * Reports the number of counter sample events (`Trace.setCounter` value emissions) recorded
         * on matching counter tracks (not the number of counter tracks). Reports `0.0` if no
         * matching counter samples exist.
         */
        object Count : Mode("Count")
    }

    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        val targetPackageName = if (targetPackageOnly) captureInfo.targetPackageName else null
        val querySql = buildCounterSamplesQuery(counterName, targetPackageName)
        val samples =
            traceSession
                .query(querySql)
                .map { row ->
                    CounterSample(
                        timestampNanos = row.long(SAMPLE_TIMESTAMP_NANOS_COLUMN),
                        value = row.double(COUNTER_VALUE_COLUMN),
                    )
                }
                .toList()

        if (windowSliceName == null) {
            val values = samples.map { it.value }
            return buildMeasurementsForValues(label, values)
        }

        val windowSlicesQuery = buildWindowSlicesQuery(windowSliceName)
        val windowSlices =
            traceSession
                .query(windowSlicesQuery)
                .map { row ->
                    WindowSlice(
                        startTimestampNanos = row.long(WINDOW_START_TIMESTAMP_NANOS_COLUMN),
                        endTimestampNanos = row.long(WINDOW_END_TIMESTAMP_NANOS_COLUMN),
                    )
                }
                .toList()

        return windowSlices.flatMapIndexed { index, windowSlice ->
            val windowValues =
                samples
                    .filter { sample ->
                        sample.timestampNanos in
                            windowSlice.startTimestampNanos..windowSlice.endTimestampNanos
                    }
                    .map { it.value }
            buildMeasurementsForValues(
                metricPrefix = "${label}Window${index + 1}",
                values = windowValues,
            )
        }
    }

    private fun buildMeasurementsForValues(
        metricPrefix: String,
        values: List<Double>,
    ): List<Measurement> = buildList {
        for (mode in modes) {
            when (mode) {
                Mode.Min -> {
                    if (values.isNotEmpty()) {
                        add(Measurement("${metricPrefix}Min", values.minOrNull()!!))
                    }
                }
                Mode.Median -> {
                    if (values.isNotEmpty()) {
                        add(Measurement("${metricPrefix}Median", values.median()))
                    }
                }
                Mode.Max -> {
                    if (values.isNotEmpty()) {
                        add(Measurement("${metricPrefix}Max", values.maxOrNull()!!))
                    }
                }
                Mode.Last -> {
                    if (values.isNotEmpty()) {
                        add(Measurement("${metricPrefix}Last", values.last()))
                    }
                }
                Mode.Count -> {
                    add(Measurement("${metricPrefix}Count", values.size.toDouble()))
                }
            }
        }
    }

    private fun List<Double>.median(): Double {
        require(isNotEmpty()) { "Cannot compute median of an empty list." }
        val sorted = sorted()
        val middle = size / 2
        return if (size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }

    internal companion object {
        private const val SAMPLE_TIMESTAMP_NANOS_COLUMN = "sample_timestamp_nanos"
        private const val COUNTER_VALUE_COLUMN = "counter_value"
        private const val WINDOW_START_TIMESTAMP_NANOS_COLUMN = "window_start_timestamp_nanos"
        private const val WINDOW_END_TIMESTAMP_NANOS_COLUMN = "window_end_timestamp_nanos"

        internal fun buildCounterSamplesQuery(
            counterName: String,
            targetPackageName: String?,
        ): String {
            val targetProcessClause =
                if (targetPackageName != null) {
                    """

                    AND ${processNameLikePkg(targetPackageName)}
                    """
                        .trimEnd()
                } else {
                    ""
                }
            return """
                -- Select all counter samples for the requested counter track in chronological order.
                SELECT
                  counter.ts AS $SAMPLE_TIMESTAMP_NANOS_COLUMN,
                  CAST(counter.value AS REAL) AS $COUNTER_VALUE_COLUMN
                FROM counter
                INNER JOIN process_counter_track
                  ON counter.track_id = process_counter_track.id
                INNER JOIN process
                  ON process_counter_track.upid = process.upid
                WHERE
                  process_counter_track.name LIKE "$counterName"$targetProcessClause
                ORDER BY
                  counter.ts ASC,
                  counter.id ASC;
                """
                .trimIndent()
        }

        internal fun buildWindowSlicesQuery(windowSlicePattern: String): String =
            """
            -- Select completed window slices in chronological order by start timestamp.
            -- Non-terminating slices (dur = -1) are excluded, matching TraceSectionMetric.
            SELECT
              slice.ts AS $WINDOW_START_TIMESTAMP_NANOS_COLUMN,
              slice.ts + slice.dur AS $WINDOW_END_TIMESTAMP_NANOS_COLUMN
            FROM slice
            WHERE
              slice.name LIKE "$windowSlicePattern"
              AND slice.dur >= 0
            ORDER BY
              slice.ts ASC;
            """
                .trimIndent()
    }
}

private class CounterSample(
    val timestampNanos: Long,
    val value: Double,
)

private class WindowSlice(
    val startTimestampNanos: Long,
    val endTimestampNanos: Long,
)
