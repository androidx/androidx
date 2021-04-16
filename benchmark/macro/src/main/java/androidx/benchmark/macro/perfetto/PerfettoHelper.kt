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

import android.os.SystemClock
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.benchmark.macro.DeviceInfo.deviceSummaryString
import androidx.benchmark.macro.device
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException

/**
 * PerfettoHelper is used to start and stop the perfetto tracing and move the
 * output perfetto trace file to destination folder.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PerfettoHelper {

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
                    throw perfettoStartupException("Unable to stop Perfetto trace capture", null)
                }
            }

            // Remove already existing temporary output trace file if any.
            val output = device.executeShellCommand("rm $PERFETTO_TMP_OUTPUT_FILE")
            Log.i(LOG_TAG, "Perfetto output file cleanup - $output")
            // Start perfetto tracing. Note that we need to use executeShellScript to be able to
            // pipe the input via cat, as **Perfetto cannot read from the filesystem without root**.
            var perfettoCmd =
                "cat $configFilePath | perfetto --background -c - -o $PERFETTO_TMP_OUTPUT_FILE"
            if (isTextProtoConfig) {
                perfettoCmd += PERFETTO_TXT_PROTO_ARG
            }
            Log.i(LOG_TAG, "Starting perfetto tracing with cmd: $perfettoCmd")
            val startOutput = device.executeShellScript(perfettoCmd, null)
            Log.i(LOG_TAG, "Perfetto start command output - $startOutput")
            // TODO : Once the output status is available use that for additional validation.
            if (!isPerfettoRunning()) {
                throw perfettoStartupException(
                    "Perfetto tracing failed to start. Command output = $startOutput", null
                )
            }
        } catch (ioe: IOException) {
            throw perfettoStartupException("Unable to start perfetto tracing", ioe)
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
            if (stopPerfetto()) {
                Log.i(LOG_TAG, "Writing to $destinationFile")
                if (!copyFileOutput(destinationFile)) {
                    return false
                }
            } else {
                Log.e(LOG_TAG, "Perfetto failed to stop.")
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
    public fun stopPerfetto(): Boolean {
        val stopOutput = device.executeShellCommand("kill -INT ${perfettoPid()}")
        Log.i(LOG_TAG, "Perfetto stop command output - $stopOutput")
        var waitCount = 0
        while (isPerfettoRunning()) {
            // 60 secs timeout for perfetto shutdown.
            if (waitCount < PERFETTO_KILL_WAIT_COUNT) {
                // Check every 5 secs if perfetto stopped successfully.
                SystemClock.sleep(PERFETTO_KILL_WAIT_TIME)
                waitCount++
                continue
            }
            return false
        }
        Log.i(LOG_TAG, "Perfetto stopped successfully.")
        return true
    }

    /**
     * Returns perfetto process pid if running, or null otherwise.
     */
    private fun perfettoPid(): String? {
        return try {
            val perfettoProcId = device.executeShellCommand(PERFETTO_PROC_ID_CMD)
            Log.i(LOG_TAG, String.format("Perfetto process id - %s", perfettoProcId))
            if (perfettoProcId.isEmpty()) {
                null
            } else perfettoProcId
        } catch (ioe: IOException) {
            Log.e(LOG_TAG, "Not able to check the perfetto status due to:" + ioe.message, ioe)
            null
        }
    }

    /**
     * Check if perfetto process is running or not.
     *
     * @return true if perfetto is running otherwise false.
     */
    public fun isPerfettoRunning(): Boolean {
        return perfettoPid() != null
    }

    /**
     * @return the [String] path to the temporary output file used to store the trace file
     * during collection.
     */
    private fun getPerfettoTmpOutputFilePath(): String {
        return PERFETTO_TMP_OUTPUT_FILE
    }

    /**
     * Copy the temporary perfetto trace output file from /data/local/tmp/trace_output.pb to given
     * destinationFile.
     *
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace file copied successfully otherwise false.
     */
    private fun copyFileOutput(destinationFile: String): Boolean {
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
                device.executeShellCommand("mv $PERFETTO_TMP_OUTPUT_FILE $destinationFile")
            if (moveResult.isNotEmpty()) {
                Log.e(
                    LOG_TAG,
                    """
                        Unable to move perfetto output file from $PERFETTO_TMP_OUTPUT_FILE
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

        // Command to check the perfetto process id.
        private const val PERFETTO_PROC_ID_CMD = "pidof perfetto"

        // Max wait count for checking if perfetto is stopped successfully
        private const val PERFETTO_KILL_WAIT_COUNT = 12

        // Check if perfetto is stopped every 5 secs.
        private const val PERFETTO_KILL_WAIT_TIME: Long = 5000
    }
}
