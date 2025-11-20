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

@file:Suppress("unused")
@file:OptIn(ExperimentalTraceProcessorApi::class)

package androidx.benchmark.samples

import androidx.annotation.Sampled
import androidx.benchmark.macro.runServer
import androidx.benchmark.macro.startServer
import androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.TraceProcessor

@Sampled
fun traceProcessorRunServerSimple(): List<Long> {
    // Collect the duration of all slices named "activityStart" in the trace
    val activityStartDurNs =
        TraceProcessor.runServer {
            loadTrace(PerfettoTrace("/path/to/trace.perfetto-trace")) {
                    query("SELECT dur FROM slice WHERE name = 'activityStart'").map {
                        it.long("dur")
                    }
                }
                .toList()
        }
    return activityStartDurNs
}

@Sampled
fun traceProcessorStartServerSimple(): List<Long> {
    // Collect the duration of all slices named "activityStart" in the trace
    val activityStartDurNs =
        TraceProcessor.startServer().use {
            it.traceProcessor.startSession(PerfettoTrace("/path/to/trace.perfetto-trace")).use {
                it.session
                    .query("SELECT dur FROM slice WHERE name = 'activityStart'")
                    .map { it.long("dur") }
                    .toList()
            }
        }
    return activityStartDurNs
}

@Sampled
fun TraceProcessor.packageFilteredQuery(
    packageName: String,
    doFrameCallback: (String, Long, Long) -> Unit,
) {
    loadTrace(PerfettoTrace("/path/to/trace.perfetto-trace")) {
        query(
                """
            |SELECT
            |    slice.name, slice.ts, slice.dur
            |FROM slice
            |    INNER JOIN thread_track on slice.track_id = thread_track.id
            |    INNER JOIN thread USING(utid)
            |    INNER JOIN process USING(upid)
            |WHERE
            |    slice.name LIKE "Choreographer#doFrame%" AND
            |    process.name = "$packageName"
            """
                    .trimMargin()
            )
            .forEach {
                // process each observed doFrame slice
                doFrameCallback(it.string("name"), it.long("ts"), it.long("dur"))
            }
    }
}
