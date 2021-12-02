/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.impl.diagnostics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.impl.workers.DiagnosticsWorker;

/**
 * The {@link android.content.BroadcastReceiver} which dumps out useful diagnostics information.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DiagnosticsReceiver extends BroadcastReceiver {
    private static final String TAG = Logger.tagWithPrefix("DiagnosticsRcvr");

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        Logger.get().debug(TAG, "Requesting diagnostics");
        try {
            WorkManager workManager = WorkManager.getInstance(context);
            workManager.enqueue(OneTimeWorkRequest.from(DiagnosticsWorker.class));
        } catch (IllegalStateException exception) {
            Logger.get().error(TAG, "WorkManager is not initialized", exception);
        }
    }
}
