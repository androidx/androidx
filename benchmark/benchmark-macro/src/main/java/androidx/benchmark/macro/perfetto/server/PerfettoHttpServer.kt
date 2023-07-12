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

import android.annotation.SuppressLint
import android.os.Build
import android.security.NetworkSecurityPolicy
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.benchmark.Shell
import androidx.benchmark.ShellScript
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.userspaceTrace
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import perfetto.protos.AppendTraceDataResult
import perfetto.protos.ComputeMetricArgs
import perfetto.protos.ComputeMetricResult
import perfetto.protos.QueryArgs
import perfetto.protos.StatusResult

/**
 * Wrapper around perfetto trace_shell_processor that communicates via http. The implementation
 * is based on the python one of the official repo:
 * https://github.com/google/perfetto/blob/master/python/perfetto/trace_processor/http.py
 */
internal class PerfettoHttpServer {

    companion object {
        private const val HTTP_ADDRESS = "http://localhost"
        private const val METHOD_GET = "GET"
        private const val METHOD_POST = "POST"
        private const val PATH_QUERY = "/query"
        private const val PATH_COMPUTE_METRIC = "/compute_metric"
        private const val PATH_PARSE = "/parse"
        private const val PATH_NOTIFY_EOF = "/notify_eof"
        private const val PATH_STATUS = "/status"
        private const val PATH_RESTORE_INITIAL_TABLES = "/restore_initial_tables"

        private const val TAG = "PerfettoHttpServer"
        private const val SERVER_START_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_SECONDS = 300000
        private const val SERVER_PROCESS_NAME = "trace_processor_shell"

        // Note that trace processor http server has a hard limit of 64Mb for payload size.
        // https://cs.android.com/android/platform/superproject/+/master:external/perfetto/src/base/http/http_server.cc;l=33
        private const val PARSE_PAYLOAD_SIZE = 16 * 1024 * 1024 // 16Mb

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
            val script = "echo pid:$$ ; exec ${PerfettoTraceProcessor.shellPath} -D" +
                " --http-port \"${PerfettoTraceProcessor.PORT}\" "
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

    private var processId: Int? = null

    /**
     * Blocking method that runs the perfetto trace_shell_processor in server mode.
     *
     * @throws IllegalStateException if the server is not running by the end of the timeout.
     */
    @SuppressLint("BanThreadSleep")
    fun startServer() = userspaceTrace("PerfettoHttpServer#startServer") {
        if (processId != null) {
            Log.w(TAG, "Tried to start a trace shell processor that is already running.")
            return@userspaceTrace
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            !Api24Impl.isCleartextTrafficPermittedForLocalhost()
        ) {
            throw IOException(
                """
                Macrobenchmark requires cleartext HTTP traffic to the on-device localhost to enable
                querying data from perfetto traces, such as timestamps that are used to calculate
                metrics. This should be enabled by default via manifest merging when building with
                Gradle.  Please refer to
                https://d.android.com/training/articles/security-config#CleartextTrafficPermitted
                and enable cleartext http requests towards localhost in your test android manifest.
            """.trimIndent()
            )
        }

        val shellScript = getOrCreateShellScript().start()

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

                // In the event that the instrumentation app cannot connect to the
                // trace_processor_shell server, trying to read the full stderr may make the
                // process hang. Here we check if the process is still running to determine if
                // that's the case and throw the correct exception.

                val processRunning =
                    processId?.let { Shell.isProcessAlive(it, SERVER_PROCESS_NAME) } ?: false

                if (processRunning) {
                    throw IllegalStateException(
                        """
                        The instrumentation app cannot connect to the trace_processor_shell server.
                        """.trimIndent()
                    )
                } else {
                    throw IllegalStateException(
                        """
                        Perfetto trace_processor_shell did not start correctly.
                        Process stderr:
                        ${shellScript.getOutputAndClose().stderr}
                        """.trimIndent()
                    )
                }
            }
        }
        Log.i(TAG, "Perfetto trace processor shell server started (pid=$processId).")
    }

    /**
     * Stops the server killing the associated process
     */
    fun stopServer() = userspaceTrace("PerfettoHttpServer#stopServer") {
        if (processId == null) {
            Log.w(TAG, "Tried to stop trace shell processor http server without starting it.")
            return@userspaceTrace
        }
        Shell.executeScriptSilent("kill -TERM $processId")
        Log.i(TAG, "Perfetto trace processor shell server stopped (pid=$processId).")
    }

    /**
     * Returns true whether the server is running, false otherwise.
     */
    fun isRunning(): Boolean = userspaceTrace("PerfettoHttpServer#isRunning") {
        return@userspaceTrace try {
            val statusResult = status()
            return@userspaceTrace statusResult.api_version != null && statusResult.api_version > 0
        } catch (e: ConnectException) {
            false
        }
    }

    /**
     * Executes the given [sqlQuery] on a previously parsed trace with custom decoding.
     *
     * Note that this does not decode the query result, so it's the caller's responsibility to check
     * for errors in the result.
     */
    fun <T> rawQuery(sqlQuery: String, decodeBlock: (InputStream) -> T): T =
        httpRequest(
            method = METHOD_POST,
            url = PATH_QUERY,
            encodeBlock = { QueryArgs.ADAPTER.encode(it, QueryArgs(sqlQuery)) },
            decodeBlock = decodeBlock
        )

    /**
     * Computes the given metrics on a previously parsed trace.
     */
    fun computeMetric(metrics: List<String>): ComputeMetricResult =
        httpRequest(
            method = METHOD_POST,
            url = PATH_COMPUTE_METRIC,
            encodeBlock = { ComputeMetricArgs.ADAPTER.encode(it, ComputeMetricArgs(metrics)) },
            decodeBlock = { ComputeMetricResult.ADAPTER.decode(it) }
        )

    /**
     * Parses the trace file in chunks. Note that [notifyEof] should be called at the end to let
     * the processor know that no more chunks will be sent.
     */
    fun parse(inputStream: InputStream): List<AppendTraceDataResult> {
        val responses = mutableListOf<AppendTraceDataResult>()
        while (true) {
            val buffer = ByteArray(PARSE_PAYLOAD_SIZE)
            val read = inputStream.read(buffer)
            if (read <= 0) break
            responses.add(httpRequest(
                method = METHOD_POST,
                url = PATH_PARSE,
                encodeBlock = { it.write(buffer, 0, read) },
                decodeBlock = { AppendTraceDataResult.ADAPTER.decode(it) }
            ))
        }
        return responses
    }

    /**
     * Notifies that the entire trace has been uploaded and no more chunks will be sent.
     */
    fun notifyEof() =
        httpRequest(
            method = METHOD_GET,
            url = PATH_NOTIFY_EOF,
            encodeBlock = null,
            decodeBlock = { }
        )

    /**
     * Clears the loaded trace and restore the state of the initial tables
     */
    fun restoreInitialTables() =
        httpRequest(
            method = METHOD_GET,
            url = PATH_RESTORE_INITIAL_TABLES,
            encodeBlock = null,
            decodeBlock = { }
        )

    /**
     * Checks the status of the trace_shell_processor http server.
     */
    private fun status(): StatusResult =
        httpRequest(
            method = METHOD_GET,
            url = PATH_STATUS,
            encodeBlock = null,
            decodeBlock = { StatusResult.ADAPTER.decode(it) }
        )

    private fun <T> httpRequest(
        method: String,
        url: String,
        contentType: String = "application/octet-stream",
        encodeBlock: ((OutputStream) -> Unit)?,
        decodeBlock: ((InputStream) -> T)
    ): T {
        with(
            URL("$HTTP_ADDRESS:${PerfettoTraceProcessor.PORT}$url")
                .openConnection() as HttpURLConnection
        ) {
            requestMethod = method
            readTimeout = READ_TIMEOUT_SECONDS
            setRequestProperty("Content-Type", contentType)
            if (encodeBlock != null) {
                doOutput = true
                encodeBlock(outputStream)
                outputStream.close()
            }
            val value = decodeBlock(inputStream)
            if (responseCode != 200) {
                throw IllegalStateException(responseMessage)
            }
            return value
        }
    }
}

@RequiresApi(24)
private object Api24Impl {
    @DoNotInline
    fun isCleartextTrafficPermittedForLocalhost() =
        NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("localhost")
}