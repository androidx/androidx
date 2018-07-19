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

package androidx.work.impl.utils;

import static android.app.AlarmManager.RTC_WAKEUP;
import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.impl.WorkManagerImpl;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager is restarted after an app was force stopped.
 * Alarms and Jobs get cancelled when an application is force-stopped. To reschedule, we
 * create a pending alarm that will not survive force stops.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ForceStopRunnable implements Runnable {

    private static final String TAG = "ForceStopRunnable";

    @VisibleForTesting
    static final String ACTION_FORCE_STOP_RESCHEDULE = "ACTION_FORCE_STOP_RESCHEDULE";

    // All our alarms are use request codes which are > 0.
    private static final int ALARM_ID = -1;
    private static final long TEN_YEARS = TimeUnit.DAYS.toMillis(10 * 365);

    private final Context mContext;
    private final WorkManagerImpl mWorkManager;

    public ForceStopRunnable(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        mContext = context.getApplicationContext();
        mWorkManager = workManager;
    }

    @Override
    public void run() {
        if (shouldRescheduleWorkers()) {
            Log.d(TAG, "Rescheduling Workers.");
            mWorkManager.rescheduleEligibleWork();
            // Mark the jobs as migrated.
            mWorkManager.getPreferences().setNeedsReschedule(false);
        } else if (isForceStopped()) {
            Log.d(TAG, "Application was force-stopped, rescheduling.");
            mWorkManager.rescheduleEligibleWork();
        }
        mWorkManager.onForceStopRunnableCompleted();
    }

    /**
     * @return {@code true} If the application was force stopped.
     */
    @VisibleForTesting
    public boolean isForceStopped() {
        // Alarms get cancelled when an app is force-stopped starting at Eclair MR1.
        // Cancelling of Jobs on force-stop was introduced in N-MR1 (SDK 25).
        // Even though API 23, 24 are probably safe, OEMs may choose to do
        // something different.
        PendingIntent pendingIntent = getPendingIntent(ALARM_ID, FLAG_NO_CREATE);
        if (pendingIntent == null) {
            setAlarm(ALARM_ID);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} If we need to reschedule Workers.
     */
    @VisibleForTesting
    public boolean shouldRescheduleWorkers() {
        return mWorkManager.getPreferences().needsReschedule();
    }

    /**
     * @param alarmId The stable alarm id to be used.
     * @param flags   The {@link PendingIntent} flags.
     * @return an instance of the {@link PendingIntent}.
     */
    @VisibleForTesting
    public PendingIntent getPendingIntent(int alarmId, int flags) {
        Intent intent = getIntent();
        return PendingIntent.getBroadcast(mContext, alarmId, intent, flags);
    }

    /**
     * @return The instance of {@link Intent} used to keep track of force stops.
     */
    @VisibleForTesting
    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(mContext, ForceStopRunnable.BroadcastReceiver.class));
        intent.setAction(ACTION_FORCE_STOP_RESCHEDULE);
        return intent;
    }

    void setAlarm(int alarmId) {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        // Using FLAG_UPDATE_CURRENT, because we only ever want once instance of this alarm.
        PendingIntent pendingIntent = getPendingIntent(alarmId, FLAG_UPDATE_CURRENT);
        long triggerAt = System.currentTimeMillis() + TEN_YEARS;
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }
    }

    /**
     * A {@link android.content.BroadcastReceiver} which takes care of recreating the
     * long lived alarm which helps track force stops for an application.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class BroadcastReceiver extends android.content.BroadcastReceiver {
        private static final String TAG = "ForceStopRunnable$Rcvr";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_FORCE_STOP_RESCHEDULE.equals(action)) {
                    Log.v(TAG, "Rescheduling alarm that keeps track of force-stops.");
                    WorkManagerImpl workManager = WorkManagerImpl.getInstance();
                    ForceStopRunnable runnable = new ForceStopRunnable(context, workManager);
                    runnable.setAlarm(ForceStopRunnable.ALARM_ID);
                }
            }
        }
    }
}
