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
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.Profiler
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.macro.perfetto.server.PerfettoHttpServer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.intellij.lang.annotations.Language
import perfetto.protos.ComputeMetricArgs
import perfetto.protos.ComputeMetricResult
import perfetto.protos.QueryResult
import perfetto.protos.TraceMetrics

/**
 * Kotlin API for [Perfetto Trace Processor](https://perfetto.dev/docs/analysis/trace-processor),
 * which enables SQL querying against the data stored in a Perfetto trace.
 *
 * This includes synchronous and async trace sections, kernel-level scheduling timing, binder
 * events... If it's displayed in Android Studio system trace or
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
 *
 * See also Perfetto project documentation:
 * * [Trace Processor overview](https://perfetto.dev/docs/analysis/trace-processor)
 * * [Common queries](https://perfetto.dev/docs/analysis/common-queries)
 *
 * @see PerfettoTrace
 */
@ExperimentalPerfettoTraceProcessorApi
class PerfettoTraceProcessor {
    companion object {
        private val SERVER_START_TIMEOUT_MS = 60.seconds
        internal const val PORT = 9001

        /**
         * The actual [File] path to the `trace_processor_shell`.
         *
         * Lazily copies the `trace_processor_shell` and enables parsing of the Perfetto trace
         * files.
         */
        internal val shellPath: String by lazy {
            // Checks for ABI support
            PerfettoHelper.createExecutable("trace_processor_shell")
        }

        /**
         * Starts a Perfetto trace processor shell server in http mode, loads a trace and executes
         * the given block. It stops the server after the block is complete
         *
         * Uses a default timeout of 5 seconds
         *
         * @param block Command to execute using trace processor
         */
        @JvmStatic
        fun <T> runServer(block: PerfettoTraceProcessor.() -> T): T =
            runServer(SERVER_START_TIMEOUT_MS, block)

        /**
         * Starts a Perfetto trace processor shell server in http mode, loads a trace and executes
         * the given block. It stops the server after the block is complete
         *
         * @param timeout waiting for the server to start. If less or equal to zero use 5 seconds
         * @param block Command to execute using trace processor
         */
        @JvmStatic
        fun <T> runServer(timeout: Duration, block: PerfettoTraceProcessor.() -> T): T =
            inMemoryTrace("PerfettoTraceProcessor#runServer") {
                var actualTimeout = timeout
                if (actualTimeout <= Duration.ZERO) {
                    actualTimeout = SERVER_START_TIMEOUT_MS
                }

                var perfettoTraceProcessor: PerfettoTraceProcessor? = null
                try {

                    // Initializes the server process
                    perfettoTraceProcessor = PerfettoTraceProcessor().startServer(actualTimeout)

                    // Executes the query block
                    return@inMemoryTrace inMemoryTrace("PerfettoTraceProcessor#runServer#block") {
                        block(perfettoTraceProcessor)
                    }
                } finally {
                    perfettoTraceProcessor?.stopServer()
                }
            }

        @RestrictTo(LIBRARY_GROUP)
        fun <T> runSingleSessionServer(absoluteTracePath: String, block: Session.() -> T) =
            runServer {
                loadTrace(PerfettoTrace(absoluteTracePath)) { block(this) }
            }
    }

    /** Loads a PerfettoTrace into the trace processor server to query data out of the trace. */
    fun <T> loadTrace(trace: PerfettoTrace, block: Session.() -> T): T {
        loadTraceImpl(trace.path)
        // TODO: unload trace after block
        try {
            return block.invoke(Session(this))
        } catch (t: Throwable) {
            // TODO: move this behavior to an extension function in benchmark when
            //  this class moves out of benchmark group
            // TODO: consider a label argument to control logging like this in the success case as
            //  well, which lets us get rid of FileLinkingRule (which doesn't work well anyway)
            if (trace.path.startsWith(Outputs.outputDirectory.absolutePath)) {
                // only link trace with failure to Studio if it's an output file
                InstrumentationResults.instrumentationReport {
                    val label = "Trace with processing error: ${t.message?.take(50)?.trim()}..."
                    reportSummaryToIde(
                        profilerResults =
                            listOf(
                                Profiler.ResultFile.ofPerfettoTrace(
                                    label = label,
                                    absolutePath = trace.path
                                )
                            )
                    )
                }
            }
            throw t
        }
    }

    /**
     * Handle to query sql data from a [PerfettoTrace].
     *
     * @see query
     */
    class Session internal constructor(private val traceProcessor: PerfettoTraceProcessor) {
        /** Computes the given metric on the previously loaded trace. */
        @RestrictTo(LIBRARY_GROUP) // avoids exposing Proto API
        fun getTraceMetrics(metric: String): TraceMetrics {
            val computeResult =
                queryAndVerifyMetricResult(
                    listOf(metric),
                    ComputeMetricArgs.ResultFormat.BINARY_PROTOBUF
                )
            return TraceMetrics.ADAPTER.decode(computeResult.metrics!!)
        }

        /**
         * Computes the given metrics, returning the results as a binary proto.
         *
         * The proto format definition for decoding this binary format can be found
         * [here](https://cs.android.com/android/platform/superproject/main/+/main:external/perfetto/protos/perfetto/metrics/).
         *
         * See
         * [perfetto metric docs](https://perfetto.dev/docs/quickstart/trace-analysis#trace-based-metrics)
         * for an overview on trace based metrics.
         */
        fun queryMetricsProtoBinary(metrics: List<String>): ByteArray {
            val computeResult =
                queryAndVerifyMetricResult(metrics, ComputeMetricArgs.ResultFormat.BINARY_PROTOBUF)
            return computeResult.metrics!!.toByteArray()
        }

