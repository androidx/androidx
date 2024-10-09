/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.benchmark.simpleperf;

import android.annotation.SuppressLint;
import android.os.Build;
import android.system.Os;
import android.system.OsConstants;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * This class uses `simpleperf record` cmd to generate a recording file.
 * It allows users to start recording with some options, pause/resume recording
 * to only profile interested code, and stop recording.
 * </p>
 *
 * <p>
 * Example:
 *   RecordOptions options = new RecordOptions();
 *   options.setDwarfCallGraph();
 *   ProfileSession session = new ProfileSession();
 *   session.StartRecording(options);
 *   Thread.sleep(1000);
 *   session.PauseRecording();
 *   Thread.sleep(1000);
 *   session.ResumeRecording();
 *   Thread.sleep(1000);
 *   session.StopRecording();
 * </p>
 *
 * <p>
 * It throws an Error when error happens. To read error messages of simpleperf record
 * process, filter logcat with `simpleperf`.
 * </p>
 *
 * NOTE: copied from
 * https://cs.android.com/android/platform/superproject/+/master:system/extras/simpleperf/app_api/
 *
 */
@SuppressWarnings({"IOStreamConstructor", "CatchMayIgnoreException",
        "IfStatementMissingBreakInLoop", "ResultOfMethodCallIgnored", "StringConcatenationInLoop",
        "unused"})
@RequiresApi(28)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanSynchronizedMethods")
public class ProfileSession {
    private static final String SIMPLEPERF_PATH_IN_IMAGE = "/system/bin/simpleperf";

    enum State {
        NOT_YET_STARTED,
        STARTED,
        PAUSED,
        STOPPED,
    }

    private State mState = State.NOT_YET_STARTED;
    private final String mAppDataDir;
    private String mSimpleperfPath;
    private final String mSimpleperfDataDir;
    private Process mSimpleperfProcess;
    private boolean mTraceOffCpu = false;

    /**
     * @param appDataDir the same as android.content.Context.getDataDir().
     *                   ProfileSession stores profiling data in appDataDir/simpleperf_data/.
     */
    public ProfileSession(@NonNull String appDataDir) {
        mAppDataDir = appDataDir;
        mSimpleperfDataDir = appDataDir + "/simpleperf_data";
    }

