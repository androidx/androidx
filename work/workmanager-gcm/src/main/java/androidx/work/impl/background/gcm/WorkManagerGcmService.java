/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.background.gcm;


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * The {@link GcmTaskService} responsible for handling requests for executing
 * {@link androidx.work.WorkRequest}s.
 */
public class WorkManagerGcmService extends GcmTaskService {
    private WorkManagerGcmDispatcher mGcmDispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        mGcmDispatcher = new WorkManagerGcmDispatcher(getApplicationContext());
    }

    @Override
    @MainThread
    public void onInitializeTasks() {
        // Reschedule all eligible work, as all tasks have been cleared in GCMNetworkManager.
        // This typically happens after an upgrade.
        mGcmDispatcher.onInitializeTasks();
    }

    @Override
    public int onRunTask(@NonNull TaskParams taskParams) {
        return mGcmDispatcher.onRunTask(taskParams);
    }
}
