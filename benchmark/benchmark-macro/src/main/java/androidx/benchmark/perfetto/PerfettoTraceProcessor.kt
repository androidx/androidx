/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.benchmark.perfetto

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.benchmark.macro.perfetto.server.PerfettoHttpServer
import androidx.benchmark.userspaceTrace
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.intellij.lang.annotations.Language
import perfetto.protos.QueryResult
import perfetto.protos.TraceMetrics

/**
 * Kotlin API for [Perfetto Trace Processor](https://perfetto.dev/docs/analysis/trace-processor),
 * which enables SQL querying against the data stored in a Perfetto trace.
 *
 * This includes synchronous and async trace sections, kernel-level scheduling timing,
 * binder events... If it's displayed in Android Studio system trace or
 * [ui.perfetto.dev](https://ui.perfetto.dev), it can be queried from this API.
 *
 * ```
 * // Collect the duration of all slices named "activityStart" in the trace
 * val activityStartDurationNs = PerfettoTraceProcessor.runServer {
 *     loadTrace(trace) {
 *         query("SELECT dur FROM slice WHERE name LIKE \"activityStart\"").toList {
 *             it.long("dur")
 *         }
 *     }
 * }
 * ```
 *
 * Note that traces generally hold events from multiple apps, services and processes, so it's
 * recommended to filter potentially common trace events to the process you're interested in. See
 * the following example which queries `Choreographer#doFrame` slices (labelled spans of time) only
 * for a given package name:
 *
 * ```
 * query("""
 *     |SELECT
 *     |    slice.name,slice.ts,slice.dur
 *     |FROM slice
 *     |    INNER JOIN thread_track on slice.track_id = thread_track.id
 *     |    INNER JOIN thread USING(utid)
 *     |    INNER JOIN process USING(upid)
 *     |WHERE
 *     |    slice.name LIKE "Choreographer#doFrame%" AND
 *     |    process.name LIKE "$packageName"
 *     """.trimMargin()
 * )
 * ```
 * See also Perfetto project documentation:
 * * [Trace Processor overview](https://perfetto.dev/docs/analysis/trace-processor)
 * * [Common queries](https://perfetto.dev/docs/analysis/common-queries)
 *
 * @see PerfettoTrace
 */
@ExperimentalPerfettoTraceProcessorApi
class PerfettoTraceProcessor {
    companion object {
        internal const val PORT = 9001

        /**
         * The actual [File] path to the `trace_processor_shell`.
         *
         * Lazily copies the `trace_processor_shell` and enables parsing of the Perfetto trace files.
         */
        internal val shellPath: String by lazy {
            // Checks for ABI support
            PerfettoHelper.createExecutable("trace_processor_shell")
        }

        /**
         * Starts a Perfetto trace processor shell server in http mode, loads a trace and executes
         * the given block. It stops the server after the block is complete
         */
        @JvmStatic
        fun <T> runServer(
            block: PerfettoTraceProcessor.() -> T
        ): T = userspaceTrace("PerfettoTraceProcessor#runServer") {
            var perfettoTraceProcessor: PerfettoTraceProcessor? = null
            try {

                // Initializes the server process
                perfettoTraceProcessor = PerfettoTraceProcessor().startServer()

                // Executes the query block
                return@userspaceTrace userspaceTrace("PerfettoTraceProcessor#runServer#block") {
                    block(perfettoTraceProcessor)
                }
            } finally {
                perfettoTraceProcessor?.stopServer()
            }
        }

        @RestrictTo(LIBRARY_GROUP)
        fun <T> runSingleSessionServer(
            absoluteTracePath: String,
            block: Session.() -> T
        ) = runServer {
            loadTrace(PerfettoTrace(absoluteTracePath)) {
                block(this)
            }
        }
    }

    /**
     * Loads a PerfettoTrace into the trace processor server to query data out of the trace.
     */
    fun <T> loadTrace(
        trace: PerfettoTrace,
        block: Session.() -> T
    ): T {
        loadTraceImpl(trace.path)
        // TODO: unload trace after block
        return block.invoke(Session(this))
    }

