/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.background.systemalarm;

import android.app.Service;
import android.arch.lifecycle.LifecycleService;
import android.content.Intent;
import android.support.annotation.MainThread;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * Service invoked by {@link android.app.AlarmManager} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmService extends LifecycleService
        implements SystemAlarmDispatcher.CommandsCompletedListener {

    private static final String TAG = "SystemAlarmService";

    private SystemAlarmDispatcher mDispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        mDispatcher = new SystemAlarmDispatcher(this);
        mDispatcher.setCompletedListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            mDispatcher.add(intent, startId);
        }
        return Service.START_STICKY;
    }

    @MainThread
    @Override
    public void onAllCommandsCompleted() {
        Log.d(TAG, "All commands completed in dispatcher");
        stopSelf();
    }
}
