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

package androidx.profileinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The {@link android.content.BroadcastReceiver} which forces a synchronous installation of the
 * baseline profile.
 *
 * This is primarily used by tools to force a synchronous install of the baseline profile without
 * starting the application's main activity. It is not expected for this receiver to be used at
 * runtime by anything other than tools, and as such, the action filter is defined with the
 * "dump" permission.
 */
public class ProfileInstallReceiver extends BroadcastReceiver {
    /**
     * This is the action constant that this broadcast receiver responds to and installs a profile.
     */
    public static final @NonNull String ACTION_INSTALL_PROFILE =
            "androidx.profileinstaller.action.INSTALL_PROFILE";

    /**
     * This is the action constant for saving the current in-memory hot method data
     * to a profile on disk.
     *
     * This is to be used with compilation:
     * <p><code>cmd package compile -f -m speed-profile myPackageName</code>
     * <p>And with profile extraction (API33+):
     * <p><code>pm dump-profiles --dump-classes-and-methods</code>
     */
    public static final @NonNull String ACTION_SAVE_PROFILE =
            "androidx.profileinstaller.action.SAVE_PROFILE";

    /**
     * This is an action constant which requests that {@link ProfileInstaller} manipulate the
     * skip file used during profile installation. This is only useful when the app is being
     * instrumented when using Jetpack Macrobenchmarks.
     */
    public static final @NonNull String ACTION_SKIP_FILE =
            "androidx.profileinstaller.action.SKIP_FILE";

    /**
     * This is an action that triggers actions required for stable benchmarking from an external
     * tool on user builds, such as clearing the code cache, or triggering garbage collection.
     */
    public static final @NonNull String ACTION_BENCHMARK_OPERATION =
            "androidx.profileinstaller.action.BENCHMARK_OPERATION";

    /**
     * This is the key in the {@link Bundle} of extras, which provides additional information on
     * the operation to be performed.
     */
    private static final @NonNull String EXTRA_SKIP_FILE_OPERATION = "EXTRA_SKIP_FILE_OPERATION";

    /**
     * The value that requests that a skip file be written.
     */
    private static final @NonNull String EXTRA_SKIP_FILE_OPERATION_WRITE = "WRITE_SKIP_FILE";
    /**
     * The value that requests that a skip file be deleted.
     */
    private static final @NonNull String EXTRA_SKIP_FILE_OPERATION_DELETE = "DELETE_SKIP_FILE";

    /**
     * This is the key in the {@link Bundle} of extras, which provides additional information on
     * the operation to be performed.
     */
    private static final @NonNull String EXTRA_BENCHMARK_OPERATION = "EXTRA_BENCHMARK_OPERATION";
    /**
     * The value that requests the shader cache be dropped.
     */
    private static final @NonNull String EXTRA_BENCHMARK_OPERATION_DROP_SHADER_CACHE =
            "DROP_SHADER_CACHE";

    /**
     * The value that requests saving profile of a separate process in the package.
     * This isn't implemented as an extra on the SAVE_PROFILE action as it would be silently ignored
     * on apps with older versions of profileinstaller
     */
    private static final @NonNull String EXTRA_BENCHMARK_OPERATION_SAVE_PROFILE =
            "SAVE_PROFILE";

    /**
     * This is the key in the {@link Bundle} of extras, which provides PID for a benchmark operation
     */
    private static final @NonNull String EXTRA_PID = "EXTRA_PID";


    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_INSTALL_PROFILE.equals(action)) {
            ProfileInstaller.writeProfile(context, Runnable::run,
                    new ResultDiagnostics(), /* forceWriteProfile */true);
        } else if (ACTION_SKIP_FILE.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String operation = extras.getString(EXTRA_SKIP_FILE_OPERATION);
                if (EXTRA_SKIP_FILE_OPERATION_WRITE.equals(operation)) {
                    ProfileInstaller.writeSkipFile(context, Runnable::run, new ResultDiagnostics());
                } else if (EXTRA_SKIP_FILE_OPERATION_DELETE.equals(operation)) {
                    ProfileInstaller.deleteSkipFile(
                            context, Runnable::run, new ResultDiagnostics());
                }
            }
        } else if (ACTION_SAVE_PROFILE.equals(action)) {
            saveProfile(new ResultDiagnostics());
        } else if (ACTION_BENCHMARK_OPERATION.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String operation = extras.getString(EXTRA_BENCHMARK_OPERATION);
                ResultDiagnostics diagnostics = new ResultDiagnostics();

                if (EXTRA_BENCHMARK_OPERATION_DROP_SHADER_CACHE.equals(operation)) {
                    BenchmarkOperation.dropShaderCache(context, diagnostics);
                } else if (EXTRA_BENCHMARK_OPERATION_SAVE_PROFILE.equals(operation)) {
                    saveProfile(extras.getInt(EXTRA_PID, Process.myPid()), diagnostics);
                } else {
                    diagnostics.onResultReceived(
                            ProfileInstaller.RESULT_BENCHMARK_OPERATION_UNKNOWN,
                            null
                    );
                }
            }
        }
    }

    /**
     * Sends SIGUSR1 signal to this process, so that the app will dump its profiles to be used for
     * profile collection.
     *
     * On user builds, this signal can't be sent by a separate (e.g. test) process or shell
     * process, so instead we flush via this broadcast event.
     *
     * Unfortunately, this isn't able to validate that the signal is processed correctly both
     * because it's async, and because the only way to validate appears to be logcat. For local
     * debugging, you should see a logcat line containing: `SIGUSR1 forcing GC (no HPROF) and
     * profile save`
     */
    static void saveProfile(@NonNull ProfileInstaller.DiagnosticsCallback callback) {
        saveProfile(Process.myPid(), callback);
    }

    /**
     * Sends SIGUSR1 signal to a specific subprocess, so that the app will dump its profiles to be
     * used for profile collection.
     *
     * On user builds, this signal can't be sent by a separate (e.g. test) app or shell
     * process, so instead we use this broadcast to do so from within the app.
     *
     * Unfortunately, this isn't able to validate that the signal is processed correctly both
     * because it's async, and because the only way to validate appears to be logcat. For local
     * debugging, you should see a logcat line containing: `SIGUSR1 forcing GC (no HPROF) and
     * profile save` in the app subprocess specified by `pid`.
     */
    static void saveProfile(int pid, @NonNull ProfileInstaller.DiagnosticsCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Process.sendSignal(pid, /* SIGUSR1 */ 10);
            callback.onResultReceived(ProfileInstaller.RESULT_SAVE_PROFILE_SIGNALLED, null);
        } else {
            callback.onResultReceived(ProfileInstaller.RESULT_SAVE_PROFILE_SKIPPED, null);
        }
    }

    class ResultDiagnostics implements ProfileInstaller.DiagnosticsCallback {
        @Override
        public void onDiagnosticReceived(int code, @Nullable Object data) {
            ProfileInstaller.LOG_DIAGNOSTICS.onDiagnosticReceived(code, data);
        }

        @Override
        public void onResultReceived(int code, @Nullable Object data) {
            ProfileInstaller.LOG_DIAGNOSTICS.onResultReceived(code, data);
            setResultCode(code);
        }
    }
}
