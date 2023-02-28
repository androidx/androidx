/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.benchmark.macro.perfetto.server.PerfettoHttpServer
import androidx.benchmark.macro.perfetto.server.QueryResultIterator
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.userspaceTrace
import java.io.File
import org.jetbrains.annotations.TestOnly
import perfetto.protos.TraceMetrics

/**
 * Enables parsing perfetto traces on-device
 */
@RestrictTo(LIBRARY_GROUP) // for internal benchmarking only
class PerfettoTraceProcessor {

    companion object {
        const val PORT = 9001

        /**
         * The actual [File] path to the `trace_processor_shell`.
         *
         * Lazily copies the `trace_processor_shell` and enables parsing of the perfetto trace files.
         */
        @get:TestOnly
        val shellPath: String by lazy {
            // Checks for ABI support
            PerfettoHelper.createExecutable("trace_processor_shell")
        }

        /**
         * Starts a perfetto trace processor shell server in http mode, loads a trace and executes
         * the given block. It stops the server after the block is complete
         */
        fun <T> runServer(
            absoluteTracePath: String? = null,
            block: PerfettoTraceProcessor.() -> T
        ): T = userspaceTrace("PerfettoTraceProcessor#runServer") {
            var perfettoTraceProcessor: PerfettoTraceProcessor? = null
            try {

                // Initializes the server process
                perfettoTraceProcessor = PerfettoTraceProcessor().startServer()

                // Loads a trace if required
                if (absoluteTracePath != null) {
                    perfettoTraceProcessor.loadTrace(absoluteTracePath)
                }

                // Executes the query block
                return@userspaceTrace userspaceTrace("PerfettoTraceProcessor#runServer#block") {
                    block(perfettoTraceProcessor)
                }
            } finally {
                perfettoTraceProcessor?.stopServer()
            }
        }
    }

    private val perfettoHttpServer: PerfettoHttpServer = PerfettoHttpServer()
    private var traceLoaded = false

    private fun startServer(): PerfettoTraceProcessor =
        userspaceTrace("PerfettoTraceProcessor#startServer") {
            perfettoHttpServer.startServer()
            return@userspaceTrace this
        }

    private fun stopServer() = userspaceTrace("PerfettoTraceProcessor#stopServer") {
        perfettoHttpServer.stopServer()
    }

    /**
     * Loads a trace in the current instance of the trace processor, clearing any previous loaded
     * trace if existing.
     */
    fun loadTrace(absoluteTracePath: String) = userspaceTrace("PerfettoTraceProcessor#loadTrace") {
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
        traceLoaded = false

        val parseResult = perfettoHttpServer.parse(traceFile.readBytes())
        if (parseResult.error != null) {
            throw IllegalStateException(parseResult.error)
        }

        // Notifies the server that it won't receive any more trace parts
        perfettoHttpServer.notifyEof()

        traceLoaded = true
    }

    /**
     * Clears the current loaded trace.
     */
    private fun clearTrace() = userspaceTrace("PerfettoTraceProcessor#clearTrace") {
        perfettoHttpServer.restoreInitialTables()
    }

    /**
     * Computes the given metric on the previously loaded trace.
     */
    fun getTraceMetrics(metric: String): TraceMetrics =
        userspaceTrace("PerfettoTraceProcessor#getTraceMetrics $metric") {
            require(!metric.contains(" ")) {
                "Metric must not contain spaces: $metric"
            }
            require(perfettoHttpServer.isRunning()) {
                "Perfetto trace_shell_process is not running."
            }

            // Compute metrics
            val computeResult = perfettoHttpServer.computeMetric(listOf(metric))
            if (computeResult.error != null) {
                throw IllegalStateException(computeResult.error)
            }

            // Decode and return trace metrics
            return@userspaceTrace TraceMetrics.ADAPTER.decode(computeResult.metrics!!)
        }

    /**
     * Computes the given query on the previously loaded trace.
     */
    fun rawQuery(query: String): QueryResultIterator =
        userspaceTrace("PerfettoTraceProcessor#rawQuery $query".take(127)) {
            require(perfettoHttpServer.isRunning()) {
                "Perfetto trace_shell_process is not running."
            }
            return@userspaceTrace perfettoHttpServer.query(query)
        }

    /**
     * Query a trace for a list of slices - name, timestamp, and duration.
     *
     * Note that sliceNames may include wildcard matches, such as `foo%`
     */
    fun querySlices(
        vararg sliceNames: String
    ): List<Slice> {
        require(perfettoHttpServer.isRunning()) { "Perfetto trace_shell_process is not running." }

        val whereClause = sliceNames
            .joinToString(separator = " OR ") {
                "slice.name LIKE \"$it\""
            }

        val queryResultIterator = rawQuery(
            query = """
                SELECT slice.name,ts,dur
                FROM slice
                WHERE $whereClause
            """.trimMargin()
        )

        return queryResultIterator.toSlices()
    }
}

/**
 * Helper for fuzzy matching process name to package
 */
internal fun processNameLikePkg(pkg: String): String {
    return """(process.name LIKE "$pkg" OR process.name LIKE "$pkg:%")"""
}
