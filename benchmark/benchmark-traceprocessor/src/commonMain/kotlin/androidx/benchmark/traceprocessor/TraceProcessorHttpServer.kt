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

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import perfetto.protos.AppendTraceDataResult
import perfetto.protos.ComputeMetricArgs
import perfetto.protos.ComputeMetricResult
import perfetto.protos.QueryArgs
import perfetto.protos.StatusResult

@OptIn(ExperimentalTraceProcessorApi::class)
internal class TraceProcessorHttpServer(
    private val serverLifecycleManager: ServerLifecycleManager,
    private val timeoutMs: Long,
) {
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
        private val WAIT_INTERVAL = 5.milliseconds

        // Note that trace processor http server has a hard limit of 64Mb for payload size.
        // https://cs.android.com/android/platform/superproject/+/master:external/perfetto/src/base/http/http_server.cc;l=33
        private const val PARSE_PAYLOAD_SIZE = 16 * 1024 * 1024 // 16Mb
    }

    private var hasStarted = false
    private var port: Int = 9001 // todo: track better

    /**
     * Blocking method that runs the perfetto trace_shell_processor in server mode.
     *
     * @throws IllegalStateException if the server is not running by the end of the timeout.
     */
    @Suppress("BanThreadSleep") // needed for awaiting trace processor instance
    fun startServer() {
        if (hasStarted) {
            log("Tried to start a trace shell processor that is already running.")
        } else {
            port = serverLifecycleManager.start()
            // Wait for the trace_processor_shell server to start.
            var elapsed = 0.milliseconds
            while (!isRunning()) {
                Thread.sleep(WAIT_INTERVAL.toLong(DurationUnit.MILLISECONDS))
                elapsed += WAIT_INTERVAL
                if (elapsed >= timeoutMs.toDuration(DurationUnit.MILLISECONDS)) {
                    throw IllegalStateException(serverLifecycleManager.timeoutMessage())
                }
            }

            hasStarted = true

            log("Perfetto trace processor shell server started (port=$port).")
        }
    }

    /** Stops the server killing the associated process */
    fun stopServer() {
        serverLifecycleManager.stop()
    }

    /** Returns true whether the server is running, false otherwise. */
    fun isRunning(): Boolean {
        return try {
            val statusResult = status()
            return statusResult.api_version != null && statusResult.api_version > 0
        } catch (e: ConnectException) {
            // Note that this is fired when the server port is not bound yet.
            // This can happen before the perfetto trace processor server is fully started.
            false
        } catch (e: FileNotFoundException) {
            // Note that this is fired when the endpoint queried does not exist.
            // This can happen before the perfetto trace processor server is fully started.
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
            decodeBlock = decodeBlock,
        )

    /** Computes the given metrics on a previously parsed trace. */
    fun computeMetric(
        metrics: List<String>,
        resultFormat: ComputeMetricArgs.ResultFormat,
    ): ComputeMetricResult =
        httpRequest(
            method = METHOD_POST,
            url = PATH_COMPUTE_METRIC,
            encodeBlock = {
                ComputeMetricArgs.ADAPTER.encode(it, ComputeMetricArgs(metrics, resultFormat))
            },
            decodeBlock = { ComputeMetricResult.ADAPTER.decode(it) },
        )

    /**
     * Parses the trace file in chunks. Note that [notifyEof] should be called at the end to let the
     * processor know that no more chunks will be sent.
     */
    fun parse(inputStream: InputStream): List<AppendTraceDataResult> {
        val responses = mutableListOf<AppendTraceDataResult>()
        while (true) {
            val buffer = ByteArray(PARSE_PAYLOAD_SIZE)
            val read = inputStream.read(buffer)
            if (read <= 0) break
            responses.add(
                httpRequest(
                    method = METHOD_POST,
                    url = PATH_PARSE,
                    encodeBlock = { it.write(buffer, 0, read) },
                    decodeBlock = { AppendTraceDataResult.ADAPTER.decode(it) },
                )
            )
        }
        return responses
    }

    /** Notifies that the entire trace has been uploaded and no more chunks will be sent. */
    fun notifyEof() =
        httpRequest(
            method = METHOD_GET,
            url = PATH_NOTIFY_EOF,
            encodeBlock = null,
            decodeBlock = {},
        )

    /** Clears the loaded trace and restore the state of the initial tables */
    fun restoreInitialTables() =
        httpRequest(
            method = METHOD_GET,
            url = PATH_RESTORE_INITIAL_TABLES,
            encodeBlock = null,
            decodeBlock = {},
        )

    /** Checks the status of the trace_shell_processor http server. */
    private fun status(): StatusResult =
        httpRequest(
            method = METHOD_GET,
            url = PATH_STATUS,
            encodeBlock = null,
            decodeBlock = { StatusResult.ADAPTER.decode(it) },
        )

    private fun <T> httpRequest(
        method: String,
        url: String,
        contentType: String = "application/octet-stream",
        encodeBlock: ((OutputStream) -> Unit)?,
        decodeBlock: ((InputStream) -> T),
    ): T {
        with(URL("$HTTP_ADDRESS:${port}$url").openConnection() as HttpURLConnection) {
            requestMethod = method
            readTimeout = timeoutMs.toInt()
            setRequestProperty("Content-Type", contentType)
            if (encodeBlock != null) {
                doOutput = true
                encodeBlock(outputStream)
                outputStream.close()
            }

            if (responseCode != 200) {
                val exceptionMessage = "${responseCode}:${responseMessage}."
                if (usingProxy()) {
                    throw IllegalStateException(
                        "$exceptionMessage " +
                            "The request executed with proxying, " +
                            "perhaps http-proxy are configured on device incorrectly. " +
                            "Please, try to add ${HTTP_ADDRESS}:${port} to exclusion list, " +
                            "or to verify other routing rules."
                    )
                }

                throw IllegalStateException(exceptionMessage)
            }

            return decodeBlock(inputStream)
        }
    }
}
