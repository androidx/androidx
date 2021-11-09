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
import androidx.work.Logger;
import androidx.work.impl.utils.WorkTimer;

import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * The {@link GcmTaskService} responsible for handling requests for executing
 * {@link androidx.work.WorkRequest}s.
 */
public class WorkManagerGcmService extends GcmTaskService {

    private static final String TAG = "WorkManagerGcmService";

    private boolean mIsShutdown;
    private WorkManagerGcmDispatcher mGcmDispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeDispatcher();
    }

    @Override
    @MainThread
    public void onInitializeTasks() {
        checkDispatcher();
        // Reschedule all eligible work, as all tasks have been cleared in GCMNetworkManager.
        // This typically happens after an upgrade.
        mGcmDispatcher.onInitializeTasks();
    }

    @Override
    public int onRunTask(@NonNull TaskParams taskParams) {
        checkDispatcher();
        return mGcmDispatcher.onRunTask(taskParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsShutdown = true;
        mGcmDispatcher.onDestroy();
    }

    @MainThread
    private void checkDispatcher() {
        if (mIsShutdown) {
            Logger.get().debug(TAG, "Re-initializing dispatcher after a request to shutdown");
            initializeDispatcher();
        }
    }

    @MainThread
    private void initializeDispatcher() {
        mIsShutdown = false;
        mGcmDispatcher = new WorkManagerGcmDispatcher(getApplicationContext(), new WorkTimer());
    }
}
