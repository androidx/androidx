/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.benchmark.samples

import androidx.annotation.Sampled
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

@Sampled
fun getMeasurementsSample() {
    /**
     * Calculates latency of sections where begin and end trace points are in different processes.
     *
     * @param beginPointName Name of begin tracepoint.
     * @param endPointName Name of end tracepoint.
     * @param eventName Name of the final metric that is spit out after the test run. The metric
     *   name will be {$eventName}LatencyMillis.
     */
    @OptIn(ExperimentalMetricApi::class)
    class CrossProcessLatencyMetricSample(
        private val beginPointName: String,
        private val endPointName: String,
        private val eventName: String,
    ) : TraceMetric() {
        @OptIn(ExperimentalMetricApi::class)
        override fun getMeasurements(
            captureInfo: CaptureInfo,
            traceSession: TraceProcessor.Session,
        ): List<Measurement> {
            val query =
                """
             INCLUDE perfetto module slices.with_context;
             SELECT
                 event,
                 ts2-ts1 AS latency_in_nanos
             FROM
             (
               (SELECT
                   ts AS ts1,
                   "event" AS event,
                   LOWER(TRIM(process_name)) as process_name_1
                   FROM thread_slice WHERE name = '${beginPointName}'
               )
               LEFT JOIN
               (SELECT
                   ts AS ts2,
                   "event" AS event,
                   LOWER(TRIM(process_name)) as process_name_2
                   FROM thread_slice WHERE name = '${endPointName}'
               )
               USING (event)
             )
               """
                    .trimIndent() // maybe add checks for process name etc too in query

            val rowSequence = traceSession.query(query)
            // First row (or null) is returned.
            val latencyResultNanos = rowSequence.firstOrNull()?.long("latency_in_nanos")
            return if (latencyResultNanos != null) {
                listOf(Measurement(eventName + "LatencyMillis", latencyResultNanos / 1_000_000.0))
            } else {
                emptyList()
            }
        }
    }
}
