/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.benchmark.perfetto;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PerfettoHelper is used to start and stop the perfetto tracing and move the
 * output perfetto trace file to destination folder.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(28)
public class PerfettoHelper {
    private static final String LOG_TAG = PerfettoHelper.class.getSimpleName();
    // Command to start the perfetto tracing in the background.
    // perfetto -b -c /data/misc/perfetto-traces/trace_config.pb -o
    // /data/misc/perfetto-traces/trace_output.pb
    private static final String PERFETTO_TMP_OUTPUT_FILE =
            "/data/misc/perfetto-traces/trace_output.pb";
    // Additional arg to indicate that the perfetto config file is text format.
    private static final String PERFETTO_TXT_PROTO_ARG = " --txt";
    // Command to check the perfetto process id.
    private static final String PERFETTO_PROC_ID_CMD = "pidof perfetto";
    // Command to move the perfetto output trace file to given folder.
    private static final String MOVE_CMD = "mv %s %s";
    // Max wait count for checking if perfetto is stopped successfully
    private static final int PERFETTO_KILL_WAIT_COUNT = 12;
    // Check if perfetto is stopped every 5 secs.
    private static final long PERFETTO_KILL_WAIT_TIME = 5000;

    private final UiDevice mUIDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    /**
     * Start the perfetto tracing in background using the given config file.
     *
     * The output will be written to /data/misc/perfetto-traces/trace_output.pb. Perfetto has
     * write access only to /data/misc/perfetto-traces/ folder. The config file may be anywhere
     * readable by shell.
     *
     * @param configFilePath used for collecting the perfetto trace.
     * @param isTextProtoConfig true if the config file is textproto format otherwise false.
     * @return true if trace collection started successfully otherwise return false.
     */
    public boolean startCollecting(@Nullable String configFilePath, boolean isTextProtoConfig) {
        if (configFilePath == null || configFilePath.isEmpty()) {
            Log.e(LOG_TAG, "Perfetto config file name is null or empty.");
            return false;
        }
        try {
            // Cleanup already existing perfetto process.
            Log.i(LOG_TAG, "Cleanup perfetto before starting.");
            if (isPerfettoRunning()) {
                Log.i(LOG_TAG, "Perfetto tracing is already running. Stopping perfetto.");
                if (!stopPerfetto()) {
                    return false;
                }
            }

            // Remove already existing temporary output trace file if any.
            String output = mUIDevice.executeShellCommand("rm " + PERFETTO_TMP_OUTPUT_FILE);
            Log.i(LOG_TAG, String.format("Perfetto output file cleanup - %s", output));

            // Start perfetto tracing. Note that we need to use executeShellScript to be able to
            // pipe the input via cat, as **Perfetto cannot read from the filesystem without root**.
            String perfettoCmd = "cat " + configFilePath
                    + " | perfetto --background -c - -o " + PERFETTO_TMP_OUTPUT_FILE;
            if (isTextProtoConfig) {
                perfettoCmd = perfettoCmd + PERFETTO_TXT_PROTO_ARG;
            }
            Log.i(LOG_TAG, "Starting perfetto tracing with cmd: " + perfettoCmd);
            String startOutput = ShellUtilsKt.executeShellScript(mUIDevice, perfettoCmd, null);

            Log.i(LOG_TAG, String.format("Perfetto start command output - %s", startOutput));
            // TODO : Once the output status is available use that for additional validation.
            if (!isPerfettoRunning()) {
                Log.e(LOG_TAG, "Perfetto tracing failed to start.");
                return false;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to start the perfetto tracing due to :" + ioe.getMessage());
            return false;
        }
        Log.i(LOG_TAG, "Perfetto tracing started successfully.");
        return true;
    }

    /**
     * Stop the perfetto trace collection under /data/misc/perfetto-traces/trace_output.pb after
     * waiting for given time in msecs and copy the output to the destination file.
     *
     * @param waitTimeInMsecs time to wait in msecs before stopping the trace collection.
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace collection is successful otherwise false.
     */
    public boolean stopCollecting(long waitTimeInMsecs, @NonNull String destinationFile) {
        // Wait for the dump interval before stopping the trace.
        Log.i(LOG_TAG, String.format(
                "Waiting for %d msecs before stopping perfetto.", waitTimeInMsecs));
        SystemClock.sleep(waitTimeInMsecs);

        // Stop the perfetto and copy the output file.
        Log.i(LOG_TAG, "Stopping perfetto.");
        try {
            if (stopPerfetto()) {
                Log.i(LOG_TAG, String.format("Writing to %s.", destinationFile));
                if (!copyFileOutput(destinationFile)) {
                    return false;
                }
            } else {
                Log.e(LOG_TAG, "Perfetto failed to stop.");
                return false;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Unable to stop the perfetto tracing due to " + ioe.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Utility method for stopping perfetto.
     *
     * @return true if perfetto is stopped successfully.
     */
    public boolean stopPerfetto() throws IOException {
        String stopOutput = mUIDevice.executeShellCommand("kill -INT " + perfettoPid());
        Log.i(LOG_TAG, String.format("Perfetto stop command output - %s", stopOutput));
        int waitCount = 0;
        while (isPerfettoRunning()) {
            // 60 secs timeout for perfetto shutdown.
            if (waitCount < PERFETTO_KILL_WAIT_COUNT) {
                // Check every 5 secs if perfetto stopped successfully.
                SystemClock.sleep(PERFETTO_KILL_WAIT_TIME);
                waitCount++;
                continue;
            }
            return false;
        }
        Log.i(LOG_TAG, "Perfetto stopped successfully.");
        return true;
    }

    /**
     * Returns perfetto process pid if running, or null otherwise.
     */
    private String perfettoPid() {
        try {
            String perfettoProcId = mUIDevice.executeShellCommand(PERFETTO_PROC_ID_CMD);
            Log.i(LOG_TAG, String.format("Perfetto process id - %s", perfettoProcId));
            if (perfettoProcId.isEmpty()) {
                return null;
            }
            return perfettoProcId;
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Not able to check the perfetto status due to:" + ioe.getMessage());
            return null;
        }
    }

    /**
     * Check if perfetto process is running or not.
     *
     * @return true if perfetto is running otherwise false.
     */
    public boolean isPerfettoRunning() {
        return perfettoPid() != null;
    }

    /**
     * @return the {@link String} path to the temporary output file used to store the trace file
     * during collection.
     */
    @NonNull
    public static String getPerfettoTmpOutputFilePath() {
        return PERFETTO_TMP_OUTPUT_FILE;
    }

    /**
     * Copy the temporary perfetto trace output file from /data/misc/perfetto-traces/ to given
     * destinationFile.
     *
     * @param destinationFile file to copy the perfetto output trace.
     * @return true if the trace file copied successfully otherwise false.
     */
    private boolean copyFileOutput(@NonNull String destinationFile) {
        Path path = Paths.get(destinationFile);
        String destDirectory = path.getParent().toString();
        // Check if the directory already exists
        File directory = new File(destDirectory);
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                Log.e(LOG_TAG, String.format(
                        "Result output directory %s not created successfully.", destDirectory));
                return false;
            }
        }

        // Copy the collected trace from /data/misc/perfetto-traces/trace_output.pb to
        // destinationFile
        try {
            String moveResult = mUIDevice.executeShellCommand(String.format(
                    MOVE_CMD, PERFETTO_TMP_OUTPUT_FILE, destinationFile));
            if (!moveResult.isEmpty()) {
                Log.e(LOG_TAG, String.format(
                        "Unable to move perfetto output file from %s to %s due to %s",
                        PERFETTO_TMP_OUTPUT_FILE, destinationFile, moveResult));
                return false;
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG,
                    "Unable to move the perfetto trace file to destination file."
                            + ioe.getMessage());
            return false;
        }
        return true;
    }
}
