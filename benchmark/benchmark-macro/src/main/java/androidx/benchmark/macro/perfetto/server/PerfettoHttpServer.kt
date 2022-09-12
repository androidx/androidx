/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto.server

import android.util.Log
import androidx.benchmark.Shell
import androidx.benchmark.ShellScript
import androidx.benchmark.macro.perfetto.PerfettoTraceProcessor
import androidx.benchmark.userspaceTrace
import okhttp3.MediaType
import okhttp3.RequestBody
import perfetto.protos.AppendTraceDataResult
import perfetto.protos.ComputeMetricArgs
import perfetto.protos.ComputeMetricResult
import perfetto.protos.QueryArgs
import perfetto.protos.StatusResult
import retrofit2.Call

/**
 * Wrapper around perfetto trace_shell_processor that communicates via http. The implementation
 * is based on the python one of the official repo:
 * https://github.com/google/perfetto/blob/master/python/perfetto/trace_processor/http.py
 */
internal class PerfettoHttpServer(private val port: Int) {

    companion object {
        private const val TAG = "PerfettoHttpServer"
        private const val SERVER_START_TIMEOUT_MS = 5000
        private var shellScript: ShellScript? = null

        /**
         * Returns a cached instance of the shell script to run the perfetto trace shell processor
         * as http server. Note that the generated script doesn't specify the port and this must
         * be passed as parameter when running the script.
         */
        fun getOrCreateShellScript(): ShellScript = shellScript ?: synchronized(this) {
            var instance = shellScript
            if (instance != null) {
                return@synchronized instance
            }
            val script =
                """echo pid:$$ ; exec ${PerfettoTraceProcessor.shellPath} -D --http-port "$@" """
            instance = Shell.createShellScript(script)
            shellScript = instance
            instance
        }

        /**
         * Clean up the shell script
         */
        fun cleanUpShellScript() = synchronized(this) {
            shellScript?.cleanUp()
            shellScript = null
        }
    }

    private val perfettoApi by lazy { PerfettoApi.create("http://localhost:$port/") }
    private var processId: Int? = null

    /**
     * Blocking method that runs the perfetto trace_shell_processor in server mode.
     *
     * @throws IllegalStateException if the server is not running by the end of the timeout.
     */
    fun startServer() = userspaceTrace("PerfettoHttpServer#startServer port $port") {
        if (processId != null) {
            Log.w(TAG, "Tried to start a trace shell processor that is already running.")
            return@userspaceTrace
        }

        val shellScript = getOrCreateShellScript().start(port.toString())

        processId = shellScript
            .stdOutLineSequence()
            .first { it.startsWith("pid:") }
            .split("pid:")[1]
            .toInt()

        // Wait for the trace_processor_shell server to start.
        var elapsed = 0
        while (!isRunning()) {
            Thread.sleep(5)
            elapsed += 5
            if (elapsed >= SERVER_START_TIMEOUT_MS) {
                throw IllegalStateException(
                    """
                        Perfetto trace_processor_shell did not start correctly.
                        Process stderr:
                        ${shellScript.getOutputAndClose().stderr}
                    """.trimIndent()
                )
            }
        }
        Log.i(TAG, "Perfetto trace processor shell server started (pid=$processId).")
    }

    /**
     * Stops the server killing the associated process
     */
    fun stopServer() = userspaceTrace("PerfettoHttpServer#stopServer port $port") {
        if (processId == null) {
            Log.w(TAG, "Tried to stop trace shell processor http server without starting it.")
            return@userspaceTrace
        }
        Shell.executeCommand("kill -TERM $processId")
        Log.i(TAG, "Perfetto trace processor shell server stopped (pid=$processId).")
    }

    /**
     * Returns true whether the server is running, false otherwise.
     */
    fun isRunning(): Boolean = userspaceTrace("PerfettoHttpServer#isRunning port $port") {
        return@userspaceTrace try {
            status()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes the given [sqlQuery] on a previously parsed trace and returns the result as a
     * query result iterator.
     */
    fun executeQuery(sqlQuery: String): QueryResultIterator =
        QueryResultIterator(perfettoApi.query(QueryArgs(sqlQuery)).executeAndGetBody())

    /**
     * Computes the given metrics on a previously parsed trace.
     */
    fun computeMetric(metrics: List<String>): ComputeMetricResult =
        perfettoApi.computeMetric(ComputeMetricArgs(metrics)).executeAndGetBody()

    /**
     * Parses the trace file in chunks. Note that [notifyEof] should be called at the end to let
     * the processor know that no more chunks will be sent.
     */
    fun parse(chunk: ByteArray): AppendTraceDataResult {
        val bytes = RequestBody.create(MediaType.parse("application/octet-stream"), chunk)
        return perfettoApi.parse(bytes).executeAndGetBody()
    }

    /**
     * Notifies that the entire trace has been uploaded and no more chunks will be sent.
     */
    fun notifyEof(): Unit =
        perfettoApi.notifyEof().executeAndGetBody()

    /**
     * Checks the status of the trace_shell_processor http server.
     */
    fun status(): StatusResult =
        perfettoApi.status().executeAndGetBody()

    /**
     * Clears the loaded trace and restore the state of the initial tables
     */
    fun restoreInitialTables(): Unit =
        perfettoApi.restoreInitialTables().executeAndGetBody()

    private fun <T> Call<T>.executeAndGetBody(): T {
        val response = execute()
        if (!response.isSuccessful) {
            throw IllegalStateException(response.message())
        }
        return response.body()!!
    }
}
