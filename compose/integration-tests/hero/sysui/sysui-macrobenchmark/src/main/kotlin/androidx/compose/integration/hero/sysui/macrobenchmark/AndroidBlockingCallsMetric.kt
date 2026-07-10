/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.integration.hero.sysui.macrobenchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

/**
 * Adoption of Perfetto's android_blocking_calls_unagg metric. This metric lists the blocking calls
 * per process, but for a predefined set of metrics.
 *
 * @param blockingCallsOfInterest List of blocking calls to track. Has to be a subset or equal to
 *   the names defined in
 *   external/perfetto/src/trace_processor/perfetto_sql/stdlib/android/critical_blocking_calls.sql.
 */
@OptIn(ExperimentalMetricApi::class)
internal class AndroidBlockingCallsMetric(
    private val blockingCallsOfInterest: List<String> = androidBlockingCallsMetricRelevantCalls
) : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        val traceMetrics = traceSession.getTraceMetrics(ANDROID_BLOCKING_CALLS_UNAGG_PERFETTO_NAME)

        val processesWithBlockingCalls =
            traceMetrics.android_blocking_calls_unagg
                ?.process_with_blocking_calls
                .orEmpty()
                .filter { process ->
                    val processName = process.process?.name
                    processName != null && captureInfo.targetPackageName.contains(processName)
                }

        // Index actual calls found in the trace by name for lookup
        val actualCallsMap =
            processesWithBlockingCalls.flatMap { it.blocking_calls }.associateBy { it.name }

        // Iterate through the fixed list of relevant calls to ensure a metric is reported
        // for every key, defaulting to 0.0 if the call is missing from the trace.
        return blockingCallsOfInterest.flatMap { relevantCallName ->
            val blockingCall = actualCallsMap[relevantCallName]
            val displayName = relevantCallName.replace(" ", "_")

            listOf(
                Measurement(
                    name = "${displayName}_cnt",
                    data = blockingCall?.cnt?.toDouble() ?: 0.0,
                ),
                Measurement(
                    name = "${displayName}_avg_dur_ms",
                    data = blockingCall?.avg_dur_ms?.toDouble() ?: 0.0,
                ),
                Measurement(
                    name = "${displayName}_min_dur_ms",
                    data = blockingCall?.min_dur_ms?.toDouble() ?: 0.0,
                ),
                Measurement(
                    name = "${displayName}_max_dur_ms",
                    data = blockingCall?.max_dur_ms?.toDouble() ?: 0.0,
                ),
            )
        }
    }
}

private const val ANDROID_BLOCKING_CALLS_UNAGG_PERFETTO_NAME = "android_blocking_calls_unagg"

internal val androidBlockingCallsMetricRelevantCalls =
    listOf(
        "measure",
        "layout",
        "animation",
        "traversal",
        "Recomposer:recompose",
        "Recomposer:animation",
        "Compose:recompose",
    )
