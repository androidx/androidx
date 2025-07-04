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

package androidx.benchmark.macro

import android.annotation.SuppressLint
import android.os.Build
import android.security.NetworkSecurityPolicy
import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.InMemoryTracing
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.Profiler
import androidx.benchmark.Shell
import androidx.benchmark.ShellScript
import androidx.benchmark.StartedShellScript
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.ServerLifecycleManager
import androidx.benchmark.traceprocessor.TraceProcessor
import java.io.IOException

@RequiresApi(24)
private object Api24Impl {
    fun isCleartextTrafficPermittedForLocalhost() =
        NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("localhost")
}

internal class ShellServerLifecycleManager : ServerLifecycleManager {
    companion object {
        private const val SERVER_PROCESS_NAME = "trace_processor_shell"

        internal val shellPath: String by lazy {
            // Checks for ABI support
            PerfettoHelper.createExecutable(SERVER_PROCESS_NAME)
        }
        internal const val PORT = 9001
    }

    private var shellScript: ShellScript? = null
    private var startedShellScript: StartedShellScript? = null
    private var processId: Int? = null

    /**
     * Returns a cached instance of the shell script to run the perfetto trace shell processor as
     * http server. Note that the generated script doesn't specify the port and this must be passed
     * as parameter when running the script.
     */
    private fun getOrCreateShellScript(): ShellScript =
        shellScript
            ?: synchronized(this) {
                var instance = shellScript
                if (instance != null) {
                    return@synchronized instance
                }
                val script = "echo pid:$$ ; exec $shellPath -D --http-port \"$PORT\""
                instance = Shell.createShellScript(script)
                shellScript = instance
                instance
            }

    @SuppressLint("BanThreadSleep")
    override fun start(): Int {
        inMemoryTrace("ShellServerLifecycleManager#start") {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
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
            """
                        .trimIndent()
                )
            }

            getOrCreateShellScript().start().apply {
                processId =
                    stdOutLineSequence().first { it.startsWith("pid:") }.split("pid:")[1].toInt()
                startedShellScript = this
                println("Started, processId $processId")
            }
        }
        return PORT
    }

    override fun timeoutMessage(): String {

        // In the event that the instrumentation app cannot connect to the
        // trace_processor_shell server, trying to read the full stderr may make the
        // process hang. Here we check if the process is still running to determine if
        // that's the case and throw the correct exception.

        val processRunning =
            processId?.let { Shell.isProcessAlive(it, SERVER_PROCESS_NAME) } ?: false

        return if (processRunning) {
            "The instrumentation app cannot connect to the trace_processor_shell server."
        } else {
            "Perfetto trace_processor_shell did not start correctly." +
                " Stderr = ${startedShellScript?.getOutputAndClose()?.stderr}"
        }
    }

    override fun stop() {
        inMemoryTrace("ShellServerLifecycleManager#stop") {
            if (processId == null) {
                Log.w(TAG, "Tried to stop trace shell processor http server without starting it.")
                return
            }
            println("stop, processId=$processId")
            Shell.executeScriptSilent("kill -TERM $processId")
            Log.i(TAG, "Perfetto trace processor shell server stopped (pid=$processId).")
            processId = null
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> TraceProcessor.Companion.runSingleSessionServer(
    absoluteTracePath: String,
    block: TraceProcessor.Session.() -> T,
) = TraceProcessor.runServer { loadTrace(PerfettoTrace(absoluteTracePath), block) }

/**
 * Starts a Perfetto TraceProcessor shell server in http mode.
 *
 * The server is stopped after the block is complete.
 *
 * @sample androidx.benchmark.samples.traceProcessorRunServerSimple
 * @param timeoutMs maximum duration in milliseconds for waiting for operations like loading the
 *   server, or querying a trace.
 * @param block Command to execute using trace processor
 */
@JvmOverloads
@ExperimentalTraceProcessorApi
fun <T> TraceProcessor.Companion.runServer(
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    block: TraceProcessor.() -> T,
): T = startServer(timeoutMs).use { block(it.traceProcessor) }

/**
 * Starts a Perfetto TraceProcessor shell server in http mode.
 *
 * @sample androidx.benchmark.samples.traceProcessorStartServerSimple
 * @param timeoutMs maximum duration in milliseconds for waiting for operations like loading the
 *   server, or querying a trace.
 */
@JvmOverloads
@ExperimentalTraceProcessorApi
@CheckResult
fun TraceProcessor.Companion.startServer(
    timeoutMs: Long = DEFAULT_TIMEOUT_MS
): TraceProcessor.Handle =
    startServer(
        ShellServerLifecycleManager(),
        eventCallback =
            object : TraceProcessor.EventCallback {
                override fun onLoadTraceFailure(trace: PerfettoTrace, throwable: Throwable) {
                    // TODO: consider a label argument to control logging like this in the success
                    //  case as well, which lets us get rid of FileLinkingRule (which doesn't work
                    //  well anyway)
                    if (trace.path.startsWith(Outputs.outputDirectory.absolutePath)) {
                        // only link trace with failure to Studio if it's an output file
                        InstrumentationResults.instrumentationReport {
                            val label =
                                "Trace with processing error: ${
                                throwable.message?.take(50)?.trim()
                            }..."
                            reportSummaryToIde(
                                profilerResults =
                                    listOf(
                                        Profiler.ResultFile.ofPerfettoTrace(
                                            label = label,
                                            absolutePath = trace.path,
                                        )
                                    )
                            )
                        }
                    }
                }
            },
        tracer =
            object : TraceProcessor.Tracer() {
                override fun beginTraceSection(label: String) {
                    InMemoryTracing.beginSection(label)
                }

                override fun endTraceSection() {
                    InMemoryTracing.endSection()
                }
            },
        timeoutMs = timeoutMs,
    )
