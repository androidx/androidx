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

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.List;
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

    private static final String TAG = Logger.tagWithPrefix("ForceStopRunnable");

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
        // Clean invalid jobs attributed to WorkManager, and Workers that might have been
        // interrupted because the application crashed (RUNNING state).
        Logger.get().debug(TAG, "Performing cleanup operations.");
        boolean needsScheduling = cleanUp();

        if (shouldRescheduleWorkers()) {
            Logger.get().debug(TAG, "Rescheduling Workers.");
            mWorkManager.rescheduleEligibleWork();
            // Mark the jobs as migrated.
            mWorkManager.getPreferences().setNeedsReschedule(false);
        } else if (isForceStopped()) {
            Logger.get().debug(TAG, "Application was force-stopped, rescheduling.");
            mWorkManager.rescheduleEligibleWork();
        } else if (needsScheduling) {
            Logger.get().debug(TAG, "Found unfinished work, scheduling it.");
            Schedulers.schedule(
                    mWorkManager.getConfiguration(),
                    mWorkManager.getWorkDatabase(),
                    mWorkManager.getSchedulers());
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
        PendingIntent pendingIntent = getPendingIntent(mContext, FLAG_NO_CREATE);
        if (pendingIntent == null) {
            setAlarm(mContext);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Performs cleanup operations like
     *
     * * Cancel invalid JobScheduler jobs.
     * * Reschedule previously RUNNING jobs.
     *
     * @return {@code true} if there are WorkSpecs that need rescheduling.
     */
    @VisibleForTesting
    public boolean cleanUp() {
        // Mitigation for faulty implementations of JobScheduler (b/134058261
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            SystemJobScheduler.cancelInvalidJobs(mContext);
        }

        // Reset previously unfinished work.
        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        workDatabase.beginTransaction();
        boolean needsScheduling;
        try {
            List<WorkSpec> workSpecs = workSpecDao.getRunningWork();
            needsScheduling = workSpecs != null && !workSpecs.isEmpty();
            if (needsScheduling) {
                // Mark every instance of unfinished work with state = ENQUEUED and
                // SCHEDULE_NOT_REQUESTED_AT = -1 irrespective of its current state.
                // This is because the application might have crashed previously and we should
                // reschedule jobs that may have been running previously.
                // Also there is a chance that an application crash, happened during
                // onStartJob() and now no corresponding job now exists in JobScheduler.
                // To solve this, we simply force-reschedule all unfinished work.
                for (WorkSpec workSpec : workSpecs) {
                    workSpecDao.setState(ENQUEUED, workSpec.id);
                    workSpecDao.markWorkSpecScheduled(workSpec.id, SCHEDULE_NOT_REQUESTED_YET);
                }
            }
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
        return needsScheduling;
    }

    /**
     * @return {@code true} If we need to reschedule Workers.
     */
    @VisibleForTesting
    boolean shouldRescheduleWorkers() {
        return mWorkManager.getPreferences().needsReschedule();
    }

    /**
     * @param flags   The {@link PendingIntent} flags.
     * @return an instance of the {@link PendingIntent}.
     */
    private static PendingIntent getPendingIntent(Context context, int flags) {
        Intent intent = getIntent(context);
        return PendingIntent.getBroadcast(context, ALARM_ID, intent, flags);
    }

    /**
     * @return The instance of {@link Intent} used to keep track of force stops.
     */
    @VisibleForTesting
    static Intent getIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, ForceStopRunnable.BroadcastReceiver.class));
        intent.setAction(ACTION_FORCE_STOP_RESCHEDULE);
        return intent;
    }

    static void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Using FLAG_UPDATE_CURRENT, because we only ever want once instance of this alarm.
        PendingIntent pendingIntent = getPendingIntent(context, FLAG_UPDATE_CURRENT);
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
     * long lived alarm which helps track force stops for an application.  This is the target of the
     * alarm set by ForceStopRunnable in {@link #setAlarm(Context)}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class BroadcastReceiver extends android.content.BroadcastReceiver {
        private static final String TAG = Logger.tagWithPrefix("ForceStopRunnable$Rcvr");

        @Override
        public void onReceive(Context context, Intent intent) {
            // Our alarm somehow got triggered, so make sure we reschedule it.  This should really
            // never happen because we set it so far in the future.
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_FORCE_STOP_RESCHEDULE.equals(action)) {
                    Logger.get().verbose(
                            TAG,
                            "Rescheduling alarm that keeps track of force-stops.");
                    ForceStopRunnable.setAlarm(context);
                }
            }
        }
    }
}