    /**
     * Handle to query sql data from a [PerfettoTrace].
     *
     * @see query
     */
    class Session internal constructor(
        private val traceProcessor: PerfettoTraceProcessor
    ) {
        /**
         * Computes the given metric on the previously loaded trace.
         */
        @RestrictTo(LIBRARY_GROUP) // avoids exposing Proto API
        fun getTraceMetrics(metric: String): TraceMetrics {
            userspaceTrace("PerfettoTraceProcessor#getTraceMetrics $metric") {
                require(!metric.contains(" ")) {
                    "Metric must not contain spaces: $metric"
                }
                require(traceProcessor.perfettoHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }

                // Compute metrics
                val computeResult = traceProcessor.perfettoHttpServer.computeMetric(listOf(metric))
                if (computeResult.error != null) {
                    throw IllegalStateException(computeResult.error)
                }

                // Decode and return trace metrics
                return TraceMetrics.ADAPTER.decode(computeResult.metrics!!)
            }
        }

        /**
         * Computes the given query on the currently loaded trace.
         *
         * Each row returned by a query is returned by the `Sequence` as a [Row]. To extract data
         * from a `Row`, query by column name. The following example does this for name, timestamp,
         * and duration of slices:
         * ```
         * // Runs the provided callback on each activityStart instance in the trace,
         * // providing name, start timestamp (in ns) and duration (in ns)
         * fun PerfettoTraceProcessor.Session.forEachActivityStart(callback: (String, Long, Long) -> Unit) {
         *     query("SELECT name,ts,dur FROM slice WHERE name LIKE \"activityStart\"").forEach {
         *         callback(it.string("name"), it.long("ts"), it.long("dur")
         *         // or, used as a map:
         *         //callback(it["name"] as String, it["ts"] as Long, it["dur"] as Long)
         *     }
         * }
         * ```
         *
         * @see PerfettoTraceProcessor
         * @see PerfettoTraceProcessor.Session
         */
        fun query(@Language("sql") query: String): Sequence<Row> {
            userspaceTrace("PerfettoTraceProcessor#query $query".take(127)) {
                require(traceProcessor.perfettoHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }
                val queryResult = traceProcessor.perfettoHttpServer.rawQuery(query) {
                    // Note: check for errors as part of decode, so it's immediate
                    // instead of lazily in QueryResultIterator
                    QueryResult.decodeAndCheckError(query, it)
                }
                return Sequence { QueryResultIterator(queryResult) }
            }
        }

        private fun QueryResult.Companion.decodeAndCheckError(
            query: String,
            inputStream: InputStream
        ) = ADAPTER.decode(inputStream).also {
            check(it.error == null) {
                throw IllegalStateException("Error with query: --$query--, error=${it.error}")
            }
        }

        /**
         * Computes the given query on the currently loaded trace, returning the resulting protobuf
         * bytes as a [ByteArray].
         *
         * Use [Session.query] if you do not wish to parse the Proto result yourself.
         *
         * The `QueryResult` protobuf definition can be found
         * [in the Perfetto project](https://github.com/google/perfetto/blob/master/protos/perfetto/trace_processor/trace_processor.proto),
         * which can be used to decode the result returned here with a protobuf parsing library.
         *
         * Note that this method does not check for errors in the protobuf, that is the caller's
         * responsibility.
         *
         * @see Session.query
         */
        fun rawQuery(@Language("sql") query: String): ByteArray {
            userspaceTrace("PerfettoTraceProcessor#query $query".take(127)) {
                require(traceProcessor.perfettoHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }
                return traceProcessor.perfettoHttpServer.rawQuery(query) { it.readBytes() }
            }
        }

        /**
         * Query a trace for a list of slices - name, timestamp, and duration.
         *
         * Note that sliceNames may include wildcard matches, such as `foo%`
         */
        @RestrictTo(LIBRARY_GROUP) // Slice API not currently exposed, since it doesn't track table
        fun querySlices(vararg sliceNames: String): List<Slice> {
            require(traceProcessor.perfettoHttpServer.isRunning()) {
                "Perfetto trace_shell_process is not running."
            }

            val whereClause = sliceNames
                .joinToString(separator = " OR ") {
                    "slice.name LIKE \"$it\""
                }

            return query(
                query = """
                    SELECT slice.name,ts,dur
                    FROM slice
                    WHERE $whereClause
                    """.trimMargin()
            ).toSlices()
        }
    }

    private val perfettoHttpServer: PerfettoHttpServer = PerfettoHttpServer()
    private var traceLoaded = false

    private fun startServer(): PerfettoTraceProcessor =
        userspaceTrace("PerfettoTraceProcessor#startServer") {
            println("startserver")
            perfettoHttpServer.startServer()
            return@userspaceTrace this
        }

    private fun stopServer() = userspaceTrace("PerfettoTraceProcessor#stopServer") {
        println("stopserver")
        perfettoHttpServer.stopServer()
    }

    /**
     * Loads a trace in the current instance of the trace processor, clearing any previous loaded
     * trace if existing.
     */
    private fun loadTraceImpl(absoluteTracePath: String) {
        userspaceTrace("PerfettoTraceProcessor#loadTraceImpl") {
            require(!absoluteTracePath.contains(" ")) {
                "Trace path must not contain spaces: $absoluteTracePath"
            }

            val traceFile = File(absoluteTracePath)
            require(traceFile.exists() && traceFile.isFile) {
                "Trace path must exist and not be a directory: $absoluteTracePath"
            }

            // In case a previous trace was loaded, ensures to clear
            if (traceLoaded) {
                clearTrace()
            }

            val parseResults = perfettoHttpServer.parse(FileInputStream(traceFile))
            parseResults.forEach { if (it.error != null) throw IllegalStateException(it.error) }

            // Notifies the server that it won't receive any more trace parts
            perfettoHttpServer.notifyEof()

            traceLoaded = true
        }
    }

    /**
     * Clears the current loaded trace.
     */
    private fun clearTrace() = userspaceTrace("PerfettoTraceProcessor#clearTrace") {
        perfettoHttpServer.restoreInitialTables()
        traceLoaded = false
    }
}

/**
 * Helper for fuzzy matching process name to package
 */
internal fun processNameLikePkg(pkg: String): String {
    return """(process.name LIKE "$pkg" OR process.name LIKE "$pkg:%")"""
}
