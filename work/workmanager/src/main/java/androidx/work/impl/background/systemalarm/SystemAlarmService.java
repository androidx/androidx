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
import android.content.Intent;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleService;
import androidx.work.Logger;
import androidx.work.impl.utils.WakeLocks;

/**
 * Service invoked by {@link android.app.AlarmManager} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmService extends LifecycleService
        implements SystemAlarmDispatcher.CommandsCompletedListener {

    private static final String TAG = Logger.tagWithPrefix("SystemAlarmService");

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
        // If the service were to crash, we want all unacknowledged Intents to get redelivered.
        return Service.START_REDELIVER_INTENT;
    }

    @MainThread
    @Override
    public void onAllCommandsCompleted() {
        Logger.get().debug(TAG, "All commands completed in dispatcher");
        // Check to see if we hold any more wake locks.
        WakeLocks.checkWakeLocks();
        // No need to pass in startId; stopSelf() translates to stopSelf(-1) which is a hard stop
        // of all startCommands. This is the behavior we want.
        stopSelf();
    }
}
