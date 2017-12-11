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

package android.arch.background.workmanager.impl.background.systemalarm;

import static android.app.AlarmManager.RTC_WAKEUP;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.utils.IdGenerator;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * A {@link Scheduler} that schedules work using {@link android.app.AlarmManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemAlarmScheduler implements Scheduler {
    private static final String TAG = "SystemAlarmScheduler";

    private final AlarmManager mAlarmManager;
    private final IdGenerator mIdGenerator;
    private final Context mAppContext;

    public SystemAlarmScheduler(Context context) {
        mAppContext = context.getApplicationContext();
        mIdGenerator = new IdGenerator(mAppContext);
        mAlarmManager = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            scheduleWorkSpec(workSpec);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        // TODO(janclarin): Retrieve alarm id mapping for work spec id.
        // TODO(xbhatnag): Cancel pending alarms via AlarmManager.
        Intent cancelIntent = SystemAlarmService.createCancelWorkIntent(mAppContext, workSpecId);
        mAppContext.startService(cancelIntent);
    }

    /**
     * Periodic work is rescheduled using one-time alarms after each run. This allows the delivery
     * times to drift to guarantee that the interval duration always elapses between alarms.
     */
    private void scheduleWorkSpec(@NonNull WorkSpec workSpec) {
        // TODO(janclarin): Store alarm id mapping for work spec in the database for cancelling.
        long triggerAtMillis = workSpec.calculateNextRunTime();
        int nextAlarmId = mIdGenerator.nextAlarmManagerId();
        Intent intent = SystemAlarmService.createDelayMetIntent(mAppContext, workSpec.getId());
        setExactAlarm(nextAlarmId, triggerAtMillis, intent);
        Log.d(TAG, "Scheduled work with ID: " + workSpec.getId());
    }

    private void setExactAlarm(int alarmId, long triggerAtMillis, @NonNull Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(mAppContext, alarmId, intent,
                PendingIntent.FLAG_ONE_SHOT);
        if (Build.VERSION.SDK_INT >= 19) {
            mAlarmManager.setExact(RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            mAlarmManager.set(RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }
}
