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
import android.os.Bundle;

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
     * This is an action constant which requests that {@link ProfileInstaller} manipulate the
     * skip file used during profile installation. This is only useful when the app is being
     * instrumented when using Jetpack Macrobenchmarks.
     */
    public static final @NonNull String ACTION_SKIP_FILE =
            "androidx.profileinstaller.action.SKIP_FILE";

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

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_INSTALL_PROFILE.equals(action)) {
            ProfileInstaller.writeProfile(context, Runnable::run,
                    new ResultDiagnostics(), /* forceWriteProfile */true);
        } else if (ACTION_SKIP_FILE.equals(action)) {
            Bundle extras = intent.getExtras();
            String operation = extras.getString(EXTRA_SKIP_FILE_OPERATION);
            if (EXTRA_SKIP_FILE_OPERATION_WRITE.equals(operation)) {
                ProfileInstaller.writeSkipFile(context, Runnable::run, new ResultDiagnostics());
            } else if (EXTRA_SKIP_FILE_OPERATION_DELETE.equals(operation)) {
                ProfileInstaller.deleteSkipFile(context, Runnable::run, new ResultDiagnostics());
            }
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
