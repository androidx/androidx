/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.systemalarm;

import android.app.Service;
import android.arch.background.workmanager.ExecutionListener;
import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.background.BackgroundProcessor;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * Service invoked by {@link android.app.AlarmManager} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmService extends Service implements ExecutionListener {
    private static final String TAG = "SystemAlarmService";

    private BackgroundProcessor mProcessor;

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(context);
        WorkDatabase database = workManager.getWorkDatabase();
        mProcessor = new BackgroundProcessor(context, database, workManager.getScheduler(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO(janclarin): Keep track of work to be done.
        // TODO(janclarin): Run work with processor.
        // TODO(janclarin): Determine appropriate stickiness.
        return START_STICKY;
    }

    @Override
    public void onExecuted(String workSpecId, int result) {
        Log.d(TAG, workSpecId + " executed on AlarmManager");
        // TODO(janclarin): Handle rescheduling or stopping service when there is no more work.
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroyed");
    }
}
