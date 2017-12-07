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

package android.arch.background.workmanager.background.systemalarm;

import static android.app.AlarmManager.RTC_WAKEUP;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.utils.IdGenerator;
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
        // TODO(xbhatnag): Initial delay and periodic work with AlarmManager
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        // TODO(janclarin): Retrieve alarm id mapping for work spec id.
        // TODO(xbhatnag): Cancel pending alarms via AlarmManager.
    }

    /**
     * Periodic work is rescheduled using one-time alarms after each run. This allows the delivery
     * times to drift to guarantee that the interval duration always elapses between alarms.
     */
    private void scheduleWorkSpec(@NonNull WorkSpec workSpec) {
        // TODO(janclarin): Store alarm id mapping for work spec in the database for cancelling.
        long triggerAtMillis = System.currentTimeMillis() + getDelayMillis(workSpec);
        int nextAlarmId = mIdGenerator.nextAlarmManagerId();
        PendingIntent pendingIntent = createPendingIntent(workSpec.getId(), nextAlarmId);

        setExactAlarm(triggerAtMillis, pendingIntent);

        Log.d(TAG, "Scheduled work with ID: " + workSpec.getId());
    }

    private static long getDelayMillis(@NonNull WorkSpec workSpec) {
        // Ignores flex duration for periodic work to ensure that individual runs do not overlap.
        return workSpec.isPeriodic() ? workSpec.getIntervalDuration() : workSpec.calculateDelay();
    }

    private PendingIntent createPendingIntent(@NonNull String workSpecId, int alarmId) {
        Intent intent = new Intent(mAppContext, SystemAlarmService.class);
        intent.putExtra(SystemAlarmService.EXTRA_WORK_ID, workSpecId);
        return PendingIntent.getService(mAppContext, alarmId, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private void setExactAlarm(long triggerAtMillis, @NonNull PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= 19) {
            mAlarmManager.setExact(RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            mAlarmManager.set(RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }
}