    /**
     * ProfileSession assumes appDataDir as /data/data/app_package_name.
     */
    public ProfileSession() {
        String packageName;
        try {
            String s = readInputStream(new FileInputStream("/proc/self/cmdline"));
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\0') {
                    s = s.substring(0, i);
                    break;
                }
            }
            packageName = s;
        } catch (IOException e) {
            throw new Error("failed to find packageName: " + e.getMessage());
        }
        if (packageName.isEmpty()) {
            throw new Error("failed to find packageName");
        }
        final int aidUserOffset = 100000;
        int uid = Os.getuid();
        if (uid >= aidUserOffset) {
            int user_id = uid / aidUserOffset;
            mAppDataDir = "/data/user/" + user_id + "/" + packageName;
        } else {
            mAppDataDir = "/data/data/" + packageName;
        }
        mSimpleperfDataDir = mAppDataDir + "/simpleperf_data";
    }

    /**
     * Start recording.
     * @param options RecordOptions
     */
    public void startRecording(@NonNull RecordOptions options) {
        startRecording(options.toRecordArgs());
    }

    /**
     * Start recording.
     * @param args arguments for `simpleperf record` cmd.
     */
    public synchronized void startRecording(@NonNull List<String> args) {
        if (mState != State.NOT_YET_STARTED) {
            throw new IllegalStateException("startRecording: session in wrong state " + mState);
        }
        for (String arg : args) {
            if (arg.equals("--trace-offcpu")) {
                mTraceOffCpu = true;
            }
        }
        mSimpleperfPath = findSimpleperf();
        checkIfPerfEnabled();
        createSimpleperfDataDir();
        startRecordingProcess(args);
        mState = State.STARTED;
    }

    /**
     * Pause recording. No samples are generated in paused state.
     */
    public synchronized void pauseRecording() {
        if (mState != State.STARTED) {
            throw new IllegalStateException("pauseRecording: session in wrong state " + mState);
        }
        if (mTraceOffCpu) {
            throw new AssertionError(
                    "--trace-offcpu option doesn't work well with pause/resume recording");
        }
        sendCmd("pause");
        mState = State.PAUSED;
    }

    /**
     * Resume a paused session.
     */
    public synchronized void resumeRecording() {
        if (mState != State.PAUSED) {
            throw new IllegalStateException("resumeRecording: session in wrong state " + mState);
        }
        sendCmd("resume");
        mState = State.STARTED;
    }

    /**
     * Stop recording and generate a recording file under appDataDir/simpleperf_data/.
     */
    public synchronized void stopRecording() {
        if (mState != State.STARTED && mState != State.PAUSED) {
            throw new IllegalStateException("stopRecording: session in wrong state " + mState);
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P + 1
                && mSimpleperfPath.equals(SIMPLEPERF_PATH_IN_IMAGE)) {
            // The simpleperf shipped on Android Q contains a bug, which may make it abort if
            // calling simpleperfProcess.destroy().
            destroySimpleperfProcessWithoutClosingStdin();
        } else {
            mSimpleperfProcess.destroy();
        }
        waitForSimpleperfProcess();
        mState = State.STOPPED;
    }

    private void destroySimpleperfProcessWithoutClosingStdin() {
        // In format "Process[pid=? ..."
        String s = mSimpleperfProcess.toString();
        final String prefix = "Process[pid=";
        if (s.startsWith(prefix)) {
            int startIndex = prefix.length();
            int endIndex = s.indexOf(',');
            if (endIndex > startIndex) {
                int pid = Integer.parseInt(s.substring(startIndex, endIndex).trim());
                android.os.Process.sendSignal(pid, OsConstants.SIGTERM);
                return;
            }
        }
        mSimpleperfProcess.destroy();
    }

    private String readInputStream(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String result = reader.lines().collect(Collectors.joining("\n"));
        try {
            reader.close();
        } catch (IOException e) {
        }
        return result;
    }

    public @NonNull String findSimpleperf() {
        // 1. Try /data/local/tmp/simpleperf. Probably it's newer than /system/bin/simpleperf.
        String simpleperfPath = findSimpleperfInTempDir();
        if (simpleperfPath != null) {
            return simpleperfPath;
        }
        // 2. Try /system/bin/simpleperf, which is available on Android >= Q.
        simpleperfPath = SIMPLEPERF_PATH_IN_IMAGE;
        if (isExecutableFile(simpleperfPath)) {
            return simpleperfPath;
        }
        throw new Error("can't find simpleperf on device. Please run api_profiler.py.");
    }

    private boolean isExecutableFile(@NonNull String path) {
        File file = new File(path);
        return file.canExecute();
    }

    private @Nullable String findSimpleperfInTempDir() {
        String path = "/data/local/tmp/simpleperf";
        File file = new File(path);
        if (!file.isFile()) {
            return null;
        }
        // Copy it to app dir to execute it.
        String toPath = mAppDataDir + "/simpleperf";
        try {
            Process process = new ProcessBuilder()
                    .command("cp", path, toPath).start();
            process.waitFor();
        } catch (Exception e) {
            return null;
        }
        if (!isExecutableFile(toPath)) {
            return null;
        }
        // For apps with target sdk >= 29, executing app data file isn't allowed.
        // For android R, app context isn't allowed to use perf_event_open.
        // So test executing downloaded simpleperf.
        try {
            Process process = new ProcessBuilder().command(toPath, "list", "sw").start();
            process.waitFor();
            String data = readInputStream(process.getInputStream());
            if (!data.contains("cpu-clock")) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return toPath;
    }

    private void checkIfPerfEnabled() {
        if (getProperty("persist.simpleperf.profile_app_uid").equals("" + Os.getuid())) {
            String timeStr = getProperty("persist.simpleperf.profile_app_expiration_time");
            if (!timeStr.isEmpty()) {
                try {
                    long expirationTime = Long.parseLong(timeStr);
                    if (expirationTime > System.currentTimeMillis() / 1000) {
                        return;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        if (getProperty("security.perf_harden").equals("1")) {
            throw new Error("Recording app isn't enabled on the device."
                    + " Please run api_profiler.py.");
        }
    }

    private String getProperty(String name) {
        String value;
        Process process;
        try {
            process = new ProcessBuilder()
                    .command("/system/bin/getprop", name).start();
        } catch (IOException e) {
            return "";
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
        }
        return readInputStream(process.getInputStream());
    }

    private void createSimpleperfDataDir() {
        File file = new File(mSimpleperfDataDir);
        if (!file.isDirectory()) {
            file.mkdir();
        }
    }

    /**
     * Convert a .data simpleperf file to the .trace proto format.
     * <p>
     * Paths may be relative to the simpleperf data dir.
     *
     * @param inputPath Path of the .data file within the simpleperf output directory
     * @param outputPath Path to write the .trace proto format to.
     */
    public void convertSimpleperfOutputToProto(
            @NonNull String inputPath,
            @NonNull String outputPath
    ) {
        ArrayList<String> args = new ArrayList<>();
        args.add(mSimpleperfPath);
        args.add("report-sample");
        args.add("--protobuf");
        args.add("--show-callchain");
        args.add("-i");
        args.add(inputPath);
        args.add("-o");
        args.add(outputPath);

        createProcess(args);
        waitForSimpleperfProcess();
    }

    private void createProcess(List<String> args) {
        ProcessBuilder pb = new ProcessBuilder(args).directory(new File(mSimpleperfDataDir));
        try {
            mSimpleperfProcess = pb.start();
        } catch (IOException e) {
            throw new Error("failed to create simpleperf process: " + e.getMessage());
        }
    }

    private void startRecordingProcess(List<String> recordArgs) {
        // 1. Prepare simpleperf arguments.
        ArrayList<String> args = new ArrayList<>();
        args.add(mSimpleperfPath);
        args.add("record");
        args.add("--log-to-android-buffer");
        args.add("--log");
        args.add("debug");
        args.add("--stdio-controls-profiling");
        args.add("--in-app");
        args.add("--tracepoint-events");
        args.add("/data/local/tmp/tracepoint_events");
        args.addAll(recordArgs);

        // 2. Create the simpleperf process.
        createProcess(args);

        // 3. Wait until simpleperf starts recording.
        String startFlag = readReply();
        if (!startFlag.equals("started")) {
            throw new Error("failed to receive simpleperf start flag, saw '" + startFlag + "'");
        }
    }

    private void waitForSimpleperfProcess() {
        try {
            int exitCode = mSimpleperfProcess.waitFor();
            if (exitCode != 0) {
                throw new AssertionError("simpleperf exited with error: " + exitCode);
            }
        } catch (InterruptedException e) {
        }
        mSimpleperfProcess = null;
    }


    private void sendCmd(@NonNull String cmd) {
        cmd += "\n";
        try {
            mSimpleperfProcess.getOutputStream().write(cmd.getBytes());
            mSimpleperfProcess.getOutputStream().flush();
        } catch (IOException e) {
            throw new Error("failed to send cmd to simpleperf: " + e.getMessage());
        }
        if (!readReply().equals("ok")) {
            throw new Error("failed to run cmd in simpleperf: " + cmd);
        }
    }

    private @NonNull String readReply() {
        // Read one byte at a time to stop at line break or EOF. BufferedReader will try to read
        // more than available and make us blocking, so don't use it.
        String s = "";
        while (true) {
            int c = -1;
            try {
                c = mSimpleperfProcess.getInputStream().read();
            } catch (IOException e) {
            }
            if (c == -1 || c == '\n') {
                break;
            }
            s += (char) c;
        }
        return s;
    }
}