        /**
         * Computes the given metrics, returning the results as JSON text.
         *
         * The proto format definition for these metrics can be found
         * [here](https://cs.android.com/android/platform/superproject/main/+/main:external/perfetto/protos/perfetto/metrics/).
         *
         * See
         * [perfetto metric docs](https://perfetto.dev/docs/quickstart/trace-analysis#trace-based-metrics)
         * for an overview on trace based metrics.
         */
        fun queryMetricsJson(metrics: List<String>): String {
            val computeResult =
                queryAndVerifyMetricResult(metrics, ComputeMetricArgs.ResultFormat.JSON)
            check(computeResult.metrics_as_json != null)
            return computeResult.metrics_as_json
        }

        /**
         * Computes the given metrics, returning the result as proto text.
         *
         * The proto format definition for these metrics can be found
         * [here](https://cs.android.com/android/platform/superproject/main/+/main:external/perfetto/protos/perfetto/metrics/).
         *
         * See
         * [perfetto metric docs](https://perfetto.dev/docs/quickstart/trace-analysis#trace-based-metrics)
         * for an overview on trace based metrics.
         */
        fun queryMetricsProtoText(metrics: List<String>): String {
            val computeResult =
                queryAndVerifyMetricResult(metrics, ComputeMetricArgs.ResultFormat.TEXTPROTO)
            check(computeResult.metrics_as_prototext != null)
            return computeResult.metrics_as_prototext
        }

        private fun queryAndVerifyMetricResult(
            metrics: List<String>,
            format: ComputeMetricArgs.ResultFormat
        ): ComputeMetricResult {
            val nameString = metrics.joinToString()
            require(metrics.none { it.contains(" ") }) {
                "Metrics must not constain spaces, metrics: $nameString"
            }

            inMemoryTrace("PerfettoTraceProcessor#getTraceMetrics $nameString") {
                require(traceProcessor.perfettoHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }

                // Compute metrics
                val computeResult = traceProcessor.perfettoHttpServer.computeMetric(metrics, format)
                if (computeResult.error != null) {
                    throw IllegalStateException(computeResult.error)
                }

                return computeResult
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
            inMemoryTrace("PerfettoTraceProcessor#query $query".take(127)) {
                require(traceProcessor.perfettoHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }
                val queryResult =
                    traceProcessor.perfettoHttpServer.rawQuery(query) {
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
        ) =
            ADAPTER.decode(inputStream).also {
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
            inMemoryTrace("PerfettoTraceProcessor#query $query".take(127)) {
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
        fun querySlices(
            vararg sliceNames: String,
            packageName: String?,
        ): List<Slice> {
            require(traceProcessor.perfettoHttpServer.isRunning()) {
                "Perfetto trace_shell_process is not running."
            }

            val whereClause =
                sliceNames.joinToString(
                    separator = " OR ",
                    prefix =
                        if (packageName == null) {
                            "("
                        } else {
                            processNameLikePkg(packageName) + " AND ("
                        },
                    postfix = ")"
                ) {
                    "slice_name LIKE \"$it\""
                }
            val innerJoins =
                if (packageName != null) {
                    """
                INNER JOIN thread_track ON slice.track_id = thread_track.id
                INNER JOIN thread USING(utid)
                INNER JOIN process USING(upid)
                """
                        .trimMargin()
                } else {
                    ""
                }

            val processTrackInnerJoins =
                """
                INNER JOIN process_track ON slice.track_id = process_track.id
                INNER JOIN process USING(upid)
            """
                    .trimIndent()

            return query(
                    query =
                        """
                    SELECT slice.name AS slice_name,ts,dur
                    FROM slice
                    $innerJoins
                    WHERE $whereClause
                    UNION
                    SELECT process_track.name AS slice_name,ts,dur
                    FROM slice
                    $processTrackInnerJoins
                    WHERE $whereClause
                    ORDER BY ts
                    """
                            .trimIndent()
                )
                .map { row ->
                    // Using an explicit mapper here to account for the aliasing of `slice_name`
                    Slice(
                        name = row.string("slice_name"),
                        ts = row.long("ts"),
                        dur = row.long("dur")
                    )
                }
                .filter { it.dur != -1L } // filter out non-terminating slices
                .toList()
        }
    }

    private val perfettoHttpServer: PerfettoHttpServer = PerfettoHttpServer()
    private var traceLoaded = false

    private fun startServer(timeout: Duration): PerfettoTraceProcessor =
        inMemoryTrace("PerfettoTraceProcessor#startServer") {
            println("startserver($timeout)")
            perfettoHttpServer.startServer(timeout)
            return@inMemoryTrace this
        }

    private fun stopServer() =
        inMemoryTrace("PerfettoTraceProcessor#stopServer") {
            println("stopserver")
            perfettoHttpServer.stopServer()
        }

    /**
     * Loads a trace in the current instance of the trace processor, clearing any previous loaded
     * trace if existing.
     */
    private fun loadTraceImpl(absoluteTracePath: String) {
        inMemoryTrace("PerfettoTraceProcessor#loadTraceImpl") {
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

    /** Clears the current loaded trace. */
    private fun clearTrace() =
        inMemoryTrace("PerfettoTraceProcessor#clearTrace") {
            perfettoHttpServer.restoreInitialTables()
            traceLoaded = false
        }
}

/** Helper for fuzzy matching process name to package */
internal fun processNameLikePkg(pkg: String): String {
    return """(process.name LIKE "$pkg" OR process.name LIKE "$pkg:%")"""
}
