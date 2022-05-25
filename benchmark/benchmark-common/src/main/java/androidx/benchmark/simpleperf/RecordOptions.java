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

import android.system.Os;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * This class sets record options used by ProfileSession. The options are
 * converted to a string list in toRecordArgs(), which is then passed to
 * `simpleperf record` cmd. Run `simpleperf record -h` or
 * `run_simpleperf_on_device.py record -h` for help messages.
 * </p>
 *
 * <p>
 * Example:
 *   RecordOptions options = new RecordOptions();
 *   options.setDuration(3).recordDwarfCallGraph().setOutputFilename("perf.data");
 *   ProfileSession session = new ProfileSession();
 *   session.startRecording(options);
 * </p>
 *
 * NOTE: copied from
 * https://cs.android.com/android/platform/superproject/+/master:system/extras/simpleperf/app_api/
 *
 * @hide
 */
@RequiresApi(28)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RecordOptions {
    /**
     * Set output filename. Default is perf-<month>-<day>-<hour>-<minute>-<second>.data.
     * The file will be generated under simpleperf_data/.
     */
    @NonNull
    public RecordOptions setOutputFilename(@NonNull String filename) {
        mOutputFilename = filename;
        return this;
    }

    /**
     * Set event to record. Default is cpu-cycles. See `simpleperf list` for all available events.
     */
    @NonNull
    public RecordOptions setEvent(@NonNull String event) {
        mEvent = event;
        return this;
    }

    /**
     * Set how many samples to generate each second running. Default is 4000.
     */
    @NonNull
    public RecordOptions setSampleFrequency(int freq) {
        mFreq = freq;
        return this;
    }

    /**
     * Set record duration. The record stops after `durationInSecond` seconds. By default,
     * record stops only when stopRecording() is called.
     */
    @NonNull
    public RecordOptions setDuration(double durationInSecond) {
        mDurationInSeconds = durationInSecond;
        return this;
    }

    /**
     * Record some threads in the app process. By default, record all threads in the process.
     */
    @NonNull
    public RecordOptions setSampleThreads(@NonNull List<Integer> threads) {
        mThreads.addAll(threads);
        return this;
    }

    /**
     * Record current thread in the app process. By default, record all threads in the process.
     */
    @NonNull
    public RecordOptions setSampleCurrentThread() {
        return setSampleThreads(Collections.singletonList(Os.gettid()));
    }

    /**
     * Record dwarf based call graph. It is needed to get Java callstacks.
     */
    @NonNull
    public RecordOptions recordDwarfCallGraph() {
        mDwarfCallGraph = true;
        mFpCallGraph = false;
        return this;
    }

    /**
     * Record frame pointer based call graph. It is suitable to get C++ callstacks on 64bit devices.
     */
    @NonNull
    public RecordOptions recordFramePointerCallGraph() {
        mFpCallGraph = true;
        mDwarfCallGraph = false;
        return this;
    }

    /**
     * Trace context switch info to show where threads spend time off cpu.
     */
    @NonNull
    public RecordOptions traceOffCpu() {
        mTraceOffCpu = true;
        return this;
    }

    /**
     * Translate record options into arguments for `simpleperf record` cmd.
     */
    @NonNull
    public List<String> toRecordArgs() {
        ArrayList<String> args = new ArrayList<>();

        String filename = mOutputFilename;
        if (filename == null) {
            filename = getDefaultOutputFilename();
        }
        args.add("-o");
        args.add(filename);
        args.add("-e");
        args.add(mEvent);
        args.add("-f");
        args.add(String.valueOf(mFreq));
        if (mDurationInSeconds != 0.0) {
            args.add("--duration");
            args.add(String.valueOf(mDurationInSeconds));
        }
        if (mThreads.isEmpty()) {
            args.add("-p");
            args.add(String.valueOf(Os.getpid()));
        } else {
            String s = "";
            for (int i = 0; i < mThreads.size(); i++) {
                if (i > 0) {
                    s += ",";
                }
                s += mThreads.get(i).toString();
            }
            args.add("-t");
            args.add(s);
        }
        if (mDwarfCallGraph) {
            args.add("-g");
        } else if (mFpCallGraph) {
            args.add("--call-graph");
            args.add("fp");
        }
        if (mTraceOffCpu) {
            args.add("--trace-offcpu");
        }
        return args;
    }

    private String getDefaultOutputFilename() {
        LocalDateTime time = LocalDateTime.now(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'perf'-MM-dd-HH-mm-ss'.data'");
        return time.format(formatter);
    }

    @Nullable
    private String mOutputFilename;

    @NonNull
    private String mEvent = "cpu-cycles";

    private int mFreq = 4000;

    private double mDurationInSeconds = 0.0;

    @NonNull
    private ArrayList<Integer> mThreads = new ArrayList<>();

    private boolean mDwarfCallGraph = false;

    private boolean mFpCallGraph = false;

    private boolean mTraceOffCpu = false;
}
