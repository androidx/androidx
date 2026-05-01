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

/**
 * Measures the number of CPU frequency change events in a trace, grouped by time.
 *
 * A CPU frequency change event counts as a change to any CPU's frequency during the trace. If
 * multiple CPUs change frequencies at the same timestamp, this gets counted as *one* frequency
 * change event. For example, for a trace with:
 * - One frequency change in CPU 6, this metric will return 1.
 * - One frequency change in CPU 6 and 7 at timestamp x, this metric will return 1.
 * - One frequency change in CPU 6 at timestamp x and one in CPU 7 at timestamp x + 2, this metric
 *   will return 2.
 *
 * If there was no change in frequency in the trace, this metric will return 0.
 *
 * This requires the trace to include CPU frequency information. See
 * https://perfetto.dev/docs/data-sources/cpu-freq for details.
 */
@OptIn(ExperimentalMetricApi::class)
class CpuFrequencyChangeMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        val cpuFrequencyResults =
            traceSession
                .query(
                    """
                    INCLUDE PERFETTO MODULE linux.cpu.frequency;

                    WITH timestamped_cpu_frequency_counters AS (
                      SELECT 
                        ts,
                        -- Assign a sequence number to each event per CPU, ordered by time
                        ROW_NUMBER() OVER (PARTITION BY cpu ORDER BY ts) AS frequency_change_event_index
                      FROM cpu_frequency_counters
                    )
                    SELECT 
                      COUNT(DISTINCT ts) AS $FREQUENCY_CHANGES_COLUMN_NAME
                    FROM timestamped_cpu_frequency_counters
                    -- A CPU with a frequency change should always have two counter values (old and new).
                    -- We filter out anything < 2 just in case.
                    WHERE frequency_change_event_index > 1;
            """
                        .trimIndent()
                )
                .toList()
        check(cpuFrequencyResults.size == 1) {
            "Expected one row from CPU frequency query, but got: $cpuFrequencyResults"
        }
        return cpuFrequencyResults.map { row ->
            val frequencyChanges = row.long(FREQUENCY_CHANGES_COLUMN_NAME)
            Measurement("freqChangeCountCpu", frequencyChanges.toDouble())
        }
    }
}

private const val FREQUENCY_CHANGES_COLUMN_NAME = "total_frequency_change_events"
