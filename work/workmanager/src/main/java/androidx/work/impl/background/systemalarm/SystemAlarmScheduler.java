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

package androidx.work.impl.background.systemalarm;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import androidx.work.impl.Scheduler;
import androidx.work.impl.model.WorkSpec;

/**
 * A {@link Scheduler} that schedules work using {@link android.app.AlarmManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmScheduler implements Scheduler {

    private static final String TAG = "SystemAlarmScheduler";

    private final Context mContext;

    public SystemAlarmScheduler(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            scheduleWorkSpec(workSpec);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        Intent cancelIntent = CommandHandler.createStopWorkIntent(mContext, workSpecId);
        mContext.startService(cancelIntent);
    }

    /**
     * Periodic work is rescheduled using one-time alarms after each run. This allows the delivery
     * times to drift to guarantee that the interval duration always elapses between alarms.
     */
    private void scheduleWorkSpec(@NonNull WorkSpec workSpec) {
        Log.d(TAG, String.format("Scheduling work with workSpecId %s", workSpec.id));
        Intent scheduleIntent = CommandHandler.createScheduleWorkIntent(mContext, workSpec.id);
        mContext.startService(scheduleIntent);
    }
}
