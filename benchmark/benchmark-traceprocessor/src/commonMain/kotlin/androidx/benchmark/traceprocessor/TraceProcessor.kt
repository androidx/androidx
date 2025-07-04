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

package androidx.benchmark.traceprocessor

import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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
 * @sample androidx.benchmark.samples.traceProcessorRunServerSimple
 *
 * Note that traces generally hold events from multiple apps, services and processes, so it's
 * recommended to filter potentially common trace events to the process you're interested in. See
 * the following example which queries `Choreographer#doFrame` slices (labelled spans of time) only
 * for a given package name:
 *
 * @sample androidx.benchmark.samples.packageFilteredQuery
 *
 * See also Perfetto project documentation:
 * * [Trace Processor overview](https://perfetto.dev/docs/analysis/trace-processor)
 * * [Common queries](https://perfetto.dev/docs/analysis/common-queries)
 *
 * @see PerfettoTrace
 */
public class TraceProcessor
@Suppress("ExecutorRegistration") // purely synchronous
@JvmOverloads
@ExperimentalTraceProcessorApi
internal constructor(
    private val serverLifecycleManager: ServerLifecycleManager,
    private val tracer: Tracer = Tracer(),
    private val eventCallback: EventCallback = EventCallback.Noop,
    @Suppress("ListenerLast") private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {

    public open class Tracer {
        public open fun beginTraceSection(label: String) {}

        public open fun endTraceSection() {}

        public inline fun <T> trace(label: String, block: () -> T): T {
            beginTraceSection(label)
            return try {
                block()
            } finally {
                endTraceSection()
            }
        }
    }

    public interface EventCallback {
        public fun onLoadTraceFailure(trace: PerfettoTrace, throwable: Throwable)

        public object Noop : EventCallback {
            override fun onLoadTraceFailure(trace: PerfettoTrace, throwable: Throwable) {}
        }
    }

    public class Handle(public val traceProcessor: TraceProcessor) : AutoCloseable {
        override fun close() {
            traceProcessor.stopServer()
        }
    }

    public companion object {
        @get:RestrictTo(LIBRARY_GROUP)
        @RestrictTo(LIBRARY_GROUP)
        @get:JvmStatic
        public val DEFAULT_TIMEOUT_MS: Long = 120_000

        /**
         * Starts a Perfetto trace processor shell server in http mode, and returns a [Handle] which
         * can be used to access and close the [TraceProcessor] server instance.
         *
         * @sample androidx.benchmark.samples.traceProcessorStartServerSimple
         * @param serverLifecycleManager controls starting and stopping the TraceProcessor process.
         * @param eventCallback callback for events such as trace load failure.
         * @param tracer used to trace begin and end of significant events within this managed run.
         * @param timeoutMs maximum duration in milliseconds for waiting for operations like loading
         *   the server, or querying a trace.
         */
        @Suppress(
            "ExecutorRegistration", // purely synchronous
            "MissingJvmstatic", // JvmOverload doesn't handle mangling
        )
        @ExperimentalTraceProcessorApi
        @JvmStatic
        @CheckResult
        public fun startServer(
            serverLifecycleManager: ServerLifecycleManager,
            eventCallback: EventCallback,
            @Suppress("ListenerLast") tracer: Tracer,
            @Suppress("ListenerLast") timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        ): Handle {
            tracer.trace("TraceProcessor#startServer") {
                return Handle(
                    TraceProcessor(
                            eventCallback = eventCallback,
                            serverLifecycleManager = serverLifecycleManager,
                            timeoutMs = timeoutMs,
                        )
                        .startServerImpl()
                )
            }
        }

        /**
         * Starts a Perfetto trace processor shell server in http mode, loads a trace and executes
         * the given block.
         *
         * @sample androidx.benchmark.samples.traceProcessorRunServerSimple
         * @param serverLifecycleManager controls starting and stopping the TraceProcessor process.
         * @param eventCallback callback for events such as trace load failure.
         * @param tracer used to trace begin and end of significant events within this managed run.
         * @param timeoutMs maximum duration in milliseconds for waiting for operations like loading
         *   the server, or querying a trace.
         * @param block Command to execute using trace processor
         */
        @Suppress(
            "ExecutorRegistration", // purely synchronous
            "MissingJvmstatic", // JvmOverload doesn't handle mangling
        )
        @ExperimentalTraceProcessorApi
        @JvmStatic
        public fun <T> runServer(
            serverLifecycleManager: ServerLifecycleManager,
            eventCallback: EventCallback,
            @Suppress("ListenerLast") tracer: Tracer,
            @Suppress("ListenerLast") timeoutMs: Long = DEFAULT_TIMEOUT_MS,
            @Suppress("ListenerLast") block: TraceProcessor.() -> T,
        ): T =
            tracer.trace("TraceProcessor#runServer") {
                startServer(serverLifecycleManager, eventCallback, tracer, timeoutMs).use {
                    block(it.traceProcessor)
                }
            }
    }

    /** Loads a PerfettoTrace into the trace processor server to query data out of the trace. */
    public fun <T> loadTrace(trace: PerfettoTrace, block: Session.() -> T): T {
        try {
            loadTraceImpl(trace.path).use {
                return block.invoke(it.session)
            }
        } catch (t: Throwable) {
            eventCallback.onLoadTraceFailure(trace, t)
            throw t
        }
    }

    /**
     * Loads a PerfettoTrace into the TraceProcessor server, and returns a [Session.Handle] which
     * can be used to access and close a [Session].
     */
    @CheckResult
    public fun startSession(trace: PerfettoTrace): Session.Handle {
        return try {
            loadTraceImpl(trace.path)
        } catch (t: Throwable) {
            eventCallback.onLoadTraceFailure(trace, t)
            throw t
        }
    }

    /**
     * Handle to query sql data from a [PerfettoTrace].
     *
     * @see query
     */
    public class Session internal constructor(private val traceProcessor: TraceProcessor) {
        public class Handle(public val session: Session) : AutoCloseable {
            override fun close() {
                session.traceProcessor.closeCurrentSession()
            }
        }

        /** Computes the given metric on the previously loaded trace. */
        @RestrictTo(LIBRARY_GROUP) // avoids exposing Proto API
        public fun getTraceMetrics(metric: String): TraceMetrics {
            val computeResult =
                queryAndVerifyMetricResult(
                    listOf(metric),
                    ComputeMetricArgs.ResultFormat.BINARY_PROTOBUF,
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
        public fun queryMetricsProtoBinary(metrics: List<String>): ByteArray {
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
        public fun queryMetricsJson(metrics: List<String>): String {
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
        public fun queryMetricsProtoText(metrics: List<String>): String {
            val computeResult =
                queryAndVerifyMetricResult(metrics, ComputeMetricArgs.ResultFormat.TEXTPROTO)
            check(computeResult.metrics_as_prototext != null)
            return computeResult.metrics_as_prototext
        }

        private fun queryAndVerifyMetricResult(
            metrics: List<String>,
            format: ComputeMetricArgs.ResultFormat,
        ): ComputeMetricResult {
            val nameString = metrics.joinToString()
            require(metrics.none { it.contains(" ") }) {
                "Metrics must not constain spaces, metrics: $nameString"
            }

            traceProcessor.tracer.trace("TraceProcessor#getTraceMetrics $nameString") {
                require(traceProcessor.traceProcessorHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }

                // Compute metrics
                val computeResult =
                    traceProcessor.traceProcessorHttpServer.computeMetric(metrics, format)
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
         * fun TraceProcessor.Session.forEachActivityStart(callback: (String, Long, Long) -> Unit) {
         *     query("SELECT name,ts,dur FROM slice WHERE name LIKE \"activityStart\"").forEach {
         *         callback(it.string("name"), it.long("ts"), it.long("dur")
         *         // or, used as a map:
         *         //callback(it["name"] as String, it["ts"] as Long, it["dur"] as Long)
         *     }
         * }
         * ```
         *
         * @see TraceProcessor
         * @see TraceProcessor.Session
         */
        public fun query(query: String): Sequence<Row> {
            traceProcessor.tracer.trace("TraceProcessor#query $query") {
                require(traceProcessor.traceProcessorHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }
                val queryResult =
                    traceProcessor.traceProcessorHttpServer.rawQuery(query) {
                        // Note: check for errors as part of decode, so it's immediate
                        // instead of lazily in QueryResultIterator
                        QueryResult.decodeAndCheckError(query, it)
                    }
                return Sequence { QueryResultIterator(queryResult) }
            }
        }

        private fun QueryResult.Companion.decodeAndCheckError(
            query: String,
            inputStream: InputStream,
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
        public fun rawQuery(query: String): ByteArray {
            traceProcessor.tracer.trace("TraceProcessor#query $query") {
                require(traceProcessor.traceProcessorHttpServer.isRunning()) {
                    "Perfetto trace_shell_process is not running."
                }
                return traceProcessor.traceProcessorHttpServer.rawQuery(query) { it.readBytes() }
            }
        }

        /**
         * Query a trace for a list of slices - name, timestamp, and duration.
         *
         * Note that sliceNames may include wildcard matches, such as `foo%`
         */
        @RestrictTo(LIBRARY_GROUP) // Slice API not currently exposed, since it doesn't track table
        public fun querySlices(vararg sliceNames: String, packageName: String?): List<Slice> {
            require(traceProcessor.traceProcessorHttpServer.isRunning()) {
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
                    postfix = ")",
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
                        dur = row.long("dur"),
                    )
                }
                .filter { it.dur != -1L } // filter out non-terminating slices
                .toList()
        }
    }

    @OptIn(ExperimentalTraceProcessorApi::class)
    private val traceProcessorHttpServer: TraceProcessorHttpServer =
        TraceProcessorHttpServer(serverLifecycleManager, timeoutMs)
    private var traceLoaded = false

    private fun startServerImpl(): TraceProcessor =
        tracer.trace("TraceProcessor#startServer") {
            println("startserver")
            traceProcessorHttpServer.startServer()
            return@trace this
        }

    private fun stopServer() =
        tracer.trace("TraceProcessor#stopServer") {
            println("stopserver")
            traceProcessorHttpServer.stopServer()
        }

    /**
     * Loads a trace in the current instance of the trace processor, clearing any previous loaded
     * trace if existing.
     */
    private fun loadTraceImpl(absoluteTracePath: String): Session.Handle {
        tracer.trace("TraceProcessor#loadTraceImpl") {
            require(!absoluteTracePath.contains(" ")) {
                "Trace path must not contain spaces: $absoluteTracePath"
            }

            val traceFile = File(absoluteTracePath)
            require(traceFile.exists() && traceFile.isFile) {
                "Trace path must exist and not be a directory: $absoluteTracePath"
            }

            check(!traceLoaded) {
                "Cannot load trace while trace is loaded, close previous trace session first"
            }

            closeCurrentSession()

            val parseResults = traceProcessorHttpServer.parse(FileInputStream(traceFile))
            parseResults.forEach { if (it.error != null) throw IllegalStateException(it.error) }

            // Notifies the server that it won't receive any more trace parts
            traceProcessorHttpServer.notifyEof()

            traceLoaded = true
        }
        return Session.Handle(Session(this))
    }

    private fun closeCurrentSession() {
        tracer.trace("TraceProcessor#clearTrace") {
            if (traceLoaded) {
                traceProcessorHttpServer.restoreInitialTables()
                traceLoaded = false
            }
        }
    }
}

/** Helper for fuzzy matching process name to package */
public fun processNameLikePkg(pkg: String): String {
    // check for truncated package names, which can sometimes occur if perfetto can't capture full
    // names, and only has 16 bytes from sched info (which results in 15 chars due to null
    // termination)
    val truncated =
        if (pkg.length > 15) {
            " OR process.name LIKE \"${pkg.takeLast(15)}\""
        } else {
            ""
        }
    return """(process.name LIKE "$pkg" OR process.name LIKE "$pkg:%"$truncated)"""
}
