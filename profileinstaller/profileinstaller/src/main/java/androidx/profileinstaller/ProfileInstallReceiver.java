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
     * This is the action constant that this broadcast receiver responds to.
     */
    public static final @NonNull String ACTION_INSTALL_PROFILE =
            "androidx.profileinstaller.action.INSTALL_PROFILE";

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) return;
        if (!ACTION_INSTALL_PROFILE.equals(intent.getAction())) return;
        ProfileInstaller.writeProfile(context, new ResultDiagnostics());
    }

    class ResultDiagnostics implements ProfileInstaller.Diagnostics {
        @Override
        public void diagnostic(int code, @Nullable Object data) {
            ProfileInstaller.LOG_DIAGNOSTICS.diagnostic(code, data);
        }

        @Override
        public void result(int code, @Nullable Object data) {
            ProfileInstaller.LOG_DIAGNOSTICS.result(code, data);
            setResultCode(code);
        }
    }
}
