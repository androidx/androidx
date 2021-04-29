/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.macro.DeviceInfo.deviceSummaryString
import androidx.benchmark.macro.device
import androidx.test.platform.app.InstrumentationRegistry
import org.jetbrains.annotations.TestOnly
import java.io.File
import java.io.IOException

/**
 * PerfettoHelper is used to start and stop the perfetto tracing and move the
 * output perfetto trace file to destination folder.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public class PerfettoHelper(private val unbundled: Boolean = Build.VERSION.SDK_INT in 21..28) {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = instrumentation.device()

    private fun perfettoStartupException(label: String, cause: Exception?): IllegalStateException {
        return IllegalStateException(
            """
            $label
            Please report a bug, and include a logcat capture of the test run and failure.
            $deviceSummaryString
            """.trimIndent(),
            cause
        )
    }

    /**
     * Start the perfetto tracing in background using the given config file.
     *
     * The output will be written to /data/misc/perfetto-traces/trace_output.pb. Perfetto has
     * write access only to /data/misc/perfetto-traces/ folder. The config file may be anywhere
     * readable by shell.
     *
     * @param configFilePath used for collecting the perfetto trace.
     * @param isTextProtoConfig true if the config file is textproto format otherwise false.
     */
    public fun startCollecting(configFilePath: String, isTextProtoConfig: Boolean) {
        require(configFilePath.isNotEmpty()) {
            "Perfetto config cannot be empty."
        }

        try {
            // Cleanup already existing perfetto process.
            Log.i(LOG_TAG, "Cleanup perfetto before starting.")
            if (isPerfettoRunning()) {
                Log.i(LOG_TAG, "Perfetto tracing is already running. Stopping perfetto.")
                if (!stopPerfetto()) {
                    throw perfettoStartupException(
                        "Unable to stop Perfetto trace capture",
                        null
                    )
                }
            }

            // The actual location of the config path.
            val actualConfigPath = if (unbundled) {
                val path = "$UNBUNDLED_PERFETTO_ROOT_DIR/config.pb"
                // Move the config to a directory that unbundled perfetto has permissions for.
                device.executeShellCommand("rm $path")
                device.executeShellCommand("mv $configFilePath $path")
                path
            } else {
                configFilePath
            }

            val outputPath = getPerfettoTmpOutputFilePath()
            // Remove already existing temporary output trace file if any.
            val output = device.executeShellCommand("rm $outputPath")
            Log.i(LOG_TAG, "Perfetto output file cleanup - $output")

            // Setup `traced` and `traced_probes` if necessary.
            setupTracedAndProbes()

            // Perfetto
            val perfettoCmd = perfettoCommand(actualConfigPath, isTextProtoConfig)
            Log.i(LOG_TAG, "Starting perfetto tracing with cmd: $perfettoCmd")
            val perfettoCmdOutput = device.executeShellScript(perfettoCmd)
            Log.i(LOG_TAG, "Perfetto pid - $perfettoCmdOutput")
        } catch (ioe: IOException) {
            throw perfettoStartupException("Unable to start perfetto tracing", ioe)
        }

        if (!isPerfettoRunning()) {
            throw perfettoStartupException("Perfetto tracing failed to start. ", null)
        }

        Log.i(LOG_TAG, "Perfetto tracing started successfully.")
    }

    /**
     * Stop the perfetto trace collection under /data/misc/perfetto-traces/trace_output.pb after
     * waiting for given time in msecs and copy the output to the destination file.
     *
     * @param waitTimeInMsecs time to wait in msecs before stopping the trace collection.
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace collection is successful otherwise false.
     */
    public fun stopCollecting(waitTimeInMsecs: Long, destinationFile: String): Boolean {
        // Wait for the dump interval before stopping the trace.
        Log.i(LOG_TAG, "Waiting for $waitTimeInMsecs millis before stopping perfetto.")
        SystemClock.sleep(waitTimeInMsecs)

        // Stop the perfetto and copy the output file.
        Log.i(LOG_TAG, "Stopping perfetto.")
        try {
            var stopped = stopPerfetto()
            if (unbundled) {
                Log.i(LOG_TAG, "Stopping `traced` and `traced_probes`.")
                stopped = stopped.or(stopProcess(getProcessId(TRACED)))
                stopped = stopped.or(stopProcess(getProcessId(TRACED_PROBES)))
            }
            if (!stopped) {
                Log.e(LOG_TAG, "Perfetto failed to stop.")
                return false
            }
            Log.i(LOG_TAG, "Writing to $destinationFile.")
            if (!copyFileOutput(destinationFile)) {
                return false
            }
        } catch (ioe: IOException) {
            Log.e(LOG_TAG, "Unable to stop the perfetto tracing due to " + ioe.message, ioe)
            return false
        }
        return true
    }

    /**
     * Utility method for stopping perfetto.
     *
     * @return true if perfetto is stopped successfully.
     */
    @Throws(IOException::class)
    public fun stopPerfetto(): Boolean = stopProcess(getProcessId(PERFETTO))

    /**
     * Check if perfetto process is running or not.
     *
     * @return true if perfetto is running otherwise false.
     */
    public fun isPerfettoRunning(): Boolean {
        val pid = getProcessId(PERFETTO)
        return !pid.isNullOrEmpty()
    }

    /**
     * Sets up `traced` and `traced_probes` if necessary.
     */
    private fun setupTracedAndProbes() {
        if (!unbundled) {
            return
        }

        // Run `traced` and `traced_probes` in background mode.

        // Setup traced
        val tracedCmd = "$UNBUNDLED_ENV_PREFIX $tracedShellPath --background"
        Log.i(LOG_TAG, "Starting traced cmd: $tracedCmd")
        device.executeShellScript(tracedCmd)

        // Setup traced_probes
        val tracedProbesCmd = "$UNBUNDLED_ENV_PREFIX $tracedProbesShellPath --background"
        Log.i(LOG_TAG, "Starting traced_probes cmd: $tracedProbesCmd")
        device.executeShellScript(tracedProbesCmd)
    }

    /**
     * @return the shell command that can be used to start Perfetto.
     */
    private fun perfettoCommand(configFilePath: String, isTextProtoConfig: Boolean): String {
        val outputPath = getPerfettoTmpOutputFilePath()
        var command = if (!unbundled) (
            // Bundled perfetto reads configuration from stdin.
            "cat $configFilePath | perfetto --background -c - -o $outputPath"
            ) else {
            // Unbundled perfetto can read configuration from a file that it has permissions to
            // read from. This because it assumes the identity of the shell and therefore has
            // access to /data/local/tmp directory.
            "$UNBUNDLED_ENV_PREFIX $perfettoShellPath --background" +
                " -c $configFilePath" +
                " -o $outputPath"
        }

        if (isTextProtoConfig) {
            command += PERFETTO_TXT_PROTO_ARG
        }
        return command
    }

    /**
     * @return the [String] path to the temporary output file used to store the trace file
     * during collection.
     */
    private fun getPerfettoTmpOutputFilePath(): String {
        return if (unbundled) {
            UNBUNDLED_TEMP_OUTPUT_FILE
        } else {
            PERFETTO_TMP_OUTPUT_FILE
        }
    }

    /**
     * @return the [String] process id for a given process name.
     */
    private fun getProcessId(processName: String): String? {
        return try {
            val processId = device.executeShellCommand("pidof $processName")
            // We want to pick the most recent invocation of the command.
            // This is because we may have more than once instance of the process.
            val pid = processId.split(" ").lastOrNull()?.trim()
            Log.d(LOG_TAG, "Process id is $pid")
            pid
        } catch (ioe: IOException) {
            Log.i(LOG_TAG, "Unable to check process status due to $ioe.", ioe)
            null
        }
    }

    /**
     * Utility method for stopping a process with a given `pid`.
     *
     * @return true if the process was stopped successfully.
     */
    private fun stopProcess(pid: String?): Boolean {
        val stopOutput = device.executeShellCommand("kill -TERM $pid")
        Log.i(LOG_TAG, "Stop command output - $stopOutput")
        var waitCount = 0
        while (isProcessRunning(pid)) {
            Log.d(LOG_TAG, "Process ($pid) is running")
            // 60 secs timeout for perfetto shutdown.
            if (waitCount < PERFETTO_KILL_WAIT_COUNT) {
                // Check every 5 secs if process stopped successfully.
                SystemClock.sleep(PERFETTO_KILL_WAIT_TIME)
                waitCount++
                continue
            }
            return false
        }
        Log.i(LOG_TAG, "Process stopped successfully.")
        return true
    }

    /**
     * Utility method for checking if a process with a given `pid` is still running.
     *
     * @return true if still running.
     */
    @Throws(IOException::class)
    private fun isProcessRunning(pid: String?): Boolean {
        if (pid.isNullOrEmpty()) {
            return false
        }

        Log.d(LOG_TAG, "Checking if $pid is running")
        val output = device.executeShellCommand("ps -A $pid")
        return output.contains(pid)
    }

    /**
     * Copy the temporary perfetto trace output file from /data/local/tmp/trace_output.pb to given
     * destinationFile.
     *
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace file copied successfully otherwise false.
     */
    private fun copyFileOutput(destinationFile: String): Boolean {
        val sourceFile = getPerfettoTmpOutputFilePath()
        val filePath = File(destinationFile)
        val destDirectory = filePath.parent
        if (destDirectory != null) {
            // Check if the directory already exists
            val directory = File(destDirectory)
            if (!directory.exists()) {
                val success = directory.mkdirs()
                if (!success) {
                    Log.e(
                        LOG_TAG,
                        "Result output directory $destDirectory not created successfully."
                    )
                    return false
                }
            }
        }

        // Copy the collected trace from /data/misc/perfetto-traces/trace_output.pb to
        // destinationFile
        try {
            val moveResult =
                device.executeShellCommand("mv $sourceFile $destinationFile")
            if (moveResult.isNotEmpty()) {
                Log.e(
                    LOG_TAG,
                    """
                        Unable to move perfetto output file from $sourceFile
                        to $destinationFile due to $moveResult.
                    """.trimIndent()
                )
                return false
            }
        } catch (ioe: IOException) {
            Log.e(
                LOG_TAG,
                "Unable to move the perfetto trace file to destination file.",
                ioe
            )
            return false
        }
        return true
    }

    internal companion object {
        internal const val LOG_TAG = "PerfettoCapture"
        // Command to start the perfetto tracing in the background.
        // perfetto --background -c /data/misc/perfetto-traces/trace_config.pb -o
        // /data/misc/perfetto-traces/trace_output.pb
        private const val PERFETTO_TMP_OUTPUT_FILE = "/data/misc/perfetto-traces/trace_output.pb"

        // Additional arg to indicate that the perfetto config file is text format.
        private const val PERFETTO_TXT_PROTO_ARG = " --txt"

        // Max wait count for checking if perfetto is stopped successfully
        private const val PERFETTO_KILL_WAIT_COUNT = 12

        // Check if perfetto is stopped every 5 secs.
        private const val PERFETTO_KILL_WAIT_TIME: Long = 5000

        // Path where perfetto, traced, and traced_probes are copied to if API >= 21 and < 29
        private const val UNBUNDLED_PERFETTO_ROOT_DIR = "/data/local/tmp"

        private const val UNBUNDLED_TEMP_OUTPUT_FILE =
            "$UNBUNDLED_PERFETTO_ROOT_DIR/trace_output.pb"

        // The environment variables necessary for unbundled perfetto (unnamed domain sockets).
        // We need unnamed sockets here because SELinux dictates that we cannot use real, file
        // based, domain sockets on Platform versions prior to S.
        private const val UNBUNDLED_ENV_PREFIX =
            "PERFETTO_PRODUCER_SOCK_NAME=@macrobenchmark_producer " +
                "PERFETTO_CONSUMER_SOCK_NAME=@macrobenchmark_consumer"

        // A set of supported ABIs
        private val SUPPORTED_64_ABIS = setOf("arm64-v8a", "x86_64")
        private val SUPPORTED_32_ABIS = setOf("armeabi")

        // Perfetto executable
        private const val PERFETTO = "perfetto"

        // Trace daemon
        private const val TRACED = "traced"

        // Traced probes
        private const val TRACED_PROBES = "traced_probes"

        @TestOnly
        fun isAbiSupported(): Boolean {
            Log.d(LOG_TAG, "Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            // Cuttlefish is x86 but claims support for x86_64
            return !Build.MODEL.contains("Cuttlefish") && ( // b/180022458
                Build.SUPPORTED_64_BIT_ABIS.any { SUPPORTED_64_ABIS.contains(it) } ||
                    Build.SUPPORTED_32_BIT_ABIS.any { SUPPORTED_32_ABIS.contains(it) }
                )
        }

        @get:TestOnly
        val tracedProbesShellPath: String by lazy {
            createExecutable(TRACED_PROBES)
        }

        @get:TestOnly
        val tracedShellPath: String by lazy {
            createExecutable(TRACED)
        }

        @get:TestOnly
        val perfettoShellPath: String by lazy {
            createExecutable(PERFETTO)
        }

        internal fun createExecutable(tool: String): String {
            if (!isAbiSupported()) {
                throw IllegalStateException(
                    "Unsupported ABI (${Build.SUPPORTED_ABIS.joinToString()})"
                )
            }
            val suffix = when {
                // The order is important because `SUPPORTED_64_BIT_ABIS` lists all ABI supported
                // by a device. That is why we need to search from most specific to least specific.
                // For e.g. emulators claim to support aarch64, when in reality they can only
                // support x86 or x86_64.
                Build.SUPPORTED_64_BIT_ABIS.any { it.startsWith("x86_64") } -> "x86_64"
                Build.SUPPORTED_64_BIT_ABIS.any { it.startsWith("arm64") } -> "aarch64"
                Build.SUPPORTED_32_BIT_ABIS.any { it.startsWith("armeabi") } -> "arm"
                else -> IllegalStateException(
                    // Perfetto does not support x86 binaries
                    "Unsupported ABI (${Build.SUPPORTED_ABIS.joinToString()})"
                )
            }
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val inputStream = instrumentation.context.assets.open("${tool}_$suffix")
            val device = instrumentation.device()
            return device.createRunnableExecutable(tool, inputStream)
        }
    }
}
